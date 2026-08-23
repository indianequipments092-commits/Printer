from pathlib import Path

main = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
if not main.exists():
    raise SystemExit(f'Missing generated MainActivity: {main}')
s = main.read_text(encoding='utf-8')
required = {
    'renderPrintex': 'private fun renderPrintex()',
    'Printex tab': '"Printex"',
    'PDF/file picker': 'ACTION_OPEN_DOCUMENT',
    'PDF renderer': 'PdfRenderer',
    'Print settings': 'private fun printexPrintSettings()',
    'Image settings': 'IMAGE SETTINGS',
    'Brightness': 'Brightness',
    'Contrast': 'Contrast',
    'Default print settings': 'Print Settings restored to default',
    'Print action': 'actionButton("PRINT")',
    'Share action': 'actionButton("SHARE")',
    'Duplex': 'Duplex',
    'Copies': 'Copies',
    'Pages': 'Pages',
    'Paper size': 'Paper Size',
    'Orientation': 'Orientation',
    'Scaling': 'Scaling',
    'Multi-page': 'Multi-Page Printing',
}
missing = [name for name, token in required.items() if token not in s]
if missing:
    raise SystemExit('Printex verification failed; missing: ' + ', '.join(missing))

# Standalone Printex must never be a build dependency.
for p in Path('ScannerApp').rglob('*'):
    if p.is_file() and p.suffix in {'.kt', '.java', '.py', '.xml'}:
        try:
            text = p.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue
        if 'PrintExActivity.kt' in text or 'PrintExActivity' in text:
            raise SystemExit(f'Legacy PrintExActivity reference remains in {p}')

# Smart Print is intentionally not part of the approved UI.
if 'Smart Print' in s or 'SMART PRINT' in s:
    raise SystemExit('Smart Print must not be present')

print('Printex verification passed: integrated navigation, print settings, image settings, PDF preview, print/share, and no legacy dependency.')
