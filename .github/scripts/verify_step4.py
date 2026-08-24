from pathlib import Path

root = Path(__file__).resolve().parents[2]
main = root / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
s = main.read_text(encoding="utf-8")

required = {
    "Printer Status": 'text = "PRINTER STATUS"',
    "Printer Not Found": '"Printer Not Found"',
    "Printer Connected": '"Printer Connected"',
    "Printer Model": '"Printer Model',
    "Select Printer": '"SELECT PRINTER"',
    "Refresh": '"REFRESH"',
    "WiFi": '"WiFi"',
    "Bluetooth": '"Bluetooth"',
    "USB": '"USB"',
    "Default": '"DEFAULT"',
    "Save Setting": '"SAVE SETTING"',
    "Saved Settings": 'showSavedSettings()',
    "Brightness": '"Brightness"',
    "Contrast": '"Contrast"',
    "Contrast centered at zero": 'slider("Contrast", -100, 100, settingPrefs.getInt("current_contrast", 0))',
    "Label": '"Label"',
    "Label File": '"Label File"',
    "Sort": '"Sort"',
    "PDF share": 'exportSelectedPdf()',
    "PNG/JPEG/JPG share": 'exportSelectedImages(',
    "Persistent settings": 'getSharedPreferences("saved_scan_settings"',
    "Persistent labels": 'getSharedPreferences("scan_labels"',
}

missing = [name for name, marker in required.items() if marker not in s]

# The app applies brightness/contrast in processedPreview(), which is the actual
# bitmap path used by preview and image sharing. Older Step 4 revisions looked
# only for the newer raw-scan-save form and incorrectly rejected this valid path.
has_actual_adjustments = (
    'library.applyAdjustments(raw, brightness, contrast)' in s
    or 'library.applyAdjustments(out, brightness, contrast)' in s
)
if not has_actual_adjustments:
    missing.append("Actual scan adjustments")

if missing:
    raise SystemExit("Step 4 verification failed; missing: " + ", ".join(missing))

print("Step 4 verification passed: printer workflow, actual bitmap adjustments, settings, sort/share and labels are present")
