from pathlib import Path

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
if not MAIN.exists():
    raise SystemExit(f'Missing required file: {MAIN}')
s = MAIN.read_text(encoding='utf-8')

marker = '    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {'
while marker in s:
    start = s.find(marker)
    brace = s.find('{', start)
    if brace < 0:
        break
    depth = 0
    end = None
    for i in range(brace, len(s)):
        if s[i] == '{': depth += 1
        elif s[i] == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None: break
    block = s[start:end]
    if 'requestCode == 8101' in block and 'printexUri' in block:
        s = s[:start] + s[end:]
    else:
        break

branch = '''        if (requestCode == 8101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            printexUri = uri
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            printexFile = copyPrintexUriToCache(uri)
            renderTab(3)
        }
'''
if 'requestCode == 8101 && resultCode == RESULT_OK' not in s:
    pos = s.find(marker)
    if pos < 0: raise SystemExit('Existing onActivityResult callback not found')
    body = s.find('{', pos)
    if body < 0: raise SystemExit('Could not locate onActivityResult body')
    s = s[:body + 1] + '\n' + branch + s[body + 1:]

s = s.replace('if(!::printexZoom.isInitialized){}; ', '')
s = s.replace('if (!::printexZoom.isInitialized) {}\n', '')
if 'import java.io.FileInputStream' not in s:
    s = s.replace('import java.io.File\n', 'import java.io.File\nimport java.io.FileInputStream\n')

MAIN.write_text(s, encoding='utf-8')
print('Repaired Printex activity-result integration and Kotlin compile issues')
