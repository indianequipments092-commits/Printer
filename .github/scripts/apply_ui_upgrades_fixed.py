from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
LIB = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/ScanLibrary.kt"
PRINTEX = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PrintExActivity.kt"


def sub(path, old, new):
    s = path.read_text(encoding="utf-8")
    if old in s:
        path.write_text(s.replace(old, new, 1), encoding="utf-8")
        print("applied:", path.name)
    else:
        print("skip:", path.name, old[:60].replace("\n", " "))

# Keep all main-screen content safely below the Android status/camera area.
sub(MAIN, "setPadding(dp(18), dp(16), dp(18), dp(18))", "setPadding(dp(18), dp(40), dp(18), dp(18))")

# Replace the old Tools tab with the new dedicated Printex tab.
sub(
    MAIN,
    '        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)\n',
    '        navButton("▦", "Library", 2)\n        navButton("▣", "Printex", 3)\n'
)
sub(MAIN, "            else -> renderTools()\n", "            else -> { /* Printex is a dedicated Activity */ }\n")

# IMPORTANT: qualify the Activity receiver explicitly. Inside the click-listener
# lambda Kotlin can resolve `this` to the wrong receiver, producing the
# Intent(String, Uri) constructor error seen during compilation.
sub(
    MAIN,
    "            setOnClickListener { renderTab(tab) }",
    "            setOnClickListener { if (tab == 3) startActivity(Intent(this@MainActivity, PrintExActivity::class.java)) else renderTab(tab) }"
)

# Preserve brightness/contrast/grayscale choices in the actual scanned image.
s = MAIN.read_text(encoding="utf-8")
s = s.replace(
    '        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)\n        scanning = true',
    '        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)\n        val selectedBrightness = brightness\n        val selectedContrast = contrast\n        val selectedGrayscale = grayscale\n        scanning = true',
    1,
)
old = '''                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                library.savePage(result.bitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(result.bitmap)
'''
new = '''                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                var finalBitmap = result.bitmap
                if (selectedBrightness != 0f || selectedContrast != 1f) {
                    finalBitmap = library.applyAdjustments(finalBitmap, selectedBrightness, selectedContrast)
                }
                if (selectedGrayscale) {
                    val gray = document.grayscale(finalBitmap)
                    if (finalBitmap !== result.bitmap && !finalBitmap.isRecycled) finalBitmap.recycle()
                    finalBitmap = gray
                }
                library.savePage(finalBitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(finalBitmap)
'''
if old in s:
    s = s.replace(old, new, 1)
    print("applied: final scan image processing")
else:
    print("skip: final scan image processing")
MAIN.write_text(s, encoding="utf-8")

# Make the same receiver qualification explicit in Printex's Scan shortcut.
if PRINTEX.exists():
    p = PRINTEX.read_text(encoding="utf-8")
    old_intent = "Intent(this, MainActivity::class.java)"
    new_intent = "Intent(this@PrintExActivity, MainActivity::class.java)"
    if old_intent in p:
        PRINTEX.write_text(p.replace(old_intent, new_intent), encoding="utf-8")
        print("applied: PrintExActivity Intent receiver")
    else:
        print("skip: PrintExActivity Intent receiver")

# 1.0 is the neutral contrast value.
sub(LIB, "val c = (contrast + 1f).coerceAtLeast(0.05f)", "val c = contrast.coerceAtLeast(0.05f)")
print("USB Scanner + Printex fixes ready")
