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

def decode_asset(src, dst):
    if not src.exists():
        return
    raw = src.read_bytes()
    try:
        decoded = base64.b64decode(raw, validate=True)
        if decoded:
            dst.write_bytes(decoded)
            return
    except Exception:
        pass
    dst.write_bytes(raw)

def replace_once(path, old, new):
    text = path.read_text(encoding='utf-8')
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding='utf-8')
        return True
    return False

RES.mkdir(parents=True, exist_ok=True)
if LOGO_B64.exists():
    decode_asset(LOGO_B64, LOGO_OUT)
LOGO_XML.unlink(missing_ok=True)
if PRINTER_ART_B64.exists():
    decode_asset(PRINTER_ART_B64, PRINTER_ART_OUT)

replace_once(MAIN, 'setPadding(dp(18), dp(16), dp(18), dp(18))', 'setPadding(dp(18), dp(40), dp(18), dp(18))')
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

main_text = MAIN.read_text(encoding='utf-8')
if 'getIntExtra("open_tab"' not in main_text:
    replace_once(MAIN, '''        buildShell()
        usb.register()
        refreshUsb()''', '''        buildShell()
        usb.register()
        refreshUsb()
        val openTab = intent.getIntExtra("open_tab", -1)
        if (openTab in 0..2) renderTab(openTab)''')

print('Applied scanner UI updates and safe binary/base64 asset decoding')
