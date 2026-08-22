package com.indianequipments.billing

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Customer(val id: Long, val name: String, val gstin: String, val address: String, val state: String, val stateCode: String, val phone: String, val email: String)
data class Item(val id: Long, val name: String, val description: String, val hsn: String, val unit: String, val gst: Double, val defaultRate: Double)
data class BillLine(val item: Item, var description: String, var qty: Double, var rate: Double, var discount: Double = 0.0) { val taxable: Double get() = (qty * rate - discount).coerceAtLeast(0.0) }
data class Bill(
    val id: Long,
    val type: String,
    val number: String,
    val date: String,
    val customer: Customer,
    val lines: List<BillLine>,
    val taxMode: String,
    val createdAt: Long,
    val deliveryNote: String = "",
    val destination: String = ""
)

class BillingDb(context: Context) : SQLiteOpenHelper(context, "billing.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,gstin TEXT,address TEXT,state TEXT,state_code TEXT,phone TEXT,email TEXT)")
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,description TEXT,hsn TEXT,unit TEXT,gst REAL,rate REAL)")
        db.execSQL("CREATE TABLE bills(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,number TEXT,date TEXT,customer_id INTEGER,tax_mode TEXT,created_at INTEGER)")
        db.execSQL("CREATE TABLE bill_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,bill_id INTEGER,item_id INTEGER,description TEXT,qty REAL,rate REAL,discount REAL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun addCustomer(c: Customer) = writableDatabase.insert("customers", null, android.content.ContentValues().apply { put("name", c.name); put("gstin", c.gstin); put("address", c.address); put("state", c.state); put("state_code", c.stateCode); put("phone", c.phone); put("email", c.email) })
    fun customers(): List<Customer> = readableDatabase.rawQuery("SELECT * FROM customers ORDER BY name", null).use { cur -> buildList { while (cur.moveToNext()) add(Customer(cur.getLong(0), cur.getString(1), cur.getString(2) ?: "", cur.getString(3) ?: "", cur.getString(4) ?: "", cur.getString(5) ?: "", cur.getString(6) ?: "", cur.getString(7) ?: "")) } }
    fun addItem(i: Item) = writableDatabase.insert("items", null, android.content.ContentValues().apply { put("name", i.name); put("description", i.description); put("hsn", i.hsn); put("unit", i.unit); put("gst", i.gst); put("rate", i.defaultRate) })
    fun items(): List<Item> = readableDatabase.rawQuery("SELECT * FROM items ORDER BY name", null).use { cur -> buildList { while (cur.moveToNext()) add(Item(cur.getLong(0), cur.getString(1), cur.getString(2) ?: "", cur.getString(3) ?: "", cur.getString(4) ?: "Nos", cur.getDouble(5), cur.getDouble(6))) } }
    fun saveBill(type: String, number: String, date: String, customerId: Long, taxMode: String, lines: List<BillLine>): Long {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val id = db.insert("bills", null, android.content.ContentValues().apply { put("type", type); put("number", number); put("date", date); put("customer_id", customerId); put("tax_mode", taxMode); put("created_at", System.currentTimeMillis()) })
            lines.forEach { l -> db.insert("bill_lines", null, android.content.ContentValues().apply { put("bill_id", id); put("item_id", l.item.id); put("description", l.description); put("qty", l.qty); put("rate", l.rate); put("discount", l.discount) }) }
            db.setTransactionSuccessful()
            id
        } finally { db.endTransaction() }
    }
    fun recentBills(): List<Array<String>> = readableDatabase.rawQuery("SELECT b.number,b.type,b.date,c.name,(SELECT COALESCE(SUM((qty*rate)-discount),0) FROM bill_lines bl WHERE bl.bill_id=b.id) FROM bills b LEFT JOIN customers c ON c.id=b.customer_id ORDER BY b.id DESC LIMIT 100", null).use { cur -> buildList { while (cur.moveToNext()) add(arrayOf(cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3) ?: "", String.format(java.util.Locale.US, "%.2f", cur.getDouble(4)))) } }
}
