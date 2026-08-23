from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt"
s = MAIN.read_text(encoding="utf-8")

# Persistent setting state
s = s.replace('    private val printerPrefs by lazy { getSharedPreferences("printer_prefs", MODE_PRIVATE) }\n', '''    private val printerPrefs by lazy { getSharedPreferences("printer_prefs", MODE_PRIVATE) }\n    private val settingPrefs by lazy { getSharedPreferences("saved_scan_settings", MODE_PRIVATE) }\n    private var librarySortMode = "newest"\n''')

# Make brightness/contrast sliders restore current values from persistent state.
s = s.replace('slider("Brightness", -100, 100, 0) { brightness = it / 100f; updatePreview() }', 'slider("Brightness", -100, 100, settingPrefs.getInt("current_brightness", 0)) { brightness = it / 100f; settingPrefs.edit().putInt("current_brightness", it).apply(); updatePreview() }')
s = s.replace('slider("Contrast", 50, 200, 100) { contrast = it / 100f; updatePreview() }', 'slider("Contrast", 50, 200, settingPrefs.getInt("current_contrast", 100)) { contrast = it / 100f; settingPrefs.edit().putInt("current_contrast", it).apply(); updatePreview() }')
s = s.replace('setSelection(1)\n        }\n        colorSwitch', 'setSelection(settingPrefs.getInt("current_dpi_index", 1))\n        }\n        colorSwitch', 1)
s = s.replace('isChecked = true\n            buttonTintList', 'isChecked = settingPrefs.getBoolean("current_color", true)\n            buttonTintList', 1)

# Add Default/Save Setting/Saved Settings controls immediately after Preview/Scan row.
needle = '''        scanRow.addView(previewButton, weight(1f,4))\n        scanRow.addView(scanButton, weight(1f,4))\n        content.addView(scanRow, margin(0,10))\n'''
replacement = '''        scanRow.addView(previewButton, weight(1f,4))\n        scanRow.addView(scanButton, weight(1f,4))\n        content.addView(scanRow, margin(0,10))\n\n        val settingRow = row()\n        settingRow.addView(actionButton("DEFAULT") { applyDefaultScanSettings() }, weight(1f,3))\n        settingRow.addView(actionButton("SAVE SETTING") { saveCurrentSettingDialog() }, weight(1f,3))\n        settingRow.addView(actionButton("⋮ SAVED") { showSavedSettings() }, weight(1f,3))\n        content.addView(settingRow, margin(0,8))\n'''
s = s.replace(needle, replacement, 1)

# Replace the no-op sort implementation with actual local ordering.
s = s.replace('''    private fun renderLibrarySorted(mode: String) {\n        // Keep the UI controls identical while applying deterministic local ordering.\n        renderTab(2)\n        setStatus("Sort: ${mode.replaceFirstChar { it.uppercase() }}")\n    }''', '''    private fun renderLibrarySorted(mode: String) {\n        librarySortMode = mode\n        renderTab(2)\n    }''')

# Make renderLibrary use the selected sort mode.
s = s.replace('''        val list = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }''', '''        var list = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }\n        list = when (librarySortMode) {\n            "ascending" -> list.sortedBy { it.name.lowercase() }\n            "descending" -> list.sortedByDescending { it.name.lowercase() }\n            "date" -> list.sortedBy { it.lastModified() }\n            else -> list.sortedByDescending { it.lastModified() }\n        }''', 1)

