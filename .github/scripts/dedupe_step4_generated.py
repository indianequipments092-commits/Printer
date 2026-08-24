from pathlib import Path
import re

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
s = MAIN.read_text(encoding='utf-8')

# Keep only the first class-level declaration for known Step 3/4 shared state.
seen = False
def keep_first_library_sort(match):
    global seen
    if not seen:
        seen = True
        return match.group(0)
    return ''
s = re.sub(r'^    private var librarySortMode\s*=\s*"newest"\s*\n', keep_first_library_sort, s, flags=re.M)

# Remove duplicate method definitions while preserving the first implementation.
def method_bounds(src, name, start_at=0):
    marker = f'    private fun {name}('
    start = src.find(marker, start_at)
    if start < 0:
        return None
    brace = src.find('{', start)
    if brace < 0:
        return None
    depth = 0
    for i in range(brace, len(src)):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return start, i + 1
    raise SystemExit(f'Unbalanced method: {name}')

for name in ('showSortDialog', 'renameLabel', 'saveCurrentSettingDialog'):
    first = method_bounds(s, name)
    if not first:
        continue
    cursor = first[1]
    while True:
        dup = method_bounds(s, name, cursor)
        if not dup:
            break
        s = s[:dup[0]] + s[dup[1]:]
        cursor = dup[0]

# renderStudio can receive the Step 3 settings row and the Step 4 settings row.
# Keep the first complete row so the generated Kotlin has no conflicting local val.
studio_start = s.find('    private fun renderStudio(')
studio_end = s.find('    private fun renderLibrary(', studio_start)
if studio_start >= 0 and studio_end > studio_start:
    studio = s[studio_start:studio_end]
    blocks = []
    pos = 0
    marker = '        val settingRow = row()'
    while True:
        start = studio.find(marker, pos)
        if start < 0:
            break
        end_marker = '        content.addView(settingRow, margin(0,8))'
        end = studio.find(end_marker, start)
        if end < 0:
            end_marker = '        content.addView(settingRow, margin(0,6))'
            end = studio.find(end_marker, start)
        if end < 0:
            break
        end += len(end_marker)
        blocks.append((start, end))
        pos = end
    if len(blocks) > 1:
        # Delete later blocks from right to left to preserve offsets.
        for start, end in reversed(blocks[1:]):
            studio = studio[:start] + studio[end:]
        s = s[:studio_start] + studio + s[studio_end:]

MAIN.write_text(s, encoding='utf-8')
print('Step 4 generated Kotlin duplicate declarations removed')
