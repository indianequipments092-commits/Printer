from pathlib import Path
import base64

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'
LOGO_B64 = Path('../.github/assets/app_logo.webp.b64')
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

replace_once(MAIN, 'setPadding(dp(18), dp(16), dp(18), dp(18))', 'setPadding(dp(18), dp(40), dp(18), dp(18))')
replace_once(MAIN, '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)\n        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            else -> renderTools()\n        }''', '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("▣", "Printex", 3)\n        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            3 -> startActivity(Intent(this, PrintExActivity::class.java))\n        }''')
replace_once(MAIN, '''                library.savePage(result.bitmap, "scan_${System.currentTimeMillis()}.jpg")\n                runOnUiThread {\n                    pages.add(result.bitmap)''', '''                var finalBitmap = result.bitmap\n                if (brightness != 0f || contrast != 1f) {\n                    val adjusted = library.applyAdjustments(finalBitmap, brightness, contrast)\n                    if (!finalBitmap.isRecycled) finalBitmap.recycle()\n                    finalBitmap = adjusted\n                }\n                if (grayscale) {\n                    val gray = document.grayscale(finalBitmap)\n                    if (!finalBitmap.isRecycled) finalBitmap.recycle()\n                    finalBitmap = gray\n                }\n                library.savePage(finalBitmap, "scan_${System.currentTimeMillis()}.jpg")\n                runOnUiThread {\n                    pages.add(finalBitmap)''')
main_text = MAIN.read_text(encoding='utf-8')
if 'getIntExtra("open_tab"' not in main_text:
    replace_once(MAIN, '''        buildShell()\n        usb.register()\n        refreshUsb()''', '''        buildShell()\n        usb.register()\n        refreshUsb()\n        val openTab = intent.getIntExtra("open_tab", -1)\n        if (openTab in 0..2) renderTab(openTab)''')

ms = MANIFEST.read_text(encoding='utf-8')
if 'android:icon="@drawable/app_logo"' not in ms:
    ms = ms.replace('<application android:theme="@style/AppTheme" android:label="USB Scanner"', '<application android:theme="@style/AppTheme" android:label="USB Scanner" android:icon="@drawable/app_logo"', 1)
MANIFEST.write_text(ms, encoding='utf-8')
print('Applied safe-area, scanner persistence, Printex routing, and unique app-logo fixes')
