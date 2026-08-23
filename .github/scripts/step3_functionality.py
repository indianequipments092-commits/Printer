from pathlib import Path

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
text = MAIN.read_text(encoding='utf-8')

def method_bounds(src, name):
    marker = f'    private fun {name}('
    start = src.find(marker)
    if start < 0:
        raise SystemExit(f'Missing method: {name}')
    brace = src.find('{', start)
    depth = 0
    for i in range(brace, len(src)):
        if src[i] == '{': depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0: return start, i + 1
    raise SystemExit(f'Unbalanced method: {name}')

def replace_method(src, name, body):
    a,b = method_bounds(src,name)
    return src[:a] + body.rstrip() + '\n' + src[b:]

# Persistent print/scan setting model.
fields = '''    private var selectedPrinter: UsbDevice? = null
    private val savedScanSettings = linkedMapOf<String, String>()
    private var activeSettingName: String? = null
'''
anchor = '    private var pendingTempPdf: File? = null\n'
if 'private var selectedPrinter: UsbDevice?' not in text:
    text = text.replace(anchor, anchor + fields, 1)

# Load saved setting names and label state when the activity starts.
needle = '        refreshUsb()\n'
if 'loadSavedSettings()' not in text:
    text = text.replace(needle, needle + '        loadSavedSettings()\n', 1)

# Replace USB printer selection so it does not incorrectly open a printer as a scanner.
usb_method = r'''    private fun renderUsbPrinters(body: FrameLayout, dialog: AlertDialog) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(12), dp(8), dp(8)) }
        box.addView(actionButton("↻  REFRESH") { refreshUsb(); renderUsbPrinters(body, dialog) }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,0,0,dp(12)) })
        val devices = usb.allDevices()
        if (devices.isEmpty()) {
            box.addView(printerSourceHint("No USB printer found. Connect the printer with a USB-OTG cable and tap Refresh."))
        } else {
            devices.forEach { printer ->
                val name = printer.productName?.takeIf { it.isNotBlank() } ?: printer.deviceName
                box.addView(actionButton(name) {
                    selectedPrinter = printer
                    if (usb.hasPermission(printer)) {
                        Toast.makeText(this, "Printer selected: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        usb.requestPermission(printer)
                        Toast.makeText(this, "USB permission requested for $name", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                    renderTab(0)
                }, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0,0,0,dp(8)) })
            }
        }
        body.addView(box)
    }'''
if 'private fun renderUsbPrinters(' in text:
    text = replace_method(text, 'renderUsbPrinters', usb_method)

# Make the printer status use a selected/visible USB printer instead of the scanner protocol state.
home_start = text.find('    private fun renderHome() {')
if home_start >= 0:
    # Replace only the connected/model declarations inside the generated Home renderer.
    text = text.replace('        val connected = device != null && protocol != null\n', '        val detectedPrinter = selectedPrinter ?: usb.allDevices().firstOrNull()\n        val connected = detectedPrinter != null\n', 1)
    text = text.replace('        val model = device?.productName?.takeIf { it.isNotBlank() }\n            ?: device?.deviceName?.takeIf { it.isNotBlank() }\n', '        val model = detectedPrinter?.productName?.takeIf { it.isNotBlank() }\n            ?: detectedPrinter?.deviceName?.takeIf { it.isNotBlank() }\n', 1)

# Scan applies brightness/contrast/grayscale before the saved bitmap is written.
scan_body = r'''    private fun scan() {
        val scanner = protocol ?: run { requestScanner(); return }
        if (scanning || previewing) return
        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)
        val selectedColor = colorSwitch?.isChecked ?: true
        val captureBrightness = brightness
        val captureContrast = contrast
        val captureGray = grayscale
        scanning = true
        setScannerButtonsEnabled(false)
        progress?.visibility = View.VISIBLE
        progressText?.visibility = View.VISIBLE
        progress?.progress = 0
        progressText?.text = "Preparing scan…"
        executor.execute {
            try {
                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = selectedColor)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                var finalBitmap = result.bitmap
                if (captureBrightness != 0f || captureContrast != 1f) {
                    val adjusted = library.applyAdjustments(finalBitmap, captureBrightness, captureContrast)
                    if (!finalBitmap.isRecycled) finalBitmap.recycle()
                    finalBitmap = adjusted
                }
                if (captureGray) {
                    val gray = document.grayscale(finalBitmap)
                    if (!finalBitmap.isRecycled) finalBitmap.recycle()
                    finalBitmap = gray
                }
                library.savePage(finalBitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(finalBitmap)
                    dpis.add(result.dpi)
                    currentPage = pages.lastIndex
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = null
                    setStatus("Scan complete • ${result.width} × ${result.height} • ${result.dpi} DPI")
                    finishScan()
                    renderTab(1)
                }
            } catch (t: Throwable) {
                runOnUiThread { setStatus("Scan failed • ${t.message ?: "Unknown scanner error"}"); finishScan() }
            }
        }
    }'''
text = replace_method(text, 'scan', scan_body)

