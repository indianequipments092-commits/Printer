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
            if depth == 0:
                return start, i + 1
    raise SystemExit(f'Unbalanced method: {name}')

def replace_method(src, name, body):
    a, b = method_bounds(src, name)
    return src[:a] + body.rstrip() + '\n' + src[b:]

home = r'''    private fun renderHome() {
        content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"))
        val deviceCard = card()
        deviceCard.addView(TextView(this).apply {
            text = "PRINTER STATUS"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145,160,180))
        })
        val connected = device != null && protocol != null
        status = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            text = if (connected) "Printer Connected" else "Printer Not Found"
            setTextColor(if (connected) Color.rgb(70,215,145) else Color.rgb(255,190,70))
            setPadding(0,dp(5),0,0)
        }
        deviceCard.addView(status)
        val model = device?.productName?.takeIf { it.isNotBlank() }
            ?: device?.deviceName?.takeIf { it.isNotBlank() }
            ?: if (connected) "Printer Model detected" else "Printer Model — waiting for printer"
        deviceCard.addView(TextView(this).apply {
            text = "PRINTER MODEL  •  $model"
            textSize = 12f
            setTextColor(Color.rgb(120,136,158))
            setPadding(0,dp(4),0,0)
        })
        val printerActions = row()
        printerActions.addView(actionButton("SELECT PRINTER") { showPrinterSelector() }, weight(1f,4))
        printerActions.addView(actionButton("↻  REFRESH") {
            refreshUsb()
            setStatus("Checking for printer…")
            renderTab(0)
        }, weight(1f,4))
        deviceCard.addView(printerActions, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,dp(14),0,0) })
        content.addView(deviceCard, margin(0,10))

        val hero = card()
        hero.addView(TextView(this).apply {
            text = "SCAN STUDIO"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(130,160,200))
        })
        hero.addView(TextView(this).apply {
            text = "Turn paper into a polished digital document"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0,dp(6),0,dp(2))
        })
        hero.addView(TextView(this).apply {
            text = "Multi-page • Preview • Enhance • Edit • Export • Share"
            textSize = 12f
            setTextColor(Color.rgb(150,165,185))
        })
        hero.addView(actionButton("＋  NEW SCAN") { renderTab(1) }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0,dp(18),0,0) })
        content.addView(hero, margin(0,12))

        val stats = row()
        stats.addView(statCard("${library.list().size}", "ALL SCANS"), weight(1f,5))
        stats.addView(statCard("${pages.size}", "CURRENT PAGES"), weight(1f,5))
        stats.addView(statCard("${selectedLibrary.size}", "SELECTED"), weight(1f,5))
        content.addView(stats, margin(0,12))
        content.addView(section("QUICK ACTIONS"))
        val quick = row()
        quick.addView(tile("▦", "All Scans") { renderTab(2) }, weight(1f,6))
        quick.addView(tile("✦", "Enhance") { renderTab(1) }, weight(1f,6))
        quick.addView(tile("↗", "Export") { showShareFormatDialog() }, weight(1f,6))
        content.addView(quick, margin(0,12))
        content.addView(section("RECENT DOCUMENTS"))
        val recent = library.list().take(3)
        if (recent.isEmpty()) content.addView(emptyCard("No scans yet", "Your scanned pages will appear here."), margin(0,8))
        recent.forEach { file -> content.addView(fileRow(file), margin(0,8)) }
    }'''

