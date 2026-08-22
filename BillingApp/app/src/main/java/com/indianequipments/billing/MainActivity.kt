package com.indianequipments.billing

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: BillingDb
    private lateinit var templateFile: File
    private var customers = listOf<Customer>()
    private var items = listOf<Item>()
    private val nav = java.util.ArrayDeque<String>()
    private var current = "home"
    private var currentLines = mutableListOf<BillLine>()
    private val bg = Color.rgb(5, 14, 27)
    private val surface = Color.rgb(12, 28, 48)
    private val surface2 = Color.rgb(18, 39, 65)
    private val primary = Color.rgb(35, 104, 255)
    private val cyan = Color.rgb(48, 196, 232)
    private val green = Color.rgb(24, 190, 132)
    private val orange = Color.rgb(245, 139, 45)
    private val purple = Color.rgb(143, 92, 246)
    private val white = Color.rgb(241, 247, 255)
    private val muted = Color.rgb(153, 173, 201)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        db = BillingDb(this)
        templateFile = File(filesDir, "invoice_template.xlsx")
        refresh()
        showHome(false)
    }

    override fun onBackPressed() {
        if (nav.isNotEmpty()) render(nav.removeLast(), false) else if (current != "home") render("home", false) else super.onBackPressed()
    }

    private fun refresh() { customers = db.customers(); items = db.items() }

    private fun render(name: String, push: Boolean) {
        if (push && current != name) nav.addLast(current)
        current = name
        when (name) {
            "home" -> showHome(false)
            "invoice" -> newInvoice(false)
            "customers" -> customerMaster(false)
            "items" -> itemMaster(false)
            "history" -> history(false)
            "reports" -> reports(false)
            "settings" -> settings(false)
            else -> showHome(false)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 16, stroke: Int? = null): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) } }

    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color); setPadding(dp(2), dp(2), dp(2), dp(2)); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun field(hint: String, value: String = ""): EditText = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 15f; setTextColor(white); setHintTextColor(muted); setSingleLine(true); setPadding(dp(14), 0, dp(14), 0); background = rounded(surface2, 12, Color.rgb(31, 57, 88))
    }

    private fun action(title: String, sub: String, color: Int, click: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(12), dp(14), dp(12)); background = rounded(color, 14); isClickable = true; setOnClickListener { click() }
        addView(text(title, 18f, Color.WHITE, true)); addView(text(sub, 11f, Color.WHITE))
    }

    private fun card(title: String, sub: String, icon: String, color: Int, click: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(surface, 15, Color.rgb(23, 45, 71)); isClickable = true; setOnClickListener { click() }
        val badge = TextView(this@MainActivity).apply { this.text = icon; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = rounded(color, 12) }
        addView(badge, LinearLayout.LayoutParams(dp(50), dp(50)))
        val box = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(6), 0) }
        box.addView(text(title, 16f, white, true)); box.addView(text(sub, 11f, muted)); addView(box, LinearLayout.LayoutParams(0, -2, 1f)); addView(text("›", 30f, muted))
    }

    private fun root(title: String, sub: String = ""): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(18)); setBackgroundColor(bg) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = Button(this).apply { text = "‹"; textSize = 30f; setTextColor(white); background = rounded(Color.TRANSPARENT, 10); setOnClickListener { onBackPressed() } }
        bar.addView(back, LinearLayout.LayoutParams(dp(52), dp(52)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        titles.addView(text(title, 20f, white, true)); if (sub.isNotBlank()) titles.addView(text(sub, 11f, muted))
        bar.addView(titles, LinearLayout.LayoutParams(0, dp(58), 1f)); outer.addView(bar)
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content); outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(outer); return content
    }

    private fun section(title: String): TextView = text(title.uppercase(Locale.US), 11f, muted, true).apply { setPadding(0, dp(12), 0, dp(6)) }
    private fun addGap(parent: LinearLayout, h: Int = 8) { parent.addView(Space(this), LinearLayout.LayoutParams(1, dp(h))) }

    private fun showHome(push: Boolean = true) {
        if (push) render("home", true) else current = "home"
        refresh()
        val v = root("INDIAN EQUIPMENTS", "Premium GST Invoice Studio • FY ${Numbering.financialYear()}")
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(18)); background = rounded(Color.rgb(15, 67, 145), 20) }
        hero.addView(text("Good Morning, Admin  👋", 24f, Color.WHITE, true)); hero.addView(text("Invoice-only • Template-driven • Professional workflow", 13f, Color.rgb(211, 229, 255))); addGap(hero, 10)
        val stats = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        stats.addView(stat("FY", Numbering.financialYear()), LinearLayout.LayoutParams(0, dp(48), 1f)); stats.addView(stat("PARTIES", customers.size.toString()), LinearLayout.LayoutParams(0, dp(48), 1f)); stats.addView(stat("ITEMS", items.size.toString()), LinearLayout.LayoutParams(0, dp(48), 1f)); hero.addView(stats); v.addView(hero)
        v.addView(section("Quick Create"))
        val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r1.addView(action("＋  INVOICE", "GST Tax Invoice", primary) { render("invoice", true) }, LinearLayout.LayoutParams(0, dp(78), 1f)); r1.addView(Space(this), LinearLayout.LayoutParams(dp(6), 1)); r1.addView(action("⌁  DUPLICATE", "From bill history", purple) { duplicateLatest() }, LinearLayout.LayoutParams(0, dp(78), 1f)); v.addView(r1); addGap(v, 8)
        v.addView(card("Customer / Party Master", "GSTIN • address • state • contact", "👥", cyan) { render("customers", true) }); addGap(v, 7)
        v.addView(card("Item Master", "HSN • description • unit • GST • rate", "📦", green) { render("items", true) }); addGap(v, 7)
        v.addView(card("Bills & History", "Search, review and regenerate invoices", "🧾", purple) { render("history", true) }); addGap(v, 7)
        v.addView(card("Reports / Summary", "Invoice count • taxable • GST totals", "📊", orange) { render("reports", true) }); addGap(v, 7)
        v.addView(card("Settings / Excel Template", "Template mapping • numbering • backup", "⚙", primary) { render("settings", true) })
    }

    private fun stat(a: String, b: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(text(a, 9f, Color.rgb(178, 207, 246), true)); addView(text(b, 14f, Color.WHITE, true)) }

    private fun customerMaster(push: Boolean = true) {
        if (push) { render("customers", true); return }; refresh(); val v = root("Customer / Party Master", "${customers.size} saved parties")
        v.addView(action("＋  ADD CUSTOMER", "Create a reusable party profile", green) { customerDialog() }); addGap(v)
        customers.forEach { c -> v.addView(card(c.name, "${c.gstin.ifBlank { "GSTIN not set" }} • ${c.state.ifBlank { "State not set" }}", "👤", cyan) { customerDialog(c) }); addGap(v, 6) }
    }

    private fun customerDialog(existing: Customer? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val f = listOf(field("Party / Customer name", existing?.name ?: ""), field("GSTIN", existing?.gstin ?: ""), field("Address", existing?.address ?: ""), field("State", existing?.state ?: ""), field("State code", existing?.stateCode ?: ""), field("Phone", existing?.phone ?: ""), field("Email", existing?.email ?: ""))
        f.forEach { box.addView(it, LinearLayout.LayoutParams(-1, dp(52))); addGap(box, 5) }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Add Customer" else "Edit Customer").setView(box).setPositiveButton("SAVE") { _, _ ->
            if (f[0].text.toString().trim().isEmpty()) { toast("Customer name is required"); return@setPositiveButton }
            db.addCustomer(Customer(0, f[0].text.toString(), f[1].text.toString(), f[2].text.toString(), f[3].text.toString(), f[4].text.toString(), f[5].text.toString(), f[6].text.toString())); customerMaster(false)
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun itemMaster(push: Boolean = true) {
        if (push) { render("items", true); return }; refresh(); val v = root("Item / Product Master", "HSN • description choices • GST • default rate")
        v.addView(action("＋  ADD ITEM", "Save item data once, reuse on every invoice", cyan) { itemDialog() }); addGap(v)
        items.forEach { i -> v.addView(card(i.name, "HSN ${i.hsn} • ${i.unit} • GST ${i.gst}% • ₹${i.defaultRate}", "📦", green) { itemDialog(i) }); addGap(v, 6) }
    }

    private fun itemDialog(existing: Item? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val f = listOf(field("Item name", existing?.name ?: ""), field("Description choices (use | between choices)", existing?.description ?: ""), field("HSN code", existing?.hsn ?: ""), field("Unit", existing?.unit ?: "Nos"), field("GST %", existing?.gst?.toString() ?: "18"), field("Default rate", existing?.defaultRate?.toString() ?: "0"))
        f.forEach { box.addView(it, LinearLayout.LayoutParams(-1, dp(52))); addGap(box, 5) }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Add Item" else "Edit Item").setView(box).setPositiveButton("SAVE") { _, _ ->
            if (f[0].text.toString().trim().isEmpty()) { toast("Item name is required"); return@setPositiveButton }
            db.addItem(Item(0, f[0].text.toString(), f[1].text.toString(), f[2].text.toString(), f[3].text.toString().ifBlank { "Nos" }, f[4].text.toString().toDoubleOrNull() ?: 0.0, f[5].text.toString().toDoubleOrNull() ?: 0.0)); itemMaster(false)
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun newInvoice(push: Boolean = true) {
        if (push) { render("invoice", true); return }
        refresh(); currentLines.clear()
        val v = root("Create Invoice", "Invoice-only • document details • customer • items")
        val number = Numbering.next(this); val date = Numbering.date(); val note = field("Delivery Note"); val destination = field("Destination")
        val customer = Spinner(this); customer.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, customers.map { it.name }.ifEmpty { listOf("＋ Add customer first") })
        v.addView(section("Invoice identity")); v.addView(infoCard("INVOICE NUMBER", number, primary)); v.addView(infoCard("DATE", date, surface2)); v.addView(note, LinearLayout.LayoutParams(-1, dp(52))); addGap(v, 6); v.addView(destination, LinearLayout.LayoutParams(-1, dp(52)))
        v.addView(section("Customer / party")); v.addView(customer, LinearLayout.LayoutParams(-1, dp(52))); v.addView(section("Tax mode"))
        val tax = Spinner(this); tax.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("CGST + SGST", "IGST", "No Tax")); v.addView(tax, LinearLayout.LayoutParams(-1, dp(52)))
        v.addView(section("Invoice items")); val lineBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; v.addView(lineBox)
        v.addView(action("＋  ADD ITEM LINE", "Select item, description, quantity, rate and discount", cyan) { if (items.isEmpty()) toast("First add an item in Item Master") else lineDialog(lineBox) }); addGap(v, 10)
        v.addView(action("GENERATE INVOICE  →", "Writes data into the exact Excel template cells", green) {
            if (customers.isEmpty()) toast("Add a customer first") else if (lineBox.childCount == 0) toast("Add at least one item line") else {
                val c = customers[customer.selectedItemPosition.coerceIn(0, customers.lastIndex)]
                val mode = when (tax.selectedItemPosition) { 1 -> "IGST"; 2 -> "NONE"; else -> "CGST_SGST" }
                val bill = Bill(0, "Invoice", number, date, c, currentLines.toList(), mode, System.currentTimeMillis(), note.text.toString(), destination.text.toString())
                db.saveBill(bill.type, bill.number, bill.date, bill.customer.id, bill.taxMode, bill.lines); generateInvoice(bill)
            }
        })
    }

    private fun lineDialog(parent: LinearLayout) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, dp(8), 0) }
        val item = Spinner(this); item.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.name }); val desc = Spinner(this); val qty = field("Quantity", "1"); val rate = field("Rate"); val discount = field("Discount", "0")
        fun load(pos: Int) { val it = items.getOrNull(pos) ?: return; val choices = it.description.split('|').map { x -> x.trim() }.filter { x -> x.isNotEmpty() }.ifEmpty { listOf(it.name) }; desc.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices); rate.setText(it.defaultRate.toString()) }
        item.onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: AdapterView<*>?) {}; override fun onItemSelected(p: AdapterView<*>?, view: View?, pos: Int, id: Long) { load(pos) } }
        box.addView(item); addGap(box, 5); box.addView(desc); addGap(box, 5); box.addView(qty, LinearLayout.LayoutParams(-1, dp(52))); addGap(box, 5); box.addView(rate, LinearLayout.LayoutParams(-1, dp(52))); addGap(box, 5); box.addView(discount, LinearLayout.LayoutParams(-1, dp(52)))
        AlertDialog.Builder(this).setTitle("Add Invoice Item").setView(box).setPositiveButton("ADD") { _, _ ->
            val it = items[item.selectedItemPosition]; val line = BillLine(it, desc.selectedItem?.toString() ?: it.name, qty.text.toString().toDoubleOrNull() ?: 1.0, rate.text.toString().toDoubleOrNull() ?: it.defaultRate, discount.text.toString().toDoubleOrNull() ?: 0.0); currentLines.add(line)
            parent.addView(card("${currentLines.size}. ${it.name}", "${line.qty} ${it.unit} × ₹${line.rate} • GST ${it.gst}% • taxable ₹${String.format(Locale.US, "%.2f", line.taxable)}", "•", primary) {}); addGap(parent, 5)
        }.setNegativeButton("CANCEL", null).show()
    }

    private fun infoCard(k: String, value: String, color: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(10), dp(15), dp(10)); background = rounded(color, 13); addView(text(k, 9f, Color.WHITE, true)); addView(text(value, 17f, Color.WHITE, true)); val lp = LinearLayout.LayoutParams(-1, dp(66)); lp.setMargins(0, 0, 0, dp(6)); layoutParams = lp
    }

    private fun generateInvoice(bill: Bill) {
        if (!templateFile.exists()) { toast("Excel template not found. Put the invoice workbook in app storage first."); return }
        try {
            val out = File(getExternalFilesDir(null), "${bill.number.replace('/', '_')}.xlsx")
            XlsxGenerator.generate(this, templateFile, out, bill)
            AlertDialog.Builder(this).setTitle("Invoice Generated ✓").setMessage("Invoice: ${bill.number}\nSaved as:\n${out.absolutePath}").setPositiveButton("OK") { _, _ -> showHome(false) }.show()
        } catch (e: Exception) { toast("Excel generation failed: ${e.message ?: "unknown error"}") }
    }

    private fun history(push: Boolean = true) {
        if (push) { render("history", true); return }; val v = root("Bills & History", "Latest 100 invoice records"); val list = db.recentBills(); if (list.isEmpty()) v.addView(infoCard("NO INVOICES", "Create your first invoice", surface2)); list.forEach { row -> v.addView(card(row[0], "${row[2]} • ${row[3]} • Taxable ₹${row[4]}", "🧾", purple) { toast("Invoice ${row[0]} saved in history") }); addGap(v, 6) }
    }

    private fun reports(push: Boolean = true) {
        if (push) { render("reports", true); return }; val v = root("Reports / Summary", "Invoice-only financial overview"); val list = db.recentBills(); var taxable = 0.0; list.forEach { taxable += it[4].toDoubleOrNull() ?: 0.0 }; v.addView(infoCard("TOTAL INVOICES", list.size.toString(), primary)); v.addView(infoCard("RECORDED TAXABLE VALUE", "₹${String.format(Locale.US, "%.2f", taxable)}", green)); v.addView(infoCard("FINANCIAL YEAR", Numbering.financialYear(), purple))
    }

    private fun settings(push: Boolean = true) {
        if (push) { render("settings", true); return }; val v = root("Settings / Excel Template", "Invoice workbook is the source of truth")
        v.addView(infoCard("EXACT CELL MAPPING", "Invoice No. → I4\nDate → K5\nDelivery Note → I6\nDestination → K18\nItems → B21:B32\nHSN → F21:F32\nQty → G21:G32\nSize → H21:H32\nRate → I21:I32\nGST → J21:J32\nUnit → K21:K32\nAmount → L21:L32\nTotal Amount → L33\nTotal GST → L34\nGrand Amount → L35\nTaxable → F38:F39\nCGST → I38:I39\nSGST → K38:K39\nGST Total → L38:L39", surface2)); addGap(v)
        v.addView(action("IMPORT / REPLACE EXCEL TEMPLATE", "Use the exact invoice workbook you provided", primary) { toast("Template picker will be connected to this invoice-only screen") }); addGap(v)
        v.addView(action("INVOICE NUMBERING", "Format: IE/${Numbering.financialYear()}/16 and onward", green) { numberingDialog() })
    }

    private fun numberingDialog() {
        val e = field("Next invoice sequence", "16")
        AlertDialog.Builder(this).setTitle("Invoice numbering").setView(e).setPositiveButton("SAVE") { _, _ -> Numbering.setInitialSequence(this, e.text.toString().toIntOrNull() ?: 16); toast("Next sequence saved") }.setNegativeButton("CANCEL", null).show()
    }

    private fun duplicateLatest() { val last = db.recentBills().firstOrNull(); if (last == null) toast("No invoice available to duplicate") else toast("Duplicate workflow will start from ${last[0]}") }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