preview_body = r'''    private fun previewScan() {
        val scanner = protocol ?: run { requestScanner(); return }
        if (scanning || previewing) return
        val selectedColor = colorSwitch?.isChecked ?: true
        val captureBrightness = brightness
        val captureContrast = contrast
        val captureGray = grayscale
        previewing = true
        setScannerButtonsEnabled(false)
        progress?.visibility = View.VISIBLE
        progressText?.visibility = View.VISIBLE
        progress?.progress = 0
        progressText?.text = "Fast preview • 150 DPI…"
        executor.execute {
            try {
                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = 150, color = selectedColor)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                var shown = result.bitmap
                if (captureBrightness != 0f || captureContrast != 1f) {
                    val adjusted = library.applyAdjustments(shown, captureBrightness, captureContrast)
                    if (!shown.isRecycled) shown.recycle()
                    shown = adjusted
                }
                if (captureGray) {
                    val gray = document.grayscale(shown)
                    if (!shown.isRecycled) shown.recycle()
                    shown = gray
                }
                runOnUiThread {
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = shown
                    currentPage = -1
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    previewing = false
                    setScannerButtonsEnabled(true)
                    imagePreview?.setImageBitmap(previewBitmap)
                    setStatus("Preview ready • settings applied")
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    previewing = false
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    setScannerButtonsEnabled(true)
                    setStatus("Preview failed • ${t.message ?: "Unknown scanner error"}")
                }
            }
        }
    }'''
text = replace_method(text, 'previewScan', preview_body)

# Add setting controls after the existing Preview/Scan row.
needle = '        content.addView(scanRow, margin(0,10))\n'
controls = '''        val settingRow = row()
        settingRow.addView(actionButton("DEFAULT") { resetScanSettingsUi() }, weight(1f,3))
        settingRow.addView(actionButton("SAVE SETTING") { saveCurrentSettingDialog() }, weight(1f,3))
        settingRow.addView(actionButton("⋮") { showSavedSettingsDialog() }, weight(1f,3))
        content.addView(settingRow, margin(0,6))
'''
if 'actionButton("SAVE SETTING")' not in text:
    text = text.replace(needle, needle + controls, 1)

helpers = r'''
    private fun settingPayload(): String {
        val dpi = dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300
        val color = colorSwitch?.isChecked ?: true
        return "${brightness}|${contrast}|${dpi}|${color}"
    }

    private fun applySettingPayload(payload: String) {
        val p = payload.split('|')
        brightness = p.getOrNull(0)?.toFloatOrNull() ?: 0f
        contrast = p.getOrNull(1)?.toFloatOrNull() ?: 1f
        val dpi = p.getOrNull(2)?.toIntOrNull() ?: 300
        val color = p.getOrNull(3)?.toBooleanStrictOrNull() ?: true
        dpiSpinner?.setSelection(listOf(150,300,600).indexOf(dpi).coerceAtLeast(0))
        colorSwitch?.isChecked = color
        updatePreview()
    }

    private fun loadSavedSettings() {
        savedScanSettings.clear()
        val raw = prefs.getString("saved_scan_settings", "") ?: ""
        raw.split("\n").forEach { line ->
            val i = line.indexOf('=')
            if (i > 0) savedScanSettings[line.substring(0,i)] = line.substring(i + 1)
        }
    }

    private fun persistSavedSettings() {
        prefs.edit().putString("saved_scan_settings", savedScanSettings.entries.joinToString("\n") { "${it.key}=${it.value}" }).apply()
    }

    private fun resetScanSettingsUi() {
        brightness = 0f
        contrast = 1f
        grayscale = false
        dpiSpinner?.setSelection(1)
        colorSwitch?.isChecked = true
        activeSettingName = null
        updatePreview()
        setStatus("Default settings restored")
    }

    private fun saveCurrentSettingDialog() {
        val input = EditText(this).apply { hint = "Setting name"; setSingleLine(true); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Save Setting").setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                savedScanSettings[name] = settingPayload()
                activeSettingName = name
                persistSavedSettings()
                Toast.makeText(this, "Saved setting: $name", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showSavedSettingsDialog() {
        loadSavedSettings()
        if (savedScanSettings.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Saved Settings").setMessage("No saved settings yet.").setPositiveButton("OK", null).show()
            return
        }
        val names = savedScanSettings.keys.toTypedArray()
        AlertDialog.Builder(this).setTitle("Saved Settings").setItems(names) { _, which -> showSettingActions(names[which]) }.show()
    }

    private fun showSettingActions(name: String) {
        AlertDialog.Builder(this).setTitle(name)
            .setItems(arrayOf("Select", "Rename", "Change Settings", "Save")) { _, which ->
                when (which) {
                    0 -> { applySettingPayload(savedScanSettings[name] ?: return@setItems); activeSettingName = name }
                    1 -> renameSavedSetting(name)
                    2 -> { applySettingPayload(savedScanSettings[name] ?: return@setItems); activeSettingName = name; renderTab(1) }
                    3 -> { savedScanSettings[name] = settingPayload(); persistSavedSettings() }
                }
            }.show()
    }

    private fun renameSavedSetting(oldName: String) {
        val input = EditText(this).apply { setSingleLine(true); setText(oldName); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Rename Setting").setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty() || newName == oldName) return@setPositiveButton
                val payload = savedScanSettings.remove(oldName) ?: return@setPositiveButton
                savedScanSettings[newName] = payload
                if (activeSettingName == oldName) activeSettingName = newName
                persistSavedSettings()
            }.show()
    }
'''
if 'private fun settingPayload()' not in text:
    anchor2 = '    private fun showPdfStudio() {'
    text = text.replace(anchor2, helpers + '\n' + anchor2, 1)

MAIN.write_text(text, encoding='utf-8')
print('Step 3 functionality chunk applied')
