from pathlib import Path
import base64
import re

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'
LOGO_B64 = Path('.github/assets/app_logo.webp.b64')
PRINTER_ART_B64 = Path('.github/assets/usb_scanner_design.jpg.b64')
RES = ROOT / 'app/src/main/res/drawable'
LOGO_OUT = RES / 'app_logo.webp'
LOGO_XML = RES / 'app_logo.xml'
PRINTER_ART_OUT = RES / 'usb_scanner_design.jpg'


def replace_once(path, old, new):
    text = path.read_text(encoding='utf-8')
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding='utf-8')
        return True
    return False


RES.mkdir(parents=True, exist_ok=True)
if LOGO_B64.exists():
    LOGO_OUT.write_bytes(base64.b64decode(LOGO_B64.read_text(encoding='utf-8')))
LOGO_XML.unlink(missing_ok=True)
if PRINTER_ART_B64.exists():
    PRINTER_ART_OUT.write_bytes(base64.b64decode(PRINTER_ART_B64.read_text(encoding='utf-8')))

# Keep the scanner UI safe-area adjustment.
replace_once(MAIN, 'setPadding(dp(18), dp(16), dp(18), dp(18))', 'setPadding(dp(18), dp(40), dp(18), dp(18))')

# Preserve scanner-page processing before saving a scan.
replace_once(MAIN, '''                library.savePage(result.bitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(result.bitmap)''', '''                var finalBitmap = result.bitmap
                if (brightness != 0f || contrast != 1f) {
                    val adjusted = library.applyAdjustments(finalBitmap, brightness, contrast)
                    if (!finalBitmap.isRecycled) finalBitmap.recycle()
                    finalBitmap = adjusted
                }
                if (grayscale) {
                    val gray = document.grayscale(finalBitmap)
                    if (!finalBitmap.isRecycled) finalBitmap.recycle()
                    finalBitmap = gray
                }
                library.savePage(finalBitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(finalBitmap)''')

# Keep support for opening a specific scanner tab from another scanner screen.
main_text = MAIN.read_text(encoding='utf-8')
if 'getIntExtra("open_tab"' not in main_text:
    replace_once(MAIN, '''        buildShell()
        usb.register()
        refreshUsb()''', '''        buildShell()
        usb.register()
        refreshUsb()
        val openTab = intent.getIntExtra("open_tab", -1)
        if (openTab in 0..2) renderTab(openTab)''')

