from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
s = MAIN.read_text(encoding="utf-8")

# Step 4 must be safely repeatable after Step 3. Never add a second declaration
# or helper when the earlier step already supplied the same functionality.
if 'private val printerPrefs by lazy' not in s:
    anchor = '    private val prefs by lazy { getSharedPreferences("scanner_prefs", MODE_PRIVATE) }\n'
    s = s.replace(anchor, anchor + '    private val printerPrefs by lazy { getSharedPreferences("printer_prefs", MODE_PRIVATE) }\n', 1)
if 'private val settingPrefs by lazy' not in s:
    anchor = '    private val prefs by lazy { getSharedPreferences("scanner_prefs", MODE_PRIVATE) }\n'
    s = s.replace(anchor, anchor + '    private val settingPrefs by lazy { getSharedPreferences("saved_scan_settings", MODE_PRIVATE) }\n', 1)
if 'private var librarySortMode' not in s:
    anchor = '    private val prefs by lazy { getSharedPreferences("scanner_prefs", MODE_PRIVATE) }\n'
    s = s.replace(anchor, anchor + '    private var librarySortMode = "newest"\n', 1)

# Brightness and contrast are user-facing values centered at zero.
s = s.replace(
    'slider("Brightness", -100, 100, 0) { brightness = it / 100f; updatePreview() }',
    'slider("Brightness", -100, 100, settingPrefs.getInt("current_brightness", 0)) { brightness = it / 100f; settingPrefs.edit().putInt("current_brightness", it).apply(); updatePreview() }'
)
s = s.replace(
    'slider("Contrast", 50, 200, 100) { contrast = it / 100f; updatePreview() }',
    'slider("Contrast", -100, 100, settingPrefs.getInt("current_contrast", 0)) { contrast = 1f + it / 100f; settingPrefs.edit().putInt("current_contrast", it).apply(); updatePreview() }'
)
s = s.replace(
    'setSelection(1)\n        }\n        colorSwitch',
    'setSelection(settingPrefs.getInt("current_dpi_index", 1))\n        }\n        colorSwitch',
    1
)
s = s.replace(
    'isChecked = true\n            buttonTintList',
    'isChecked = settingPrefs.getBoolean("current_color", true)\n            buttonTintList',
    1
)

# Add Default / Save Setting / Saved Settings controls only when Step 3 has not already added them.
if 'actionButton("SAVE SETTING")' not in s:
    needle = '''        scanRow.addView(previewButton, weight(1f,4))
        scanRow.addView(scanButton, weight(1f,4))
        content.addView(scanRow, margin(0,10))
'''
    replacement = '''        scanRow.addView(previewButton, weight(1f,4))
        scanRow.addView(scanButton, weight(1f,4))
        content.addView(scanRow, margin(0,10))

        val settingRow = row()
        settingRow.addView(actionButton("DEFAULT") { applyDefaultScanSettings() }, weight(1f,3))
        settingRow.addView(actionButton("SAVE SETTING") { saveCurrentSettingDialog() }, weight(1f,3))
        settingRow.addView(actionButton("⋮ SAVED") { showSavedSettings() }, weight(1f,3))
        content.addView(settingRow, margin(0,8))
'''
    s = s.replace(needle, replacement, 1)

# Actual library sorting: only patch the library body when the Step 3 UI did not already provide it.
if 'var list = library.list().filter' not in s:
    s = s.replace(
        '''        val list = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }''',
        '''        var list = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }
        list = when (librarySortMode) {
            "ascending" -> list.sortedBy { it.name.lowercase() }
            "descending" -> list.sortedByDescending { it.name.lowercase() }
            "date" -> list.sortedBy { it.lastModified() }
            else -> list.sortedByDescending { it.lastModified() }
        }''',
        1
    )

# Insert a function only if that function is not already supplied by Step 3.
def insert_function(src, name, body):
    if re.search(rf'    private fun {re.escape(name)}\s*\(', src):
        return src
    marker = '    private fun renderTools() {'
    if marker not in src:
        raise SystemExit(f'Missing insertion marker for {name}')
    return src.replace(marker, body.rstrip() + '\n\n' + marker, 1)

