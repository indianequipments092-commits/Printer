package com.indianequipments.billing

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

class MainActivity: Activity(){
    private lateinit var db:BillingDb
    private lateinit var root:LinearLayout
    private var customers=listOf<Customer>(); private var items=listOf<Item>(); private var lines=mutableListOf<BillLine>()
    private var selectedCustomer:Customer?=null
    private lateinit var templateFile:File
    private val BLUE=Color.rgb(13,71,161)
    private val PAD=16

    override fun onCreate(b:Bundle?){super.onCreate(b);db=BillingDb(this);templateFile=File(filesDir,"invoice_template.xlsx");refresh();home()}
    private fun refresh(){customers=db.customers();items=db.items()}
    private fun base(title:String):LinearLayout{root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(PAD,PAD,PAD,PAD);root.setBackgroundColor(Color.rgb(247,249,252));
        val bar=LinearLayout(this);bar.gravity=Gravity.CENTER_VERTICAL; val t=TextView(this);t.text=title;t.textSize=24f;t.setTextColor(BLUE);t.setTypeface(null,1);bar.addView(t,LinearLayout.LayoutParams(0,60,1f)); val menu=Button(this);menu.text="☰";menu.setOnClickListener{home()};bar.addView(menu,LinearLayout.LayoutParams(56,60));root.addView(bar);setContentView(root);return root}
    private fun button(text:String,click:()->Unit)=Button(this).apply{this.text=text;setOnClickListener{click()}}
    private fun input(hint:String)=EditText(this).apply{this.hint=hint;this.setPadding(14,10,14,10);this.setSingleLine(false)}
    private fun home(){val v=base("Advanced Billing"); val fy=Numbering.financialYear();
        val hero=TextView(this).apply{text="GST Billing Studio\nFinancial Year $fy\nTemplate-driven • Tally-style workflow";textSize=18f;setTextColor(Color.WHITE);setPadding(20,22,20,22);setBackgroundColor(BLUE)};v.addView(hero)
        v.addView(button("＋  New Document"){newBill()}); v.addView(button("👥  Customer / Party Master"){customerMaster()});v.addView(button("📦  Item Master"){itemMaster()});v.addView(button("🧾  Bills & History"){history()});v.addView(button("📊  Reports / Summary"){report()});v.addView(button("⚙  Settings / Excel Template"){settings()})
    }
    private fun customerMaster(){val v=base("Customer / Party Master");v.addView(button("＋ Add Customer"){customerDialog()});customers.forEach{c->val tv=TextView(this).apply{text="${c.name}\nGSTIN: ${c.gstin}\n${c.state} • ${c.phone}";textSize=16f;setPadding(10,14,10,14);setBackgroundColor(Color.WHITE)};v.addView(tv)} }
    private fun customerDialog(){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val name=input("Party / Customer name");val gst=input("GSTIN");val address=input("Address");val state=input("State");val code=input("State code");val phone=input("Phone");val email=input("Email");listOf(name,gst,address,state,code,phone,email).forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("Add Customer").setView(box).setPositiveButton("Save"){_,_->db.addCustomer(Customer(0,name.text.toString(),gst.text.toString(),address.text.toString(),state.text.toString(),code.text.toString(),phone.text.toString(),email.text.toString()));refresh();customerMaster()}.setNegativeButton("Cancel",null).show()}
    private fun itemMaster(){val v=base("Item / Product Master");v.addView(button("＋ Add Item"){itemDialog()});items.forEach{it->val tv=TextView(this).apply{text="${it.name}  • HSN ${it.hsn}\n${it.description}\n${it.unit} • GST ${it.gst}% • ₹${it.defaultRate}";textSize=16f;setPadding(10,14,10,14);setBackgroundColor(Color.WHITE)};v.addView(tv)} }
    private fun itemDialog(){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val name=input("Item name");val desc=input("Default description");val hsn=input("HSN code");val unit=input("Unit (Nos/Kg/Mtr/etc.)");val gst=input("GST %");val rate=input("Default rate");listOf(name,desc,hsn,unit,gst,rate).forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("Add Item").setView(box).setPositiveButton("Save"){_,_->db.addItem(Item(0,name.text.toString(),desc.text.toString(),hsn.text.toString(),unit.text.toString().ifBlank{"Nos"},gst.text.toString().toDoubleOrNull()?:0.0,rate.text.toString().toDoubleOrNull()?:0.0));refresh();itemMaster()}.setNegativeButton("Cancel",null).show()}
    private fun newBill(){refresh();lines.clear();selectedCustomer=null;val v=base("New Document");
        val type=Spinner(this);type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Invoice","Delivery Challan","Proforma Invoice","Quotation"));v.addView(label("Document Type"));v.addView(type)
        val party=Spinner(this);party.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,customers.map{it.name});v.addView(label("Customer / Party"));v.addView(party)
        val tax=Spinner(this);tax.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("CGST + SGST","IGST","No Tax"));v.addView(label("Tax Mode"));v.addView(tax)
        val linesBox=LinearLayout(this);linesBox.orientation=LinearLayout.VERTICAL;v.addView(label("Items"));v.addView(linesBox)
        val add=button("＋ Add Item Line"){ if(items.isEmpty()){toast("Add items in Item Master first");return@button}; lineDialog(linesBox)};v.addView(add)
        val generate=button("✓ Save & Generate Excel Bill"){if(customers.isEmpty()||items.isEmpty()||party.selectedItemPosition<0||lines.isEmpty()){toast("Select a customer and add at least one item");return@button};selectedCustomer=customers[party.selectedItemPosition];val number=if(type.selectedItem.toString()=="Invoice")Numbering.next(this) else type.selectedItem.toString().uppercase(Locale.US).replace(" ","-")+"/"+Numbering.financialYear()+"/"+System.currentTimeMillis().toString().takeLast(5);val bill=Bill(0,type.selectedItem.toString(),number,Numbering.date(),selectedCustomer!!,lines.toList(),if(tax.selectedItemPosition==1)"IGST" else if(tax.selectedItemPosition==2)"NONE" else "CGST_SGST",System.currentTimeMillis());db.saveBill(bill.type,bill.number,bill.date,bill.customer.id,bill.taxMode,bill.lines);generateFiles(bill)};v.addView(generate)
    }
    private fun lineDialog(parent:LinearLayout){val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,items.map{it.name});val desc=input("Description (defaults from item)");val qty=input("Quantity");val rate=input("Rate");val discount=input("Discount");box.addView(sp);box.addView(desc);box.addView(qty);box.addView(rate);box.addView(discount);AlertDialog.Builder(this).setTitle("Add Item Line").setView(box).setPositiveButton("Add"){_,_->val item=items[sp.selectedItemPosition];val d=desc.text.toString().ifBlank{item.description};val l=BillLine(item,d,qty.text.toString().toDoubleOrNull()?:1.0,rate.text.toString().toDoubleOrNull()?:item.defaultRate,discount.text.toString().toDoubleOrNull()?:0.0);lines.add(l);val tv=TextView(this).apply{text="${lines.size}. ${l.description} • ${l.qty} × ₹${l.rate} • HSN ${l.item.hsn}";setPadding(10,12,10,12);textSize=15f};parent.addView(tv)}.setNegativeButton("Cancel",null).show()}
    private fun generateFiles(bill:Bill){if(!templateFile.exists()){toast("Please import your Excel template in Settings first");return};val dir=getExternalFilesDir("Billing")?:filesDir;val safe=bill.number.replace('/','_');val x=File(dir,"$safe.xlsx");XlsxGenerator.generate(this,templateFile,x,bill);toast("Generated: ${x.name}");share(x,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")}
    private fun history(){val v=base("Bills & History");db.recentBills().forEach{r->val tv=TextView(this).apply{text="${r[0]}\n${r[1]} • ${r[2]} • ${r[3]}\nTaxable ₹${r[4]}";textSize=16f;setPadding(10,14,10,14);setBackgroundColor(Color.WHITE)};v.addView(tv)} }
    private fun report(){val v=base("Reports");val b=db.recentBills();val total=b.sumOf{it[4].toDoubleOrNull()?:0.0};v.addView(TextView(this).apply{text="Bills: ${b.size}\nTaxable value: ₹${String.format(Locale.US,"%.2f",total)}\nFinancial year: ${Numbering.financialYear()}";textSize=20f;setPadding(20,20,20,20)})}
    private fun settings(){val v=base("Settings");v.addView(TextView(this).apply{text="Excel template: ${if(templateFile.exists())"Imported ✓" else "Not imported"}";textSize=17f});v.addView(button("Import / Replace Excel Invoice Template"){pickTemplate()});v.addView(TextView(this).apply{text="The imported workbook is preserved as the master visual template. Generated invoices fill its cells without redesigning the bill.";setPadding(8,20,8,8)})}
    private fun pickTemplate(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";addCategory(Intent.CATEGORY_OPENABLE)},501)}
    override fun onActivityResult(req:Int,res:Int,data:Intent?){super.onActivityResult(req,res,data);if(req==501&&res==RESULT_OK&&data?.data!=null){contentResolver.openInputStream(data.data!!)?.use{input->FileOutputStream(templateFile).use{input.copyTo(it)}};toast("Excel template imported ✓");settings()}}
    private fun share(f:File,mime:String){try{val uri=androidx.core.content.FileProvider.getUriForFile(this,packageName+".provider",f);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type=mime;putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share generated bill"))}catch(_:Exception){}}
    private fun label(s:String)=TextView(this).apply{text=s;textSize=14f;setPadding(2,10,2,4);setTextColor(Color.DKGRAY)}
    private fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
}
