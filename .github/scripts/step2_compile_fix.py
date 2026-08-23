from pathlib import Path

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
text = MAIN.read_text(encoding='utf-8')
old = '''        val tabButtons = listOf(\n            actionButton("WiFi") { showPrinterSource(body, 0, dialog, tabButtons) },\n            actionButton("Bluetooth") { showPrinterSource(body, 1, dialog, tabButtons) },\n            actionButton("USB") { showPrinterSource(body, 2, dialog, tabButtons) }\n        )'''
new = '''        lateinit var tabButtons: List<Button>\n        tabButtons = listOf(\n            actionButton("WiFi") { showPrinterSource(body, 0, dialog, tabButtons) },\n            actionButton("Bluetooth") { showPrinterSource(body, 1, dialog, tabButtons) },\n            actionButton("USB") { showPrinterSource(body, 2, dialog, tabButtons) }\n        )'''
if old in text:
    MAIN.write_text(text.replace(old, new, 1), encoding='utf-8')
    print('Applied Step 2 Kotlin initialization fix')
else:
    print('Step 2 Kotlin initialization fix already applied or selector block changed')
