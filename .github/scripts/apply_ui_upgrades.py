from pathlib import Path
import base64

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
LIB = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/ScanLibrary.kt"
MANIFEST = ROOT / "ScannerApp/app/src/main/AndroidManifest.xml"
LOGO = ROOT / "ScannerApp/app/src/main/res/drawable/usb_scanner_logo.png"

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f"Patch target not found: {label}")
    return text.replace(old, new, 1)

main = MAIN.read_text(encoding="utf-8")
main = replace_once(main, 'setPadding(dp(18), dp(16), dp(18), dp(18))',
                    'setPadding(dp(18), dp(30), dp(18), dp(18))', "move content slightly down")
main = replace_once(main, 'content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"))',
                    'content.addView(homeHeader())', "home header")

tile_old = """    private fun tile(icon: String, text: String, action: () -> Unit) = Button(this).apply {
        this.text = "$icon\\n$text"; textSize = 11f; setTextColor(Color.rgb(215,225,238)); isAllCaps = false
        background = rounded(Color.rgb(18,25,36),16); setOnClickListener { action() }; minHeight = 0; minimumHeight = 0
    }"""
tile_new = """    private fun tile(icon: String, text: String, action: () -> Unit): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(8), dp(4), dp(8))
            background = rounded(Color.rgb(18,25,36),16)
            setOnClickListener { action() }
        }
        tile.addView(TextView(this).apply {
            this.text = icon
            gravity = Gravity.CENTER
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(220,230,242))
        }, LinearLayout.LayoutParams(-1, dp(34)))
        tile.addView(TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(215,225,238))
            setPadding(0, dp(2), 0, 0)
        }, LinearLayout.LayoutParams(-1, dp(24)))
        return tile
    }"""
main = replace_once(main, tile_old, tile_new, "larger page/action icons")

home_header = """    private fun homeHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(6))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.usb_scanner_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(8,20,38),14)
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(76), dp(76)).apply {
            setMargins(0, 0, dp(14), 0)
        })
        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        text.addView(TextView(this@MainActivity).apply {
            text = "USB SCANNER"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        text.addView(TextView(this@MainActivity).apply {
            text = "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(105,145,190))
            setPadding(0, dp(4), 0, 0)
        })
        header.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        return header
    }

"""
main = replace_once(main, '    private fun renderHome() {', home_header + '    private fun renderHome() {', "home logo header helper")

processed_old = """    private fun processedPreview(): Bitmap {
        var out = pages[currentPage]
        if (brightness != 0f || contrast != 1f) out = library.applyAdjustments(out, brightness, contrast)
        if (grayscale) {
            val g = document.grayscale(out)
            if (out !== pages[currentPage]) out.recycle()
            out = g
        }
        return out
    }

    private fun updatePreview() {
        imagePreview?.setImageBitmap(if (hasPage()) processedPreview() else previewBitmap)
        pageCount?.text = "${pages.size} pages"
    }"""
processed_new = """    private fun processedPreview(): Bitmap? {
        var out = if (hasPage()) pages[currentPage] else previewBitmap ?: return null
        if (brightness != 0f || contrast != 1f) out = library.applyAdjustments(out, brightness, contrast)
        if (grayscale) {
            val g = document.grayscale(out)
            if (out !== pages.getOrNull(currentPage) && out !== previewBitmap) out.recycle()
            out = g
        }
        return out
    }

    private fun updatePreview() {
        imagePreview?.setImageBitmap(processedPreview())
        pageCount?.text = "${pages.size} pages"
    }

    private fun applyCurrentEdits() {
        if (!hasPage()) return
        if (brightness == 0f && contrast == 1f && !grayscale) return
        val edited = processedPreview() ?: return
        val old = pages[currentPage]
        pages[currentPage] = edited
        if (old !== edited && !old.isRecycled) old.recycle()
        brightness = 0f
        contrast = 1f
        grayscale = false
    }"""
main = replace_once(main, processed_old, processed_new, "preview adjustments")

main = replace_once(main,
    '    private fun savePdf() {\n        if (pages.isEmpty()) { setStatus("No pages to export"); return }',
    '    private fun savePdf() {\n        if (pages.isEmpty()) { setStatus("No pages to export"); return }\n        applyCurrentEdits()',
    "apply edits before PDF export")
main = replace_once(main,
    '    private fun sharePdf() {\n        if (pages.isEmpty()) return',
    '    private fun sharePdf() {\n        if (pages.isEmpty()) return\n        applyCurrentEdits()',
    "apply edits before PDF share")
main = replace_once(main,
    '    private fun shareImage(format: String) {\n        if (!hasPage()) { setStatus("Select a page first"); return }',
    '    private fun shareImage(format: String) {\n        if (!hasPage()) { setStatus("Select a page first"); return }\n        applyCurrentEdits()',
    "apply edits before image share")
MAIN.write_text(main, encoding="utf-8")

lib = LIB.read_text(encoding="utf-8")
lib_old = """    fun applyAdjustments(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val c = (contrast + 1f).coerceAtLeast(0.05f)
        val translate = (-0.5f * c + 0.5f) * 255f + brightness * 255f
        val matrix = ColorMatrix(floatArrayOf(
            c,0f,0f,0f,translate, 0f,c,0f,0f,translate, 0f,0f,c,0f,translate, 0f,0f,0f,1f,0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }"""
lib_new = """    fun applyAdjustments(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val c = contrast.coerceIn(0.05f, 3f)
        val translate = (1f - c) * 127.5f + brightness.coerceIn(-1f, 1f) * 255f
        val matrix = ColorMatrix(floatArrayOf(
            c,0f,0f,0f,translate,
            0f,c,0f,0f,translate,
            0f,0f,c,0f,translate,
            0f,0f,0f,1f,0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }"""
lib = replace_once(lib, lib_old, lib_new, "correct brightness/contrast matrix")
LIB.write_text(lib, encoding="utf-8")

manifest = MANIFEST.read_text(encoding="utf-8")
manifest = replace_once(
    manifest,
    '<application android:theme="@style/AppTheme" android:label="USB Scanner" android:allowBackup="false" android:supportsRtl="true">',
    '<application android:theme="@style/AppTheme" android:label="USB Scanner" android:icon="@drawable/usb_scanner_logo" android:roundIcon="@drawable/usb_scanner_logo" android:allowBackup="false" android:supportsRtl="true">',
    "application icon",
)
MANIFEST.write_text(manifest, encoding="utf-8")

LOGO.parent.mkdir(parents=True, exist_ok=True)
LOGO.write_bytes(base64.b64decode("""LOGO_B64"""))
'''
script = script.replace('"""LOGO_B64"""', '"""' + logo_b64 + '"""')

workflow = """name: Build USB Scanner APK

on:
  push:
    paths:
      - 'ScannerApp/**'
      - '.github/workflows/build-scanner.yml'
      - '.github/scripts/apply_ui_upgrades.py'
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ScannerApp
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'
      - name: Apply UI and image-edit fixes
        run: python3 ../.github/scripts/apply_ui_upgrades.py
      - name: Build debug APK
        run: gradle :app:assembleDebug --no-daemon
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: usb-scanner-debug-apk
          path: ScannerApp/app/build/outputs/apk/debug/app-debug.apk
"""
Path('/mnt/data/apply_ui_upgrades.py').write_text(script, encoding='utf-8')
print(len(script.encode()), len(workflow.encode()))
