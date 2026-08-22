from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
LIB = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/ScanLibrary.kt"


def sub(path, old, new):
    s = path.read_text(encoding="utf-8")
    if old in s:
        path.write_text(s.replace(old, new, 1), encoding="utf-8")
        print("applied:", path.name, old[:50].replace("\n", " "))
    else:
        print("skip:", path.name)

# Move all screen content 24dp below the status/camera area.
sub(MAIN, "setPadding(dp(18), dp(16), dp(18), dp(18))", "setPadding(dp(18), dp(40), dp(18), dp(18))")

# Remove Tools from the visible bottom navigation.
sub(MAIN, '        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)\n', '        navButton("▦", "Library", 2)\n')

# Preserve the user's brightness/contrast/grayscale choices for the final scan.
s = MAIN.read_text(encoding="utf-8")
s = s.replace(
    '        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)\n        scanning = true',
    '        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)\n        val selectedBrightness = brightness\n        val selectedContrast = contrast\n        val selectedGrayscale = grayscale\n        scanning = true', 1)
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
                    val adjusted = library.applyAdjustments(finalBitmap, selectedBrightness, selectedContrast)
                    finalBitmap = adjusted
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

# Make 1.0 the neutral contrast value instead of accidentally turning it into 2.0.
sub(LIB, "val c = (contrast + 1f).coerceAtLeast(0.05f)", "val c = contrast.coerceAtLeast(0.05f)")
print("USB Scanner fixes ready")
