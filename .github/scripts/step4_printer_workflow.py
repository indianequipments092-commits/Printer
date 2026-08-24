from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"

s = MAIN.read_text(encoding="utf-8")

# Step 4 printer state
s = s.replace('    private var pendingTempPdf: File? = null\n', '''    private var pendingTempPdf: File? = null
    private var printerStatusView: TextView? = null
    private var printerModelView: TextView? = null
    private var selectedPrinterName: String? = null
    private val printerPrefs by lazy { getSharedPreferences("printer_prefs", MODE_PRIVATE) }
''')

home_pat = re.compile(r'    private fun renderHome\(\) \{.*?\n    \}\n\n    private fun renderStudio', re.S)
new_home = '''    private fun renderHome() {
        content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"))
        val deviceCard = card()
        deviceCard.addView(TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(145,160,180))
            text = "PRINTER STATUS"
        })
        printerStatusView = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (selectedPrinterName.isNullOrBlank()) Color.rgb(255,196,70) else Color.rgb(80,220,150))
            text = if (selectedPrinterName.isNullOrBlank()) "Printer Not Found" else "Printer Connected"
            setPadding(0,dp(5),0,0)
        }
        deviceCard.addView(printerStatusView)
        printerModelView = TextView(this).apply {
            text = "Printer Model • ${selectedPrinterName ?: "Not detected"}"
            textSize = 12f
            setTextColor(Color.rgb(120,136,158))
            setPadding(0,dp(4),0,0)
        }
        deviceCard.addView(printerModelView)
        val printerRow = row()
        printerRow.addView(actionButton("SELECT PRINTER") { showPrinterSelector() }, weight(1f,4))
        printerRow.addView(actionButton("↻  REFRESH") { refreshPrinters() }, weight(1f,4))
        deviceCard.addView(printerRow, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,dp(14),0,0) })
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
        hero.addView(actionButton("＋  NEW SCAN") { renderTab(1) }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0,dp(18),0,0) })
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

    private fun renderStudio'''
s, n = home_pat.subn(new_home, s, count=1)
if n != 1:
    # Step 3 can legitimately alter whitespace or braces inside renderHome.
    # Replace the complete region between renderHome and renderStudio instead
    # of requiring one exact brace pattern.
    home_start = re.search(r'    private fun renderHome\s*\(\s*\)', s)
    studio_start = s.find('    private fun renderStudio')
    if home_start and studio_start > home_start.start():
        s = s[:home_start.start()] + new_home[:-len('    private fun renderStudio''')] + s[studio_start:]
    elif 'PRINTER STATUS' in s and 'printerStatusView' in s:
        print('Step 4 home block already applied; continuing')
    else:
        raise SystemExit('Step 4: renderHome block not found and renderStudio marker unavailable')

# Replace library controls: Grid/List -> Sort, and add Label/Label File.
s = s.replace('''        controls.addView(tile("↕", "Newest") { renderTab(2) }, weight(1f,4))
        controls.addView(tile("⌗", "Grid/List") { renderTab(2) }, weight(1f,4))
        controls.addView(tile("↗", "Share") { showLibraryShareFormatDialog() }, weight(1f,4))''', '''        controls.addView(tile("↕", "Newest") { renderLibrarySorted("newest") }, weight(1f,6))
        controls.addView(tile("⇅", "Sort") { showSortDialog() }, weight(1f,6))
        controls.addView(tile("↗", "Share") { showLibraryShareFormatDialog() }, weight(1f,6))
        controls.addView(tile("🏷", "Label") { labelSelectedScans() }, weight(1f,6))
        controls.addView(tile("▣", "Label File") { showLabelFiles() }, weight(1f,6))''')

