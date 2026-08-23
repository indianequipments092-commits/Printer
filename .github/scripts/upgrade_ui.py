from pathlib import Path
import base64

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'
LOGO_B64 = Path('.github/assets/app_logo.webp.b64')
RES = ROOT / 'app/src/main/res/drawable'
LOGO_OUT = RES / 'app_logo.webp'
LOGO_XML = RES / 'app_logo.xml'


def replace_once(path, old, new):
    text = path.read_text(encoding='utf-8')
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding='utf-8')


RES.mkdir(parents=True, exist_ok=True)
if LOGO_B64.exists():
    LOGO_OUT.write_bytes(base64.b64decode(LOGO_B64.read_text(encoding='utf-8')))
LOGO_XML.unlink(missing_ok=True)

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

# App logo only. No Printex activity, routing, patcher, or Printex resources are added here.
ms = MANIFEST.read_text(encoding='utf-8')
if 'android:icon="@drawable/app_logo"' not in ms:
    ms = ms.replace('<application android:theme="@style/AppTheme" android:label="USB Scanner"', '<application android:theme="@style/AppTheme" android:label="USB Scanner" android:icon="@drawable/app_logo"', 1)
MANIFEST.write_text(ms, encoding='utf-8')

print('Applied scanner safe-area, scan persistence, scanner routing, and app-logo fixes')