# STEP 1: Home-screen printer identity/status redesign only.
home_block = '''    private fun renderHome() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        val art = ImageView(this).apply {
            setImageResource(com.indianequipments.usbscanner.R.drawable.usb_scanner_design)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = rounded(Color.rgb(18,25,36), 16)
        }
        header.addView(art, LinearLayout.LayoutParams(dp(58), dp(58)).apply { setMargins(0,0,dp(12),0) })
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(this).apply {
            text = "USB SCANNER"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        heading.addView(TextView(this).apply {
            text = "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(110,155,215))
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(header)

        val deviceCard = card()
        deviceCard.addView(TextView(this).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145,160,180))
            text = "PRINTER STATUS"
        })
        status = TextView(this).apply {
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (device == null) Color.rgb(255,205,70) else Color.rgb(75,220,145))
            text = if (device == null) "Printer Not Found" else "Printer Connected"
            setPadding(0,dp(5),0,0)
        }
        deviceCard.addView(status)
        deviceCard.addView(TextView(this).apply {
            val model = device?.productName ?: device?.deviceName
            text = if (model.isNullOrBlank()) "Printer Model • Not detected" else "Printer Model • $model"
            textSize = 12f
            setTextColor(Color.rgb(120,136,158))
            setPadding(0,dp(4),0,0)
        })

        val printerActions = row()
        printerActions.addView(actionButton("SELECT PRINTER") { showPrinterSelector() }, weight(1f,4))
        printerActions.addView(actionButton("↻  REFRESH") { refreshUsb() }, weight(1f,4))
        deviceCard.addView(printerActions, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,dp(14),0,0) })
        content.addView(deviceCard, margin(0,10))

        val hero = card()
        hero.addView(TextView(this).apply {
            text = "SCAN STUDIO"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(130,160,200))
        })
        hero.addView(TextView(this).apply {
            text = "Turn paper into a polished digital document"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0,dp(6),0,dp(2))
        })
        hero.addView(TextView(this).apply {
            text = "Multi-page • Preview • Enhance • Edit • Export • Share"
            textSize = 12f
            setTextColor(Color.rgb(150,165,185))
        })
        hero.addView(actionButton("＋  NEW SCAN") { renderTab(1) }, LinearLayout.LayoutParams(-1, dp(54)).apply {
            setMargins(0,dp(18),0,0)
        })
        content.addView(hero, margin(0,12))

        val stats = row()
        stats.addView(statCard("${library.list().size}", "ALL SCANS"), weight(1f,5))
        stats.addView(statCard("${pages.size}", "CURRENT PAGES"), weight(1f,5))
        stats.addView(statCard("${selectedLibrary.size}", "SELECTED"), weight(1f,5))
        content.addView(stats, margin(0,12))
        content.addView(section("QUICK ACTIONS"))
        val quick = row()
        quick.addView(tile("▦", "All Scans") { renderTab(2) }, weight(1f,6))
        quick.addView(tile("✦", "Enhance") { if (pages.isNotEmpty()) { autoEnhance(); renderTab(1) } else renderTab(1) }, weight(1f,6))
        quick.addView(tile("↗", "Export") { showShareFormatDialog() }, weight(1f,6))
        content.addView(quick, margin(0,12))
        content.addView(section("RECENT DOCUMENTS"))
        val recent = library.list().take(3)
        if (recent.isEmpty()) content.addView(emptyCard("No scans yet", "Your scanned pages will appear here."), margin(0,8))
        recent.forEach { file -> content.addView(fileRow(file), margin(0,8)) }
    }

'''

selector = '''    private fun showPrinterSelector() {
        val dialog = AlertDialog.Builder(this).setTitle("Select Printer").create()
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        val sourceRow = row()
        sourceRow.addView(actionButton("WiFi") { dialog.dismiss(); Toast.makeText(this, "WiFi printer selection — next step", Toast.LENGTH_SHORT).show() }, weight(1f,3))
        sourceRow.addView(actionButton("Bluetooth") { dialog.dismiss(); Toast.makeText(this, "Bluetooth printer selection — next step", Toast.LENGTH_SHORT).show() }, weight(1f,3))
        sourceRow.addView(actionButton("USB") { dialog.dismiss(); requestScanner() }, weight(1f,3))
        wrap.addView(sourceRow)
        dialog.setView(wrap)
        dialog.show()
    }

'''

text = MAIN.read_text(encoding='utf-8')
pattern = re.compile(r'    private fun renderHome\(\) \{.*?\n    \}\n\n    private fun renderStudio\(\)', re.S)
if pattern.search(text):
    text = pattern.sub(home_block + '    private fun renderStudio()', text, count=1)
if 'private fun showPrinterSelector()' not in text:
    marker = '    private fun renderStudio()'
    text = text.replace(marker, selector + marker, 1)
MAIN.write_text(text, encoding='utf-8')

ms = MANIFEST.read_text(encoding='utf-8')
if 'android:icon="@drawable/app_logo"' not in ms:
    ms = ms.replace('<application android:theme="@style/AppTheme" android:label="USB Scanner"', '<application android:theme="@style/AppTheme" android:label="USB Scanner" android:icon="@drawable/app_logo"', 1)
MANIFEST.write_text(ms, encoding='utf-8')

print('Applied Step 1 printer-status home UI, printer design art, safe-area, scan persistence, scanner routing, and app-logo fixes')
