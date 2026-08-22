from pathlib import Path

# The PDF Reader implementation is committed directly in the Android source.
# This build-time script only makes the main screen respect the status-bar /
# front-camera safe area and enforces the requested 100x maximum zoom.
MAIN = Path("ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt")
READER = Path("ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PdfReaderActivity.kt")

main = MAIN.read_text(encoding="utf-8")
old_shell = '''        root.addView(nav, LinearLayout.LayoutParams(-1, dp(82)))
        setContentView(root)
        renderTab(0)'''
new_shell = '''        root.addView(nav, LinearLayout.LayoutParams(-1, dp(82)))
        root.setOnApplyWindowInsetsListener { _, insets ->
            content.setPadding(dp(18), insets.systemWindowInsetTop + dp(12), dp(18), dp(18))
            insets
        }
        setContentView(root)
        root.requestApplyInsets()
        renderTab(0)'''
if old_shell in main:
    main = main.replace(old_shell, new_shell, 1)

old_padding = 'setPadding(dp(18), dp(38), dp(18), dp(18))'
if old_padding in main:
    main = main.replace(old_padding, 'setPadding(dp(18), dp(12), dp(18), dp(18))', 1)
MAIN.write_text(main, encoding="utf-8")

reader = READER.read_text(encoding="utf-8")
reader = reader.replace('10000f', '100f')
reader = reader.replace('50% up to 10000%', '50% up to 100%')
READER.write_text(reader, encoding="utf-8")

print("Safe-area fix applied; PDF Reader is capped at 100x zoom")