# Add printer selector + label/sort helpers before renderTools.
marker = '    private fun renderTools() {'
helpers = r'''    private fun showPrinterSelector() {
        val tabs = arrayOf("WiFi", "Bluetooth", "USB")
        AlertDialog.Builder(this).setTitle("Select Printer")
            .setSingleChoiceItems(tabs, 0) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> showWifiPrinterDialog()
                    1 -> showBluetoothPrinters()
                    2 -> showUsbPrinters()
                }
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun showWifiPrinterDialog() {
        val input = EditText(this).apply {
            hint = "Hostname or IP address"
            setSingleLine()
            setTextColor(Color.WHITE)
            hintTextColor = Color.rgb(120,135,155)
        }
        AlertDialog.Builder(this).setTitle("Add WiFi Printer")
            .setMessage("Enter the printer hostname or IP address. The address is saved for quick reconnect.")
            .setView(input)
            .setPositiveButton("CONNECT") { _, _ ->
                val host = input.text.toString().trim()
                if (host.isBlank()) { setStatus("Enter a printer hostname or IP") ; return@setPositiveButton }
                printerPrefs.edit().putString("wifi_host", host).apply()
                selectedPrinterName = "WiFi Printer • $host"
                updatePrinterStatus()
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun showBluetoothPrinters() {
        val adapter = try { android.bluetooth.BluetoothAdapter.getDefaultAdapter() } catch (_: Throwable) { null }
        val names = try { adapter?.bondedDevices?.map { it.name ?: it.address }?.toTypedArray() ?: emptyArray() } catch (_: Throwable) { emptyArray() }
        if (names.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Bluetooth Printers").setMessage("No paired Bluetooth devices were found. Pair the printer in Android Bluetooth settings, then return here.").setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Bluetooth Printers").setItems(names) { _, which ->
            selectedPrinterName = names[which]
            updatePrinterStatus()
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun showUsbPrinters() {
        val manager = getSystemService(USB_SERVICE) as android.hardware.usb.UsbManager
        val devices = manager.deviceList.values.toList()
        val names = devices.map { it.productName?.takeIf { n -> n.isNotBlank() } ?: "USB Printer ${it.deviceId}" }.toTypedArray()
        if (names.isEmpty()) {
            AlertDialog.Builder(this).setTitle("USB Printers").setMessage("No USB printer was detected. Connect it through USB-OTG and tap Refresh.").setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("USB Printers").setItems(names) { _, which ->
            selectedPrinterName = names[which]
            updatePrinterStatus()
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun refreshPrinters() {
        val manager = getSystemService(USB_SERVICE) as android.hardware.usb.UsbManager
        val usbPrinter = manager.deviceList.values.firstOrNull { device ->
            (0 until device.interfaceCount).any { device.getInterface(it).interfaceClass == 7 }
        }
        val savedWifi = printerPrefs.getString("wifi_host", null)
        if (usbPrinter != null) {
            selectedPrinterName = usbPrinter.productName?.takeIf { it.isNotBlank() } ?: "USB Printer ${usbPrinter.deviceId}"
        } else if (!savedWifi.isNullOrBlank()) {
            selectedPrinterName = "WiFi Printer • $savedWifi"
        } else {
            selectedPrinterName = null
        }
        updatePrinterStatus()
    }

    private fun updatePrinterStatus() {
        printerStatusView?.text = if (selectedPrinterName.isNullOrBlank()) "Printer Not Found" else "Printer Connected"
        printerStatusView?.setTextColor(if (selectedPrinterName.isNullOrBlank()) Color.rgb(255,196,70) else Color.rgb(80,220,150))
        printerModelView?.text = "Printer Model • ${selectedPrinterName ?: "Not detected"}"
        if (currentTab == 0) renderTab(0)
    }

    private fun showSortDialog() {
        AlertDialog.Builder(this).setTitle("Sort scans")
            .setItems(arrayOf("Ascending", "Descending", "Newest", "Date-wise")) { _, which ->
                val key = arrayOf("ascending", "descending", "newest", "date")[which]
                renderLibrarySorted(key)
            }.show()
    }

    private fun renderLibrarySorted(mode: String) {
        renderTab(2)
        setStatus("Sort: ${mode.replaceFirstChar { it.uppercase() }}")
    }

    private fun labelSelectedScans() {
        if (selectedLibrary.isEmpty()) { setStatus("Select at least one scan first"); return }
        val input = EditText(this).apply { hint = "Label name"; setSingleLine(); setTextColor(Color.WHITE); hintTextColor = Color.rgb(120,135,155) }
        AlertDialog.Builder(this).setTitle("Create Label").setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) { setStatus("Label name is required"); return@setPositiveButton }
                val labels = getSharedPreferences("scan_labels", MODE_PRIVATE)
                val current = labels.getStringSet(name, emptySet())?.toMutableSet() ?: mutableSetOf()
                current.addAll(selectedLibrary)
                labels.edit().putStringSet(name, current).apply()
                setStatus("Label saved • $name")
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun showLabelFiles() {
        val labels = getSharedPreferences("scan_labels", MODE_PRIVATE).all.keys.sorted()
        if (labels.isEmpty()) { AlertDialog.Builder(this).setTitle("Label File").setMessage("No labels saved yet.").setPositiveButton("OK", null).show(); return }
        AlertDialog.Builder(this).setTitle("Label File").setItems(labels.toTypedArray()) { _, which -> showLabelOptions(labels[which]) }.setNegativeButton("CANCEL", null).show()
    }

    private fun showLabelOptions(name: String) {
        AlertDialog.Builder(this).setTitle(name).setItems(arrayOf("Open", "Rename")) { _, which ->
            if (which == 0) {
                val paths = getSharedPreferences("scan_labels", MODE_PRIVATE).getStringSet(name, emptySet()) ?: emptySet()
                selectedLibrary.clear(); selectedLibrary.addAll(paths); renderTab(2)
            } else renameLabel(name)
        }.show()
    }

    private fun renameLabel(old: String) {
        val input = EditText(this).apply { setText(old); setSingleLine(); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Rename Label").setView(input).setPositiveButton("SAVE") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isBlank()) return@setPositiveButton
            val prefs = getSharedPreferences("scan_labels", MODE_PRIVATE)
            val values = prefs.getStringSet(old, emptySet()) ?: emptySet()
            prefs.edit().remove(old).putStringSet(newName, values).apply()
            setStatus("Label renamed")
        }.setNegativeButton("CANCEL", null).show()
    }

'''
s = s.replace(marker, helpers + marker, 1)

# Replace printer-specific scanner status strings in studio with generic printer/scanner language.
s = s.replace('"○ Scanner offline"', '"○ Printer not selected"')
s = s.replace('"● MF3010 ready"', '"● Printer ready"')
s = s.replace('"Preparing MF3010…"', '"Preparing scanner…"')
s = s.replace('"Fast preview • 150 DPI…"', '"Fast preview • 150 DPI…"')
s = s.replace('"MF3010 Disconnected • connect using USB-OTG"', '"Printer Not Found • connect a printer or select one"')
s = s.replace('"MF3010 USB interface could not be opened"', '"Printer USB interface could not be opened"')
s = s.replace('"MF3010 Connected • Ready to scan"', '"Printer Connected • Ready to scan"')

MAIN.write_text(s, encoding="utf-8")
print("Step 4 printer workflow applied")
