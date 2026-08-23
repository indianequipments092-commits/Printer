from pathlib import Path

p = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
if not p.exists():
    raise SystemExit(f'Missing generated MainActivity: {p}')
s = p.read_text(encoding='utf-8')

# finalize_printex removes the zoom state while leaving the zoom handler behind.
# Keep the state because the integrated preview uses it.
if 'private var printexZoom = 1f' not in s and 'private var printexPreview: ImageView? = null' in s:
    s = s.replace('    private var printexPreview: ImageView? = null\n', '    private var printexPreview: ImageView? = null\n    private var printexZoom = 1f\n', 1)

# A Float is not lateinit; the old ::property.isInitialized check is invalid Kotlin.
s = s.replace('if(!::printexZoom.isInitialized){}; ', '')
s = s.replace('if (!::printexZoom.isInitialized) {} ', '')

# Keep the default preview fit-to-page while retaining the existing zoom handler.
needle = 'scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE); setPadding(dp(8),dp(8),dp(8),dp(8))'
replacement = needle + '; setOnTouchListener { v, e -> printexZoomTouch(v, e) }'
if needle in s and 'printexZoomTouch(v, e)' not in s[s.find(needle):s.find(needle)+400]:
    s = s.replace(needle, replacement, 1)

p.write_text(s, encoding='utf-8')
print('Printex generated-source compile guard applied')
