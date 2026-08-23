from pathlib import Path

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
RES = ROOT / 'app/src/main/res/drawable'
RES.mkdir(parents=True, exist_ok=True)

def replace(path, old, new):
    if not path.exists():
        print(f'Skipping missing optional file: {path}')
        return
    s = path.read_text(encoding='utf-8')
    if old in s:
        path.write_text(s.replace(old, new, 1), encoding='utf-8')

replace(MAIN, '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("▣", "Printex", 3)''', '''        navButton(R.drawable.ic_home, "Home", 0)\n        navButton(R.drawable.ic_studio, "Studio", 1)\n        navButton(R.drawable.ic_library, "Library", 2)\n        navButton(R.drawable.ic_printer, "Printex", 3)''')

# Standalone PrintExActivity was intentionally removed. Never require it here.

icons = {
'ic_home.xml': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="28dp" android:height="28dp" android:viewportWidth="64" android:viewportHeight="64"><path android:fillColor="#FFFFFF" android:pathData="M8,29 L32,8 L56,29 L52,34 L52,55 L12,55 L12,34 Z"/><path android:fillColor="#287CC9" android:pathData="M27,55 L27,39 L37,39 L37,55 Z"/></vector>',
'ic_studio.xml': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="28dp" android:height="28dp" android:viewportWidth="64" android:viewportHeight="64"><path android:fillColor="#168FF5" android:pathData="M9,35 L18,20 L26,22 L24,14 L35,9 L45,18 L55,13 L57,25 L49,33 L54,42 L43,50 L33,44 L23,53 L14,46 Z"/></vector>',
'ic_library.xml': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="28dp" android:height="28dp" android:viewportWidth="64" android:viewportHeight="64"><path android:fillColor="#2E73C9" android:pathData="M7,16 L47,25 L47,53 L7,44 Z"/><path android:fillColor="#63A7FF" android:pathData="M17,9 L55,18 L55,46 L47,53 L47,25 L17,18 Z"/></vector>',
'ic_printer.xml': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="28dp" android:height="28dp" android:viewportWidth="64" android:viewportHeight="64"><path android:fillColor="#2F67C0" android:pathData="M17,20 L47,20 L47,31 L17,31 Z"/><path android:fillColor="#3978FF" android:pathData="M11,27 L53,27 Q58,27 58,33 L58,46 Q58,51 53,51 L11,51 Q6,51 6,46 L6,33 Q6,27 11,27 Z"/><path android:fillColor="#D8E4F7" android:pathData="M17,39 L47,39 L47,57 L17,57 Z"/></vector>'
}
for name, data in icons.items():
    (RES / name).write_text(data, encoding='utf-8')
print('Applied scanner/Printex icons')
