from pathlib import Path

ROOT = Path('ScannerApp')
MAIN = ROOT / 'app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'

text = MAIN.read_text(encoding='utf-8')

# Step 2 keeps the Home redesign intact and replaces the temporary selector
# with a real three-source printer selection screen.
imports = 'import android.bluetooth.BluetoothAdapter\n'
if imports not in text:
    text = text.replace('import android.app.Activity\n', 'import android.app.Activity\n' + imports, 1)

selector = r'''    private fun showPrinterSelector() {
        val dialog = AlertDialog.Builder(this).setTitle("Select Printer").create()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(12))
        }

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val body = FrameLayout(this)
        lateinit var tabButtons: List<Button>
        tabButtons = listOf(
            actionButton("WiFi") { showPrinterSource(body, 0, dialog, tabButtons) },
            actionButton("Bluetooth") { showPrinterSource(body, 1, dialog, tabButtons) },
            actionButton("USB") { showPrinterSource(body, 2, dialog, tabButtons) }
        )
        tabButtons.forEach { tabs.addView(it, weight(1f, 3)) }
        root.addView(tabs)
        root.addView(body, LinearLayout.LayoutParams(-1, dp(390)))
        dialog.setView(root)
        dialog.show()
        showPrinterSource(body, 0, dialog, tabButtons)
    }

    private fun showPrinterSource(body: FrameLayout, source: Int, dialog: AlertDialog, tabs: List<Button>) {
        body.removeAllViews()
        tabs.forEachIndexed { index, button ->
            button.alpha = if (index == source) 1f else 0.55f
        }
        when (source) {
            0 -> renderWifiPrinters(body, dialog)
            1 -> renderBluetoothPrinters(body, dialog)
            else -> renderUsbPrinters(body, dialog)
        }
    }

    private fun renderWifiPrinters(body: FrameLayout, dialog: AlertDialog) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }
        box.addView(actionButton("＋  ADD MANUALLY") { showWifiManualDialog(dialog) }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,0,0,dp(10)) })
        box.addView(actionButton("↻  REFRESH") { renderWifiPrinters(body, dialog) }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,0,0,dp(14)) })
        box.addView(printerSourceHint("WiFi printers on the same network will appear here.\nUse Add Manually when automatic discovery is unavailable."))
        val savePdf = card()
        savePdf.addView(TextView(this).apply {
            text = "🖨  Save as PDF"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        savePdf.addView(TextView(this).apply {
            text = "Use this option without selecting a physical printer."
            textSize = 12f
            setTextColor(Color.rgb(155,165,180))
            setPadding(0,dp(4),0,0)
        })
        box.addView(savePdf, LinearLayout.LayoutParams(-1, dp(78)).apply { setMargins(0,0,0,dp(8)) })
        body.addView(box)
    }

    private fun showWifiManualDialog(parent: AlertDialog) {
        val input = EditText(this).apply {
            hint = "Hostname or IP address"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(175,175,175))
        }
        AlertDialog.Builder(this)
            .setTitle("Add Manually")
            .setMessage("Hostname or IP address")
            .setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("OK") { _, _ ->
                val address = input.text.toString().trim()
                if (address.isNotEmpty()) {
                    Toast.makeText(this, "WiFi printer added: $address", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun renderBluetoothPrinters(body: FrameLayout, dialog: AlertDialog) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }
        box.addView(actionButton("↻  REFRESH") { renderBluetoothPrinters(body, dialog) }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,0,0,dp(12)) })
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
        val devices = runCatching { adapter?.bondedDevices?.toList() ?: emptyList() }.getOrDefault(emptyList())
        if (adapter == null) {
            box.addView(printerSourceHint("Bluetooth is not available on this device."))
        } else if (devices.isEmpty()) {
            box.addView(printerSourceHint("No paired Bluetooth printers found. Pair the printer in Android settings, then tap Refresh."))
        } else {
            devices.sortedBy { it.name ?: it.address }.forEach { printer ->
                val name = printer.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Printer"
                val item = actionButton("$name\n${printer.address}") {
                    Toast.makeText(this, "Selected $name", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                box.addView(item, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0,0,0,dp(8)) })
            }
        }
        body.addView(box)
    }

    private fun renderUsbPrinters(body: FrameLayout, dialog: AlertDialog) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(8))
        }
        box.addView(actionButton("↻  REFRESH") { refreshUsb(); renderUsbPrinters(body, dialog) }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,0,0,dp(12)) })
        val devices = usb.allDevices()
        if (devices.isEmpty()) {
            box.addView(printerSourceHint("No USB printer found. Connect the printer with a USB-OTG cable and tap Refresh."))
        } else {
            devices.forEach { printer ->
                val name = printer.productName?.takeIf { it.isNotBlank() } ?: printer.deviceName
                val label = if (usb.isKnownMf3010(printer)) "$name\nCanon-compatible USB device" else name
                val item = actionButton(label) {
                    device = printer
                    if (usb.hasPermission(printer)) connectDevice(printer) else usb.requestPermission(printer)
                    dialog.dismiss()
                }
                box.addView(item, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0,0,0,dp(8)) })
            }
        }
        body.addView(box)
    }

    private fun printerSourceHint(message: String): TextView = TextView(this).apply {
        text = message
        textSize = 13f
        setTextColor(Color.rgb(155,165,180))
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(24), dp(14), dp(24))
    }
'''

start = text.find('    private fun showPrinterSelector() {')
end = text.find('    private fun renderStudio()', start)
if start < 0 or end < 0:
    raise SystemExit('Could not locate Step 1 printer selector block')
text = text[:start] + selector + '\n' + text[end:]
MAIN.write_text(text, encoding='utf-8')

manifest = MANIFEST.read_text(encoding='utf-8')
perm = '    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />\n'
if 'android.permission.BLUETOOTH_CONNECT' not in manifest:
    manifest = manifest.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n', '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n' + perm, 1)
MANIFEST.write_text(manifest, encoding='utf-8')

print('Applied Step 2: three-source Select Printer screen (WiFi/Bluetooth/USB) with refresh and WiFi manual-add UI')
