package com.indianequipments.billing

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: BillingDb
    private var customers = listOf<Customer>()
    private var items = listOf<Item>()
    private var lines = mutableListOf<BillLine>()
    private var selectedCustomer: Customer? = null
    private lateinit var templateFile: File
    private val bg = Color.rgb(7, 18, 33)
    private val surface = Color.rgb(14, 30, 50)
    private val surface2 = Color.rgb(19, 39, 64)
    private val blue = Color.rgb(38, 111, 255)
    private val cyan = Color.rgb(52, 211, 238)
    private val green = Color.rgb(24, 190, 130)
    private val orange = Color.rgb(245, 139, 45)
    private val purple = Color.rgb(143, 92, 246)
    private val text = Color.rgb(239, 246, 255)
    private val muted = Color.rgb(151, 169, 194)
    private val pad = 18
    private val screenStack = ArrayDeque<() -> Unit>()
    private var currentScreen = "home"

    override fun onCreate(b: Bundle?) { super.onCreate(b); db = BillingDb(this); templateFile = File(filesDir, "invoice_template.xlsx"); refresh(); showHome(false) }

    override fun onBackPressed() {
        if (screenStack.isNotEmpty()) screenStack.removeLast().invoke()
        else if (currentScreen != "home") showHome(false)
        else super.onBackPressed()
    }

    private fun refresh() { customers = db.customers(); items = db.items() }

    private fun navigate(name: String, action: () -> Unit) {
        if (currentScreen != name) {
            val old = currentScreen
            screenStack.addLast {
                when (old) {
                    "home" -> showHome(false)
                    "customers" -> customerMaster(false)
                    "items" -> itemMaster(false)
                    "history" -> history(false)
                    "reports" -> report(false)
                    "settings" -> settings(false)
                    else -> showHome(false)
                }
            }
        }
        currentScreen = name
        action()
    }

    private fun root(title: String, subtitle: String? = null, back: Boolean = true): LinearLayout {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, 10, pad, 18); setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        if (back) top.addView(iconButton("‹") { onBackPressed() }, LinearLayout.LayoutParams(50, 52))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(6, 0, 6, 0) }
        titles.addView(label(title, 21, text, true)); if (subtitle != null) titles.addView(label(subtitle, 12, muted))
        top.addView(titles, LinearLayout.LayoutParams(0, 58, 1f)); top.addView(iconButton("⋮") {}, LinearLayout.LayoutParams(50, 52)); root.addView(top); setContentView(root); return root
    }

    private fun label(s: String, size: Float, color: Int = text, bold: Boolean = false) = TextView(this).apply { this.text = s; textSize = size; setTextColor(color); setPadding(0, 3, 0, 3); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun iconButton(symbol: String, click: () -> Unit) = Button(this).apply { text = symbol; textSize = 24f; setTextColor(text); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { click() } }
    private fun button(textValue: String, click: () -> Unit) = Button(this).apply { text = textValue; textSize = 14f; setTextColor(text); setBackgroundColor(surface2); setOnClickListener { click() } }
    private fun input(hint: String) = EditText(this).apply { this.hint = hint; hintTextColor = muted; setTextColor(text); setSingleLine(true); setPadding(14, 12, 14, 12); setBackgroundColor(surface2) }

    private fun card(title: String, subtitle: String, icon: String, color: Int, click: () -> Unit): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(16, 14, 12, 14); setBackgroundColor(surface); setOnClickListener { click() } }
        val bubble = TextView(this).apply { this.text = icon; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setBackgroundColor(color) }
        c.addView(bubble, LinearLayout.LayoutParams(54, 54))
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 0, 8, 0) }
        tx.addView(label(title, 16, text, true)); tx.addView(label(subtitle, 12, muted)); c.addView(tx, LinearLayout.LayoutParams(0, 70, 1f)); c.addView(label("›", 30, muted))
        val lp = LinearLayout.LayoutParams(-1, 78); lp.setMargins(0, 6, 0, 6); c.layoutParams = lp; return c
    }

    private fun actionCard(title: String, subtitle: String, color: Int, click: () -> Unit): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 14, 12, 12); setBackgroundColor(color); setOnClickListener { click() } }
        c.addView(label(title, 17, Color.WHITE, true)); c.addView(label(subtitle, 11, Color.WHITE)); return c
    }

    private fun showHome(clear: Boolean = true) {
        currentScreen = "home"; if (clear) screenStack.clear(); refresh(); val v = root("INDIAN EQUIPMENTS", "GST Billing Studio • FY ${Numbering.financialYear()}", false)
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 18, 20, 18); setBackgroundColor(Color.rgb(13, 58, 132)) }
        hero.addView(label("Good Morning, Admin 👋", 22, Color.WHITE, true)); hero.addView(label("Premium GST Billing Studio", 13, Color.rgb(207, 225, 255))); hero.addView(label("Template-driven • Tally-style workflow", 12, Color.rgb(180, 210, 255)))
        val stats = LinearLayout(this).apply { setPadding(0, 14, 0, 0) }; stats.addView(stat("FY", Numbering.financialYear()), LinearLayout.LayoutParams(0, 55, 1f)); stats.addView(stat("Customers", customers.size.toString()), LinearLayout.LayoutParams(0, 55, 1f)); stats.addView(stat("Items", items.size.toString()), LinearLayout.LayoutParams(0, 55, 1f)); hero.addView(stats); v.addView(hero)
        v.addView(label("QUICK CREATE", 12, muted, true))
        val grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; grid.addView(actionCard("＋ Invoice", "GST Tax Invoice", blue) { newBill() }, LinearLayout.LayoutParams(0, 82, 1f)); grid.addView(actionCard("▣ Quotation", "Sales quotation", green) { newBill("Quotation") }, LinearLayout.LayoutParams(0, 82, 1f)); v.addView(grid)
        val grid2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; grid2.addView(actionCard("▤ Challan", "Delivery challan", orange) { newBill("Delivery Challan") }, LinearLayout.LayoutParams(0, 82, 1f)); grid2.addView(actionCard("▥ Proforma", "Proforma invoice", purple) { newBill("Proforma Invoice") }, LinearLayout.LayoutParams(0, 82, 1f)); v.addView(grid2)
        v.addView(card("Customer / Party Master", "Manage saved customers, GSTIN & addresses", "👥", cyan) { navigate("customers") { customerMaster(false) } })
        v.addView(card("Item Master", "HSN, description, unit, GST & default rate", "📦", green) { navigate("items") { itemMaster(false) } })
        v.addView(card("Bills & History", "Search, edit, duplicate and review documents", "🧾", purple) { navigate("history") { history(false) } })
        v.addView(card("Reports / Summary", "Sales, tax and customer insights", "📊", orange) { navigate("reports") { report(false) } })
        v.addView(card("Settings / Excel Template", "Company, numbering, template & backup", "⚙", blue) { navigate("settings") { settings(false) } })
    }

    private fun stat(a: String, b: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 0, 4, 0); addView(label(a, 10, Color.rgb(180, 210, 255))); addView(label(b, 14, Color.WHITE, true)) }

    private fun customerMaster(push: Boolean = true) {
        if (push) { navigate("customers") { customerMaster(false) }; return }
        refresh(); val v = root("Customers / Party Master", "${customers.size} saved parties"); v.addView(input("Search by name, GSTIN, phone or city…")); v.addView(button("＋  Add New Customer") { customerDialog() }); customers.forEach { c -> v.addView(card(c.name, "${c.gstin.ifBlank { "GSTIN not set" }} • ${c.state}", "👤", green) { customerDialog(c) }) }
    }

    private fun customerDialog(existing: Customer? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 0, 8, 0) }
        val fields = listOf(input("Party / Customer name"), input("GSTIN"), input("Address"), input("State"), input("State code"), input("Phone"), input("Email"))
        existing?.let { fields[0].setText(it.name); fields[1].setText(it.gstin); fields[2].setText(it.address); fields[3].setText(it.state); fields[4].setText(it.stateCode); fields[5].setText(it.phone); fields[6].setText(it.email) }
        fields.forEach { f -> box.addView(f, LinearLayout.LayoutParams(-1, 52)) }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Add Customer" else "Edit Customer").setView(box).setPositiveButton("Save") { _, _ -> if (existing == null) db.addCustomer(Customer(0, fields[0].text.toString(), fields[1].text.toString(), fields[2].text.toString(), fields[3].text.toString(), fields[4].text.toString(), fields[5].text.toString(), fields[6].text.toString())); refresh(); customerMaster(false) }.setNegativeButton("Cancel", null).show()
    }

    private fun itemMaster(push: Boolean = true) {
        if (push) { navigate("items") { itemMaster(false) }; return }
        refresh(); val v = root("Item / Product Master", "HSN • description • unit • GST • rate"); v.addView(button("＋  Add New Item") { itemDialog() }); items.forEach { it -> v.addView(card(it.name, "HSN ${it.hsn} • ${it.unit} • GST ${it.gst}% • ₹${it.defaultRate}", "📦", cyan) { itemDialog(it) }) }
    }

    private fun itemDialog(existing: Item? = null) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 0, 8, 0) }
        val fields = listOf(input("Item name"), input("Saved descriptions — separate choices with |"), input("HSN code"), input("Unit (Nos/Kg/Mtr/etc.)"), input("GST %"), input("Default rate"))
        existing?.let { fields[0].setText(it.name); fields[1].setText(it.description); fields[2].setText(it.hsn); fields[3].setText(it.unit); fields[4].setText(it.gst.toString()); fields[5].setText(it.defaultRate.toString()) }
        fields.forEach { f -> box.addView(f, LinearLayout.LayoutParams(-1, 52)) }
        AlertDialog.Builder(this).setTitle(if (existing == null) "Add Item" else "Edit Item").setView(box).setPositiveButton("Save") { _, _ -> if (existing == null) db.addItem(Item(0, fields[0].text.toString(), fields[1].text.toString(), fields[2].text.toString(), fields[3].text.toString().ifBlank { "Nos" }, fields[4].text.toString().toDoubleOrNull() ?: 0.0, fields[5].text.toString().toDoubleOrNull() ?: 0.0)); refresh(); itemMaster(false) }.setNegativeButton("Cancel", null).show()
    }

    private fun newBill(preset: String? = null, push: Boolean = true) {
        if (push) { navigate("newbill") { newBill(preset, false) }; return }
        refresh(); lines.clear(); selectedCustomer = null; val v = root("New Document", "Step 1 of 3 • Customer & document details")
        val type = Spinner(this); val types = arrayOf("Invoice", "Delivery Challan", "Proforma Invoice", "Quotation"); type.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types); preset?.let { type.setSelection(types.indexOf(it).coerceAtLeast(0)) }
        v.addView(label("DOCUMENT TYPE", 11, muted, true)); v.addView(type)
        val party = Spinner(this); party.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, customers.map { it.name }.ifEmpty { listOf("No customers — add one first") }); v.addView(label("CUSTOMER / PARTY", 11, muted, true)); v.addView(party)
        val tax = Spinner(this); tax.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("CGST + SGST", "IGST", "No Tax")); v.addView(label("TAX MODE", 11, muted, true)); v.addView(tax)
        v.addView(label("ITEM LINES", 11, muted, true)); val linesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; v.addView(linesBox)
        v.addView(button("＋  Add Item Line") { if (items.isEmpty()) toast("Add items in Item Master first") else lineDialog(linesBox) })
        v.addView(button("Next  →  Review & Generate") { if (customers.isEmpty() || items.isEmpty() || lines.isEmpty() || party.selectedItemPosition < 0) toast("Select customer and add at least one item") else { selectedCustomer = customers[party.selectedItemPosition]; val document = type.selectedItem.toString(); val number = if (document == "Invoice") Numbering.next(this) else document.uppercase(Locale.US).replace(" ", "-") + "/" + Numbering.financialYear() + "/" + System.currentTimeMillis().toString().takeLast(5); val bill = Bill(0, document, number, Numbering.date(), selectedCustomer!!, lines.toList(), if (tax.selectedItemPosition == 1) "IGST" else if (tax.selectedItemPosition == 2) "NONE" else "CGST_SGST", System.currentTimeMillis()); db.saveBill(bill.type, bill.number, bill.date, bill.customer.id, bill.taxMode, bill.lines); generateFiles(bill) } })
    }

    private fun lineDialog(parent: LinearLayout) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 0, 8, 0) }; val sp = Spinner(this); sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items.map { it.name }); val desc = Spinner(this); val qty = input("Quantity"); val rate = input("Rate"); val discount = input("Discount")
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: AdapterView<*>?) {}; override fun onItemSelected(p: AdapterView<*>?, view: View?, pos: Int, id: Long) { val item = items.getOrNull(pos) ?: return; val choices = item.description.split('|').map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(item.name) }; desc.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, choices); rate.setText(item.defaultRate.toString()) } }
        box.addView(label("ITEM", 11, muted, true)); box.addView(sp); box.addView(label("DESCRIPTION — SELECT SAVED CHOICE", 11, muted, true)); box.addView(desc); box.addView(qty); box.addView(rate); box.addView(discount)
        AlertDialog.Builder(this).setTitle("Add Item Line").setView(box).setPositiveButton("Add") { _, _ -> val item = items[sp.selectedItemPosition]; val d = desc.selectedItem?.toString().orEmpty().ifBlank { item.description }; lines.add(BillLine(item, d, qty.text.toString().toDoubleOrNull() ?: 1.0, rate.text.toString().toDoubleOrNull() ?: item.defaultRate, discount.text.toString().toDoubleOrNull() ?: 0.0)); parent.addView(label("${lines.size}. ${item.name} • ${lines.last().qty} × ₹${lines.last().rate} • HSN ${item.hsn}", 14, text)) }.setNegativeButton("Cancel", null).show()
    }

    private fun generateFiles(bill: Bill) { if (!templateFile.exists()) { toast("Import your Excel invoice template in Settings first"); settings(false); return }; val dir = getExternalFilesDir("Billing") ?: filesDir; val safe = bill.number.replace('/', '_'); val x = File(dir, "$safe.xlsx"); XlsxGenerator.generate(this, templateFile, x, bill); toast("Generated ${x.name}"); share(x, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }

    private fun history(push: Boolean = true) { if (push) { navigate("history") { history(false) }; return }; val v = root("Bills & History", "All documents • newest first"); v.addView(input("Search invoice no., customer or type…")); db.recentBills().forEach { r -> v.addView(card(r[0].toString(), "${r[1]} • ${r[2]} • ${r[3]} • Taxable ₹${r[4]}", "🧾", purple) {}) } }
    private fun report(push: Boolean = true) { if (push) { navigate("reports") { report(false) }; return }; val v = root("Reports / Summary", "Business overview • FY ${Numbering.financialYear()}"); val b = db.recentBills(); val total = b.sumOf { it[4].toDoubleOrNull() ?: 0.0 }; v.addView(card("Total Bills", b.size.toString(), "🧾", blue) {}); v.addView(card("Taxable Value", "₹${String.format(Locale.US, "%.2f", total)}", "₹", green) {}); v.addView(card("Customers", customers.size.toString(), "👥", cyan) {}); v.addView(card("Items", items.size.toString(), "📦", orange) {}) }
    private fun settings(push: Boolean = true) { if (push) { navigate("settings") { settings(false) }; return }; val v = root("Settings / Excel Template", "Company, numbering and output controls"); v.addView(card("Excel Template", if (templateFile.exists()) "Imported ✓ • master format ready" else "Not imported yet", "▣", green) { pickTemplate() }); v.addView(card("Invoice Numbering", "IE/${Numbering.financialYear()}/16 • automatic sequence", "#", blue) { val e = input("Next invoice number"); e.setText("16"); AlertDialog.Builder(this).setTitle("Invoice numbering").setView(e).setPositiveButton("Save") { _, _ -> Numbering.setInitialSequence(this, e.text.toString().toIntOrNull() ?: 16); toast("Next sequence saved") }.setNegativeButton("Cancel", null).show() }); v.addView(label("OUTPUT", 11, muted, true)); v.addView(button("Import / Replace Excel Invoice Template") { pickTemplate() }); v.addView(label("The workbook stays the master visual format. The app fills its cells; it does not redesign your bill.", 12, muted)) }

    private fun pickTemplate() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; addCategory(Intent.CATEGORY_OPENABLE) }, 501) }
    override fun onActivityResult(req: Int, res: Int, data: Intent?) { super.onActivityResult(req, res, data); if (req == 501 && res == RESULT_OK && data?.data != null) { contentResolver.openInputStream(data.data!!)?.use { input -> FileOutputStream(templateFile).use { input.copyTo(it) } }; toast("Excel template imported ✓"); settings(false) } }
    private fun share(f: File, mime: String) { try { val uri = androidx.core.content.FileProvider.getUriForFile(this, packageName + ".provider", f); startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share generated bill")) } catch (_: Exception) { toast("File created: ${f.absolutePath}") } }
    private fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_LONG).show() }
}