library = r'''    private fun renderLibrary() {
        content.addView(title("ALL SCANS", "YOUR LOCAL DOCUMENT LIBRARY"))
        val search = EditText(this).apply {
            hint = "Search scans…"
            hintTextColor = Color.rgb(110,125,145)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine()
            background = rounded(Color.rgb(18,25,36),18)
            setPadding(dp(16),0,dp(16),0)
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0,0,0,dp(10)) })
        val controls = row()
        controls.addView(tile("☑", "Select all") {
            selectedLibrary.clear()
            library.list().forEach { selectedLibrary.add(it.absolutePath) }
            renderTab(2)
        }, weight(1f,6))
        controls.addView(tile("↕", "Newest") {
            librarySortMode = "newest"
            renderTab(2)
        }, weight(1f,6))
        controls.addView(tile("⇅", "Sort") { showSortDialog() }, weight(1f,6))
        controls.addView(tile("↗", "Share") { showLibraryShareFormatDialog() }, weight(1f,6))
        controls.addView(tile("🏷", "Label") { showLabelDialogForSelection() }, weight(1f,6))
        controls.addView(tile("▣", "Label File") { showLabelFileDialog() }, weight(1f,6))
        content.addView(controls, margin(0,8))

        val base = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }
        val list = when (librarySortMode) {
            "ascending" -> base.sortedBy { it.name.lowercase() }
            "descending" -> base.sortedByDescending { it.name.lowercase() }
            "date" -> base.sortedBy { it.lastModified() }
            else -> base.sortedByDescending { it.lastModified() }
        }
        if (list.isEmpty()) content.addView(emptyCard("Library is empty", "Scan a page and it will be saved here automatically."))
        list.forEach { file ->
            val selected = selectedLibrary.contains(file.absolutePath)
            val row = fileRow(file, true)
            row.setOnClickListener {
                if (selectedLibrary.contains(file.absolutePath)) selectedLibrary.remove(file.absolutePath) else selectedLibrary.add(file.absolutePath)
                renderTab(2)
            }
            content.addView(row, margin(0,8))
        }
        if (selectedLibrary.isNotEmpty()) {
            content.addView(actionButton("SHARE ${selectedLibrary.size} SELECTED") { showLibraryShareFormatDialog() }, margin(0,6))
            content.addView(actionButton("LABEL ${selectedLibrary.size} SELECTED") { showLabelDialogForSelection() }, margin(0,6))
        }
    }'''

text = replace_method(text, 'renderHome', home)
text = replace_method(text, 'renderLibrary', library)

fields = '''    private var librarySortMode = "newest"
    private val labelAssignments = mutableMapOf<String, String>()
    private val labelNames = linkedSetOf<String>()
'''
anchor = '    private val prefs by lazy { getSharedPreferences("scanner_prefs", MODE_PRIVATE) }\n'
if 'private var librarySortMode' not in text:
    text = text.replace(anchor, anchor + fields, 1)

helpers = r'''
    private fun showSortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sort")
            .setItems(arrayOf("Ascending", "Descending", "Newest", "Date wise")) { _, which ->
                librarySortMode = when (which) { 0 -> "ascending"; 1 -> "descending"; 2 -> "newest"; else -> "date" }
                renderTab(2)
            }.show()
    }

    private fun showLabelDialogForSelection() {
        if (selectedLibrary.isEmpty()) {
            Toast.makeText(this, "Select at least one scan first", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply { hint = "Label name"; setSingleLine(true); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Save Label").setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                labelNames.add(name)
                selectedLibrary.forEach { labelAssignments[it] = name }
                prefs.edit().putStringSet("label_names", labelNames).putString("labels", labelAssignments.entries.joinToString("\n") { "${it.key}|${it.value}" }).apply()
                Toast.makeText(this, "Saved label: $name", Toast.LENGTH_SHORT).show()
                renderTab(2)
            }.show()
    }

    private fun showLabelFileDialog() {
        val saved = prefs.getStringSet("label_names", emptySet())?.toList()?.sorted() ?: labelNames.toList().sorted()
        if (saved.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Label File").setMessage("No labels saved yet.").setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Label File").setItems(saved.toTypedArray()) { _, which -> showLabelItems(saved[which]) }.show()
    }

    private fun showLabelItems(label: String) {
        val items = labelAssignments.filterValues { it == label }.keys.mapNotNull { path -> File(path).takeIf { it.exists() } }
        val names = if (items.isEmpty()) "No scans in this label." else items.joinToString("\n") { it.name }
        AlertDialog.Builder(this).setTitle(label).setMessage(names)
            .setNeutralButton("RENAME") { _, _ -> renameLabel(label) }
            .setPositiveButton("CLOSE", null).show()
    }

    private fun renameLabel(oldName: String) {
        val input = EditText(this).apply { setSingleLine(true); setText(oldName); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Rename Label").setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty() || newName == oldName) return@setPositiveButton
                labelNames.remove(oldName); labelNames.add(newName)
                labelAssignments.entries.filter { it.value == oldName }.forEach { labelAssignments[it.key] = newName }
                prefs.edit().putStringSet("label_names", labelNames).putString("labels", labelAssignments.entries.joinToString("\n") { "${it.key}|${it.value}" }).apply()
                renderTab(2)
            }.show()
    }
'''
if 'private fun showSortDialog()' not in text:
    anchor2 = '    private fun renderTools() {'
    text = text.replace(anchor2, helpers + '\n' + anchor2, 1)

MAIN.write_text(text, encoding='utf-8')
print('Step 3 UI chunk applied: printer home status/actions, Library Sort/Label/Label File and persistent label names')
