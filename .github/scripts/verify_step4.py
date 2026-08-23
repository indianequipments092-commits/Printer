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
    "Label": '"Label"',
    "Label File": '"Label File"',
    "Sort": '"Sort"',
    "PDF share": 'exportSelectedPdf()',
    "Persistent settings": 'getSharedPreferences("saved_scan_settings"',
    "Persistent labels": 'getSharedPreferences("scan_labels"',
}
missing = [name for name, marker in required.items() if marker not in s]
if missing:
    raise SystemExit("Step 4 verification failed; missing: " + ", ".join(missing))
print("Step 4 verification passed: printer workflow, settings, sort/share and labels are present")
