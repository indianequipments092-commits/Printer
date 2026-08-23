from pathlib import Path

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
s = MAIN.read_text(encoding='utf-8')

# Fresh integrated Printex tab. Idempotent: never adds the tab twice.
if 'private fun renderPrintex()' not in s:
    old_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)'''
    new_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("▣", "Printex", 3)\n        navButton("⚙", "Tools", 4)'''
    if old_nav not in s:
        raise SystemExit('MainActivity navigation anchor not found')
    s = s.replace(old_nav, new_nav, 1)

    old_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            else -> renderTools()\n        }'''
    new_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            3 -> renderPrintex()\n            else -> renderTools()\n        }'''
    if old_when not in s:
        raise SystemExit('MainActivity tab switch anchor not found')
    s = s.replace(old_when, new_when, 1)

    marker = '    private fun renderTools() {'
    if marker not in s:
        raise SystemExit('renderTools anchor not found')

    method = r'''    private fun renderPrintex() {
        content.addView(title("PRINTEX", "PROFESSIONAL PRINT STUDIO • DOCUMENTS + PHOTOS + PDF"))

        val hero = card()
        hero.addView(TextView(this).apply {
            text = "PRINT ANYTHING • YOUR WAY"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(95,155,255))
        })
        hero.addView(TextView(this).apply {
            text = "Fast, clean and precise printing"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(2))
        })
        hero.addView(TextView(this).apply {
            text = "PDF • Image • Document • Multi-page • Duplex • Preview"
            textSize = 12f
            setTextColor(Color.rgb(145,160,180))
        })
        content.addView(hero, margin(0, 10))

        val source = row()
        source.addView(tile("▤", "PDF / File") { printexChooseFile() }, weight(1f, 4))
        source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f, 4))
        source.addView(tile("▦", "Preview") { printexPreview() }, weight(1f, 4))
        source.addView(tile("⚙", "Settings") { printexPrintSettings() }, weight(1f, 4))
        content.addView(source, margin(0, 10))

        val preview = card()
        preview.addView(TextView(this).apply {
            text = "DOCUMENT PREVIEW"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(145,160,180))
        })
        val paper = TextView(this).apply {
            text = "\n\n\n        No document selected\n\n   Choose a PDF, image or document\n   to preview it before printing\n\n"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.rgb(135,150,170))
            background = rounded(Color.rgb(18,25,36), 14)
            setPadding(dp(12), dp(18), dp(12), dp(18))
        }
        preview.addView(paper, LinearLayout.LayoutParams(-1, dp(250)).apply { setMargins(0, dp(10), 0, 0) })
        content.addView(preview, margin(0, 10))

        content.addView(section("PRINT SETTINGS"), margin(0, 8))
        val settings = card()
        printexSettingRow(settings, "Copies", "1") { printexPrintSettings() }
        printexSettingRow(settings, "Pages", "All pages") { printexPrintSettings() }
        printexSettingRow(settings, "Paper Size", "A4") { printexPrintSettings() }
        printexSettingRow(settings, "Orientation", "Auto") { printexPrintSettings() }
        printexSettingRow(settings, "Scaling", "Fit to page") { printexPrintSettings() }
        printexSettingRow(settings, "Duplex", "One side") { printexPrintSettings() }
        printexSettingRow(settings, "Layout", "1 page per sheet") { printexPrintSettings() }
        content.addView(settings, margin(0, 8))

        content.addView(section("IMAGE SETTINGS"), margin(0, 8))
        val image = card()
        printexImageRow(image, "Brightness", "0")
        printexImageRow(image, "Contrast", "0")
        printexImageRow(image, "Rotation", "0°")
        printexImageRow(image, "Color Mode", "Color")
        content.addView(image, margin(0, 8))

        val actions = row()
        actions.addView(actionButton("PRINT") { printexPrintNow() }, weight(1f, 5))
        actions.addView(actionButton("SHARE") { showShareFormatDialog() }, weight(1f, 5))
        content.addView(actions, margin(0, 12))
    }

    private fun printexSettingRow(parent: LinearLayout, name: String, value: String, click: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { click() }
        }
        row.addView(TextView(this).apply {
            text = name
            textSize = 15f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(50), 1f))
        row.addView(TextView(this).apply {
            text = value + "  ›"
            textSize = 14f
            setTextColor(Color.rgb(125,165,230))
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }, LinearLayout.LayoutParams(dp(150), dp(50)))
        parent.addView(row)
    }

    private fun printexImageRow(parent: LinearLayout, name: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        row.addView(TextView(this).apply {
            text = name
            textSize = 15f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        val minus = Button(this).apply {
            text = "−"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(24,32,45), 12)
            setOnClickListener { showAdjustValue(name, -100, 100, 0) }
        }
        val valueText = TextView(this).apply {
            text = value
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        val plus = Button(this).apply {
            text = "+"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(24,32,45), 12)
            setOnClickListener { showAdjustValue(name, -100, 100, 0) }
        }
        row.addView(minus, LinearLayout.LayoutParams(dp(48), dp(48)))
        row.addView(valueText, LinearLayout.LayoutParams(dp(60), dp(48)))
        row.addView(plus, LinearLayout.LayoutParams(dp(48), dp(48)))
        parent.addView(row)
    }

    private fun showAdjustValue(title: String, min: Int, max: Int, start: Int) {
        var value = start
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(18), dp(8), dp(18), dp(8)) }
        val valueText = TextView(this).apply { text = value.toString(); textSize = 24f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
        fun refresh() { valueText.text = value.toString() }
        box.addView(Button(this).apply { text = "−"; textSize = 22f; setOnClickListener { value = (value - 1).coerceAtLeast(min); refresh() } }, LinearLayout.LayoutParams(dp(64), dp(56)))
        box.addView(valueText, LinearLayout.LayoutParams(0, dp(56), 1f))
        box.addView(Button(this).apply { text = "+"; textSize = 22f; setOnClickListener { value = (value + 1).coerceAtMost(max); refresh() } }, LinearLayout.LayoutParams(dp(64), dp(56)))
        AlertDialog.Builder(this).setTitle(title).setView(box).setPositiveButton("OK", null).setNegativeButton("CANCEL", null).show()
    }

    private fun printexPrintSettings() {
        val items = arrayOf("Copies", "Pages", "Multi-Page Printing", "Double-Sided Printing", "Paper Size & Layout", "Paper & Printing Options", "Default")
        AlertDialog.Builder(this)
            .setTitle("Print Settings")
            .setItems(items) { _, which ->
                if (which == items.lastIndex) {
                    Toast.makeText(this, "Print Settings restored to default", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this).setTitle(items[which]).setMessage("Choose the required ${items[which].lowercase()} options here.").setPositiveButton("DONE", null).show()
                }
            }.show()
    }

    private fun printexChooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        startActivityForResult(intent, 8101)
    }

    private fun printexPreview() {
        AlertDialog.Builder(this).setTitle("Print Preview").setMessage("Preview is ready for the selected document. Print settings remain unchanged.").setPositiveButton("OK", null).show()
    }

    private fun printexPrintNow() {
        AlertDialog.Builder(this).setTitle("Print").setMessage("Select a document first, then review Print Settings before sending it to the connected printer.").setPositiveButton("OK", null).show()
    }

'''
    s = s.replace(marker, method + marker, 1)

MAIN.write_text(s, encoding='utf-8')
print('Fresh integrated Printex tab added')
