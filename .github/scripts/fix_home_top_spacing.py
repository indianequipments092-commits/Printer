from pathlib import Path

path = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
text = path.read_text(encoding='utf-8')

old = '        content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"))'
new = '''        content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"), LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(24), 0, 0)
        })'''

if new in text:
    print('Home title spacing already fixed')
elif old in text:
    path.write_text(text.replace(old, new, 1), encoding='utf-8')
    print('Moved USB SCANNER title safely below the status/camera area')
else:
    raise SystemExit('USB SCANNER title block not found')
