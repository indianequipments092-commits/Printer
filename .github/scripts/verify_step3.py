from pathlib import Path

main = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt').read_text(encoding='utf-8')
checks = {
    'Printer status': 'PRINTER STATUS',
    'Printer not found': 'Printer Not Found',
    'Printer connected': 'Printer Connected',
    'Select Printer': 'SELECT PRINTER',
    'Refresh': '↻  REFRESH',
    'Sort': 'showSortDialog()',
    'Label': 'showLabelDialogForSelection()',
    'Label File': 'showLabelFileDialog()',
    'Save Setting': 'saveCurrentSettingDialog()',
    'Default settings': 'resetScanSettingsUi()',
    'Saved settings menu': 'showSavedSettingsDialog()',
    'Change Settings': 'showChangeSettingDialog(name)',
    'Rename setting': 'renameSavedSetting(name)',
    'Brightness applied to scan': 'library.applyAdjustments(finalBitmap, captureBrightness, captureContrast)',
    'Contrast applied to preview': 'library.applyAdjustments(shown, captureBrightness, captureContrast)',
    'Selected PDF share': 'exportSelectedPdf()',
    'PNG/JPEG/JPG share': 'exportSelectedImages(',
}
missing = [name for name, needle in checks.items() if needle not in main]
if missing:
    print('Step 3 verification failed; missing: ' + ', '.join(missing))
    raise SystemExit(1)
print('Step 3 source verification passed: all required UI and functional hooks are present')
