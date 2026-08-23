from pathlib import Path

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
if not MAIN.exists():
    raise SystemExit(f'Missing generated MainActivity: {MAIN}')

s = MAIN.read_text(encoding='utf-8')

# This step is verification only: it must never silently patch source.
required = {
    'Printex navigation': 'navButton("▣", "Printex", 3)',
    'Tools navigation': 'navButton("⚙", "Tools", 4)',
    'Printex renderer': 'private fun renderPrintex()',
    'PDF/file picker': 'Intent.ACTION_OPEN_DOCUMENT',
    'PDF renderer': 'android.graphics.pdf.PdfRenderer',
    'Preview': 'printexOpenPreview()',
    'Fit-to-page preview': 'ImageView.ScaleType.FIT_CENTER',
    'Zoom handler': 'printexZoomTouch',
    'Library entry': 'printexOpenLibrary()',
    'Library selection': 'printexUseSelectedLibraryFile()',
    'Print settings': 'private fun printexPrintSettings()',
    'Copies': 'printexCopies',
    'Pages': 'printexPages',
    'Manual page range': 'printexPageSpec',
    'Page parser': 'private fun printexPageIndices',
    'Paper size': 'printexPaper',
    'Orientation': 'printexOrientation',
    'Scaling': 'printexScaling',
    'Layout': 'printexLayout',
    'Duplex': 'printexDuplex',
    'Brightness': 'printexBrightness',
    'Contrast': 'printexContrast',
    'Default': 'actionButton("DEFAULT")',
    'Rendered print PDF': 'private fun printexPrintPdf()',
    'Print action': 'actionButton("PRINT")',
    'PrintManager': 'android.print.PrintManager',
}
missing = [name for name, token in required.items() if token not in s]
if missing:
    raise SystemExit('Printex V1 verification failed; missing: ' + ', '.join(missing))

for forbidden in ('actionButton("SHARE")', 'printexRotation', '"Rotation"', 'Smart Print', 'SMART PRINT'):
    if forbidden in s:
        raise SystemExit(f'Printex V1 verification failed; forbidden item remains: {forbidden}')

# The old standalone activity must not be referenced anywhere in the app or scripts.
for p in ROOT.rglob('*'):
    if not p.is_file() or p.suffix not in {'.kt', '.java', '.py', '.xml'}:
        continue
    text = p.read_text(encoding='utf-8', errors='ignore')
    if 'PrintExActivity' in text:
        raise SystemExit(f'Legacy PrintExActivity reference remains in {p}')

# Basic structural sanity checks catch accidental truncated/generated Kotlin.
for token in ('private fun renderPrintex()', 'private fun printexPrintPdf()', 'private fun printexPrint()'):
    start = s.find(token)
    if start < 0:
        raise SystemExit(f'Missing function body: {token}')
    if s.find('{', start) < 0:
        raise SystemExit(f'Function has no opening brace: {token}')

print('Printex V1 source verification passed: integrated navigation, preview, Library, settings, rendering, printing, and forbidden-feature checks are present.')
