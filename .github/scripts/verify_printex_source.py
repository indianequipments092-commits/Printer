from pathlib import Path

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
if not MAIN.exists():
    raise SystemExit(f'Missing generated MainActivity: {MAIN}')

s = MAIN.read_text(encoding='utf-8')

# Verification only. This script must never modify source.
required = {
    'Printex navigation': 'navButton("▣", "Printex", 3)',
    'Tools navigation': 'navButton("⚙", "Tools", 4)',
    'Printex renderer': 'private fun renderPrintex()',
    'PDF/file picker': 'Intent.ACTION_OPEN_DOCUMENT',
    'PDF renderer': 'android.graphics.pdf.PdfRenderer',
    'Preview': 'printexOpenPreview()',
    'Fit-to-page preview': 'ImageView.ScaleType.FIT_CENTER',
    'Zoom handler': 'private fun printexZoomTouch',
    'Zoom state': 'private var printexZoom = 1f',
    'Preview applies image settings': 'private fun printexPreviewBitmap()',
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
    'Brightness/contrast renderer': 'private fun printexAdjust',
    'Print action': 'actionButton("PRINT")',
    'PrintManager': 'android.print.PrintManager',
    'Paper size print attributes': 'setMediaSize(',
    'Duplex print attributes': 'setDuplexMode(',
    'Copies rendered': 'repeat(printexCopies',
    'Scaling rendered': 'printexScaling',
    'Layout rendered': 'printexLayout',
}
missing = [name for name, token in required.items() if token not in s]
if missing:
    raise SystemExit('Printex V1 verification failed; missing: ' + ', '.join(missing))

for forbidden in ('actionButton("SHARE")', 'printexRotation', '"Rotation"', 'Smart Print', 'SMART PRINT'):
    if forbidden in s:
        raise SystemExit(f'Printex V1 verification failed; forbidden item remains: {forbidden}')

# Old standalone activity must not be referenced anywhere in app source/scripts.
for p in ROOT.rglob('*'):
    if not p.is_file() or p.suffix not in {'.kt', '.java', '.py', '.xml'}:
        continue
    text = p.read_text(encoding='utf-8', errors='ignore')
    if 'PrintExActivity' in text:
        raise SystemExit(f'Legacy PrintExActivity reference remains in {p}')

# Catch the exact regression that caused the previous loop: a Float must never be
# treated as lateinit, and the generated print writer must have FileInputStream.
if '::printexZoom.isInitialized' in s:
    raise SystemExit('Printex V1 verification failed: invalid lateinit check on Float printexZoom')
if 'FileInputStream' not in s:
    raise SystemExit('Printex V1 verification failed: FileInputStream missing from print writer')

print('Printex V1 source verification passed: navigation, fit preview/zoom, Library, settings, rendered effects, printing, and forbidden-feature checks are present.')