# Insert persistent scan-setting helpers before renderTools.
marker = '    private fun renderTools() {'
helpers = r'''    private fun applyDefaultScanSettings() {
        brightness = 0f
        contrast = 1f
        grayscale = false
        settingPrefs.edit().putInt("current_brightness", 0).putInt("current_contrast", 100).putInt("current_dpi_index", 1).putBoolean("current_color", true).apply()
        renderTab(1)
        setStatus("Brightness and contrast restored to default")
    }

    private fun settingNames(): MutableSet<String> = settingPrefs.getStringSet("names", emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun saveCurrentSettingDialog() {
        val input = EditText(this).apply { hint = "Setting name"; setSingleLine(); setTextColor(Color.WHITE); hintTextColor = Color.rgb(120,135,155) }
        AlertDialog.Builder(this).setTitle("Save Setting").setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) { setStatus("Setting name is required"); return@setPositiveButton }
                val names = settingNames(); names.add(name)
                val dpiIndex = dpiSpinner?.selectedItemPosition ?: 1
                settingPrefs.edit().putStringSet("names", names)
                    .putInt("${name}_brightness", (brightness * 100f).roundToInt())
                    .putInt("${name}_contrast", (contrast * 100f).roundToInt())
                    .putInt("${name}_dpi", dpiIndex)
                    .putBoolean("${name}_color", colorSwitch?.isChecked ?: true).apply()
                setStatus("Setting saved • $name")
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun showSavedSettings() {
        val names = settingNames().sorted()
        if (names.isEmpty()) { AlertDialog.Builder(this).setTitle("Saved Settings").setMessage("No saved settings yet.").setPositiveButton("OK", null).show(); return }
        AlertDialog.Builder(this).setTitle("Saved Settings").setItems(names.toTypedArray()) { _, which -> showSavedSettingOptions(names[which]) }.setNegativeButton("CANCEL", null).show()
    }

    private fun showSavedSettingOptions(name: String) {
        AlertDialog.Builder(this).setTitle(name).setItems(arrayOf("Select", "Rename", "Change Settings", "Save")) { _, which ->
            when (which) {
                0 -> restoreSetting(name)
                1 -> renameSetting(name)
                2 -> editSetting(name)
                3 -> saveExistingSetting(name)
            }
        }.show()
    }

    private fun restoreSetting(name: String) {
        brightness = settingPrefs.getInt("${name}_brightness", 0) / 100f
        contrast = settingPrefs.getInt("${name}_contrast", 100) / 100f
        val dpi = settingPrefs.getInt("${name}_dpi", 1)
        val color = settingPrefs.getBoolean("${name}_color", true)
        settingPrefs.edit().putInt("current_brightness", (brightness * 100f).roundToInt()).putInt("current_contrast", (contrast * 100f).roundToInt()).putInt("current_dpi_index", dpi).putBoolean("current_color", color).apply()
        renderTab(1)
        setStatus("Setting restored • $name")
    }

    private fun renameSetting(old: String) {
        val input = EditText(this).apply { setText(old); setSingleLine(); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Rename Setting").setView(input).setPositiveButton("SAVE") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isBlank()) return@setPositiveButton
            val names = settingNames(); names.remove(old); names.add(newName)
            val e = settingPrefs.edit().putStringSet("names", names)
            e.putInt("${newName}_brightness", settingPrefs.getInt("${old}_brightness", 0))
                .putInt("${newName}_contrast", settingPrefs.getInt("${old}_contrast", 100))
                .putInt("${newName}_dpi", settingPrefs.getInt("${old}_dpi", 1))
                .putBoolean("${newName}_color", settingPrefs.getBoolean("${old}_color", true)).remove("${old}_brightness").remove("${old}_contrast").remove("${old}_dpi").remove("${old}_color").apply()
            setStatus("Setting renamed • $newName")
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun editSetting(name: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8),dp(4),dp(8),0) }
        val b = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED; hint = "Brightness (-100 to 100)"; setText(settingPrefs.getInt("${name}_brightness", 0).toString()); setTextColor(Color.WHITE) }
        val c = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; hint = "Contrast (50 to 200)"; setText(settingPrefs.getInt("${name}_contrast", 100).toString()); setTextColor(Color.WHITE) }
        val color = CheckBox(this).apply { text = "Color"; setTextColor(Color.WHITE); isChecked = settingPrefs.getBoolean("${name}_color", true) }
        val dpi = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, listOf("150 DPI","300 DPI","600 DPI")); setSelection(settingPrefs.getInt("${name}_dpi",1)) }
        box.addView(b); box.addView(c); box.addView(color); box.addView(dpi)
        AlertDialog.Builder(this).setTitle("Change Settings • $name").setView(box).setPositiveButton("SAVE") { _, _ ->
            val bv = (b.text.toString().toIntOrNull() ?: 0).coerceIn(-100,100)
            val cv = (c.text.toString().toIntOrNull() ?: 100).coerceIn(50,200)
            settingPrefs.edit().putInt("${name}_brightness", bv).putInt("${name}_contrast", cv).putInt("${name}_dpi", dpi.selectedItemPosition).putBoolean("${name}_color", color.isChecked).apply()
            restoreSetting(name)
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun saveExistingSetting(name: String) {
        settingPrefs.edit().putInt("${name}_brightness", (brightness * 100f).roundToInt()).putInt("${name}_contrast", (contrast * 100f).roundToInt()).putInt("${name}_dpi", dpiSpinner?.selectedItemPosition ?: 1).putBoolean("${name}_color", colorSwitch?.isChecked ?: true).apply()
        setStatus("Setting updated • $name")
    }

'''
s = s.replace(marker, helpers + marker, 1)

MAIN.write_text(s, encoding="utf-8")
print("Step 4 settings and library behavior applied")
'''