helpers = {
    'applyDefaultScanSettings': r'''    private fun applyDefaultScanSettings() {
        brightness = 0f
        contrast = 1f
        grayscale = false
        settingPrefs.edit().putInt("current_brightness", 0).putInt("current_contrast", 0).putInt("current_dpi_index", 1).putBoolean("current_color", true).apply()
        renderTab(1)
        setStatus("Brightness and contrast restored to default")
    }''',
    'settingNames': r'''    private fun settingNames(): MutableSet<String> = settingPrefs.getStringSet("names", emptySet())?.toMutableSet() ?: mutableSetOf()''',
    'saveCurrentSettingDialog': r'''    private fun saveCurrentSettingDialog() {
        val input = EditText(this).apply { hint = "Setting name"; setSingleLine(); setTextColor(Color.WHITE); hintTextColor = Color.rgb(120,135,155) }
        AlertDialog.Builder(this).setTitle("Save Setting").setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) { setStatus("Setting name is required"); return@setPositiveButton }
                val names = settingNames(); names.add(name)
                val dpiIndex = dpiSpinner?.selectedItemPosition ?: 1
                val contrastValue = ((contrast - 1f) * 100f).roundToInt()
                settingPrefs.edit().putStringSet("names", names)
                    .putInt("${name}_brightness", (brightness * 100f).roundToInt())
                    .putInt("${name}_contrast", contrastValue)
                    .putInt("${name}_dpi", dpiIndex)
                    .putBoolean("${name}_color", colorSwitch?.isChecked ?: true).apply()
                setStatus("Setting saved • $name")
            }.setNegativeButton("CANCEL", null).show()
    }''',
    'showSavedSettings': r'''    private fun showSavedSettings() {
        val names = settingNames().sorted()
        if (names.isEmpty()) { AlertDialog.Builder(this).setTitle("Saved Settings").setMessage("No saved settings yet.").setPositiveButton("OK", null).show(); return }
        AlertDialog.Builder(this).setTitle("Saved Settings").setItems(names.toTypedArray()) { _, which -> showSavedSettingOptions(names[which]) }.setNegativeButton("CANCEL", null).show()
    }''',
    'showSavedSettingOptions': r'''    private fun showSavedSettingOptions(name: String) {
        AlertDialog.Builder(this).setTitle(name).setItems(arrayOf("Select", "Rename", "Change Settings", "Save")) { _, which ->
            when (which) {
                0 -> restoreSetting(name)
                1 -> renameSetting(name)
                2 -> editSetting(name)
                3 -> saveExistingSetting(name)
            }
        }.show()
    }''',
    'restoreSetting': r'''    private fun restoreSetting(name: String) {
        brightness = settingPrefs.getInt("${name}_brightness", 0) / 100f
        contrast = 1f + settingPrefs.getInt("${name}_contrast", 0) / 100f
        val dpi = settingPrefs.getInt("${name}_dpi", 1)
        val color = settingPrefs.getBoolean("${name}_color", true)
        settingPrefs.edit().putInt("current_brightness", (brightness * 100f).roundToInt()).putInt("current_contrast", ((contrast - 1f) * 100f).roundToInt()).putInt("current_dpi_index", dpi).putBoolean("current_color", color).apply()
        renderTab(1)
        setStatus("Setting restored • $name")
    }''',
    'renameSetting': r'''    private fun renameSetting(old: String) {
        val input = EditText(this).apply { setText(old); setSingleLine(); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Rename Setting").setView(input).setPositiveButton("SAVE") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isBlank()) return@setPositiveButton
            val names = settingNames(); names.remove(old); names.add(newName)
            settingPrefs.edit().putStringSet("names", names)
                .putInt("${newName}_brightness", settingPrefs.getInt("${old}_brightness", 0))
                .putInt("${newName}_contrast", settingPrefs.getInt("${old}_contrast", 0))
                .putInt("${newName}_dpi", settingPrefs.getInt("${old}_dpi", 1))
                .putBoolean("${newName}_color", settingPrefs.getBoolean("${old}_color", true))
                .remove("${old}_brightness").remove("${old}_contrast").remove("${old}_dpi").remove("${old}_color").apply()
            setStatus("Setting renamed • $newName")
        }.setNegativeButton("CANCEL", null).show()
    }''',
    'editSetting': r'''    private fun editSetting(name: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8),dp(4),dp(8),0) }
        val b = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED; hint = "Brightness (-100 to 100)"; setText(settingPrefs.getInt("${name}_brightness", 0).toString()); setTextColor(Color.WHITE) }
        val c = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED; hint = "Contrast (-100 to 100)"; setText(settingPrefs.getInt("${name}_contrast", 0).toString()); setTextColor(Color.WHITE) }
        val color = CheckBox(this).apply { text = "Color"; setTextColor(Color.WHITE); isChecked = settingPrefs.getBoolean("${name}_color", true) }
        val dpi = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, listOf("150 DPI","300 DPI","600 DPI")); setSelection(settingPrefs.getInt("${name}_dpi",1)) }
        box.addView(b); box.addView(c); box.addView(color); box.addView(dpi)
        AlertDialog.Builder(this).setTitle("Change Settings • $name").setView(box).setPositiveButton("SAVE") { _, _ ->
            val bv = (b.text.toString().toIntOrNull() ?: 0).coerceIn(-100,100)
            val cv = (c.text.toString().toIntOrNull() ?: 0).coerceIn(-100,100)
            settingPrefs.edit().putInt("${name}_brightness", bv).putInt("${name}_contrast", cv).putInt("${name}_dpi", dpi.selectedItemPosition).putBoolean("${name}_color", color.isChecked).apply()
            restoreSetting(name)
        }.setNegativeButton("CANCEL", null).show()
    }''',
    'saveExistingSetting': r'''    private fun saveExistingSetting(name: String) {
        settingPrefs.edit().putInt("${name}_brightness", (brightness * 100f).roundToInt()).putInt("${name}_contrast", ((contrast - 1f) * 100f).roundToInt()).putInt("${name}_dpi", dpiSpinner?.selectedItemPosition ?: 1).putBoolean("${name}_color", colorSwitch?.isChecked ?: true).apply()
        setStatus("Setting updated • $name")
    }''',
}
for name, body in helpers.items():
    s = insert_function(s, name, body)

