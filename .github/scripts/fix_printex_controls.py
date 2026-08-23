from pathlib import Path

PRINT = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PrintExActivity.kt')
s = PRINT.read_text(encoding='utf-8')

# Printex image controls: both values use a symmetric -100..100 scale and
# always start at 0. The UI is intentionally numeric: minus / value / plus.
s = s.replace('private var contrast = 100', 'private var contrast = 0', 1)
s = s.replace('addSetting(imageSettings, "Brightness", brightness.toString()) { slider("Brightness", -100, 100, brightness) { brightness = it; refreshUi() } }',
              'addSetting(imageSettings, "Brightness", brightness.toString()) { adjustValueDialog("Brightness", brightness) { brightness = it; refreshUi() } }', 1)
s = s.replace('addSetting(imageSettings, "Contrast", "$contrast%") { slider("Contrast", 50, 200, contrast) { contrast = it; refreshUi() } }',
              'addSetting(imageSettings, "Contrast", contrast.toString()) { adjustValueDialog("Contrast", contrast) { contrast = it; refreshUi() } }', 1)

# Insert the numeric +/- dialog immediately before the old slider helper. Keep
# the old helper for compatibility with older generated source revisions.
if 'private fun adjustValueDialog(' not in s:
    marker = '    private fun slider(title: String, minValue: Int, maxValue: Int, current: Int, done: (Int) -> Unit) {'
    helper = '''    private fun adjustValueDialog(title: String, current: Int, done: (Int) -> Unit) {
        var value = current.coerceIn(-100, 100)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        val minus = Button(this).apply {
            text = "−"
            textSize = 22f
            setTextColor(Color.WHITE)
            background = rounded(PANEL, 14)
        }
        val valueText = TextView(this).apply {
            textSize = 22f
            setTypeface(null, 1)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val plus = Button(this).apply {
            text = "+"
            textSize = 22f
            setTextColor(Color.WHITE)
            background = rounded(PANEL, 14)
        }
        fun updateValue() { valueText.text = value.toString() }
        minus.setOnClickListener { value = (value - 1).coerceAtLeast(-100); updateValue() }
        plus.setOnClickListener { value = (value + 1).coerceAtMost(100); updateValue() }
        updateValue()
        row.addView(minus, LinearLayout.LayoutParams(dp(58), dp(52)))
        row.addView(valueText, LinearLayout.LayoutParams(0, dp(52), 1f))
        row.addView(plus, LinearLayout.LayoutParams(dp(58), dp(52)))
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(row)
            .setPositiveButton("OK") { _, _ -> done(value) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

'''
    if marker in s:
        s = s.replace(marker, helper + marker, 1)

# Make Printex use the same bottom-navigation proportions and spacing as Home.
old_nav = '''    private fun navBar(): View {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)); background = rounded(PANEL, 22) }
        navItem(bar, "⌂", "Home", MainActivity::class.java, false); navItem(bar, "◉", "Studio", MainActivity::class.java, false); navItem(bar, "▦", "Library", LibraryActivity::class.java, false); navItem(bar, "▣", "Printex", PrintExActivity::class.java, true); return bar
    }'''
new_nav = '''    private fun navBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(PANEL, 18)
        }
        navItem(bar, "⌂", "Home", MainActivity::class.java, false)
        navItem(bar, "◎", "Studio", MainActivity::class.java, false)
        navItem(bar, "▦", "Library", LibraryActivity::class.java, false)
        navItem(bar, "▣", "Printex", PrintExActivity::class.java, true)
        return bar
    }'''
if old_nav in s:
    s = s.replace(old_nav, new_nav, 1)

# Keep each item visually aligned with MainActivity's shared bottom navigation.
old_item = '''    private fun navItem(bar: LinearLayout, icon: String, label: String, target: Class<*>, active: Boolean) {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { if (!active) openTab(target) } }
        item.addView(txt(icon, 20f, if (active) BLUE else MUTED, false)); item.addView(txt(label, 10f, if (active) BLUE else MUTED, active)); bar.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }'''
new_item = '''    private fun navItem(bar: LinearLayout, icon: String, label: String, target: Class<*>, active: Boolean) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
            setOnClickListener { if (!active) openTab(target) }
        }
        item.addView(txt(icon, 20f, if (active) BLUE else MUTED, true))
        item.addView(txt(label, 11f, if (active) BLUE else MUTED, true))
        bar.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
    }'''
if old_item in s:
    s = s.replace(old_item, new_item, 1)

PRINT.write_text(s, encoding='utf-8')
print('Fixed Printex numeric image controls and synchronized bottom navigation with Home')