# Make the actual scanner result and preview honor current brightness/contrast.
if 'val adjusted = if (brightness != 0f || contrast != 1f) library.applyAdjustments(raw, brightness, contrast)' not in s:
    old_scan = '''                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                library.savePage(result.bitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(result.bitmap)
                    dpis.add(result.dpi)
                    currentPage = pages.lastIndex
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = null
                    brightness = 0f
                    contrast = 1f
                    grayscale = false
                    setStatus("Scan complete • ${result.width} × ${result.height} • ${result.dpi} DPI")
                    finishScan()
                    renderTab(1)
                }'''
    new_scan = '''                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                val raw = result.bitmap
                val adjusted = if (brightness != 0f || contrast != 1f) library.applyAdjustments(raw, brightness, contrast) else raw
                if (adjusted !== raw && !raw.isRecycled) raw.recycle()
                library.savePage(adjusted, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(adjusted)
                    dpis.add(result.dpi)
                    currentPage = pages.lastIndex
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = null
                    grayscale = false
                    setStatus("Scan complete • ${result.width} × ${result.height} • ${result.dpi} DPI • adjustments applied")
                    finishScan()
                    renderTab(1)
                }'''
    s = s.replace(old_scan, new_scan, 1)

if 'Fast preview ready • ${result.width} × ${result.height} • 150 DPI • current adjustments applied' not in s:
    old_preview = '''                runOnUiThread {
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = result.bitmap
                    brightness = 0f
                    contrast = 1f
                    grayscale = false
                    currentPage = -1
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    previewing = false
                    setScannerButtonsEnabled(true)
                    imagePreview?.setImageBitmap(previewBitmap)
                    setStatus("Fast preview ready • ${result.width} × ${result.height} • 150 DPI. Adjust settings, then tap SCAN PAGE.")
                }'''
    new_preview = '''                val raw = result.bitmap
                val adjusted = if (brightness != 0f || contrast != 1f) library.applyAdjustments(raw, brightness, contrast) else raw
                if (adjusted !== raw && !raw.isRecycled) raw.recycle()
                runOnUiThread {
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = adjusted
                    grayscale = false
                    currentPage = -1
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    previewing = false
                    setScannerButtonsEnabled(true)
                    imagePreview?.setImageBitmap(previewBitmap)
                    setStatus("Fast preview ready • ${result.width} × ${result.height} • 150 DPI • current adjustments applied")
                }'''
    s = s.replace(old_preview, new_preview, 1)

MAIN.write_text(s, encoding="utf-8")
print("Step 4 settings, actual scan adjustments, and library behavior applied")
