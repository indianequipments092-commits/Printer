package com.indianequipments.usbscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LibraryActivity : Activity() {
    private lateinit var library: ScanLibrary
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private val selected = linkedSetOf<File>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        library = ScanLibrary(this)
        build()
        refresh()
    }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,24); setBackgroundColor(Color.rgb(246,248,252)) }
        val title = TextView(this).apply { text = "ALL SCANS"; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(18,30,46)) }
        root.addView(title)
        count = TextView(this).apply { textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,6,0,12) }
        root.addView(count)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(button("Select All") { selectAll() }, LinearLayout.LayoutParams(0,-2,1f))
        actions.addView(button("Clear") { selected.clear(); refresh() }, LinearLayout.LayoutParams(0,-2,1f))
        actions.addView(button("PDF") { exportPdf() }, LinearLayout.LayoutParams(0,-2,1f))
        actions.addView(button("Share") { shareSelected() }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(actions)
        val hint = TextView(this).apply { text = "Select pages to export. Each scan stays available after app restart."; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,10,0,12) }
        root.addView(hint)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun refresh() {
        list.removeAllViews(); val files = library.list(); count.text = "${files.size} saved scan page(s)  •  ${selected.size} selected"
        files.forEach { file ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8,10,8,10); setBackgroundColor(Color.WHITE) }
            val cb = CheckBox(this).apply { isChecked = selected.contains(file); setOnCheckedChangeListener { _, checked -> if (checked) selected.add(file) else selected.remove(file); count.text = "${files.size} saved scan page(s)  •  ${selected.size} selected" } }
            val info = TextView(this).apply { text = "${file.name}\n${file.length()/1024} KB"; textSize = 14f; setTextColor(Color.rgb(35,45,60)) }
            val share = button("↗") { shareFiles(listOf(file)) }
            row.addView(cb); row.addView(info, LinearLayout.LayoutParams(0,-2,1f)); row.addView(share)
            list.addView(row, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,8) })
        }
    }

    private fun selectAll() { selected.clear(); selected.addAll(library.list()); refresh() }

    private fun exportPdf() {
        val files = selected.toList(); if (files.isEmpty()) { toast("Select at least one page"); return }
        val bitmaps = files.mapNotNull { library.decode(it) }; if (bitmaps.isEmpty()) { toast("No readable pages"); return }
        val dpis = List(bitmaps.size) { 300 }
        try { val pdf = ScanDocument(this).savePdf(bitmaps, dpis, "document_${System.currentTimeMillis()}.pdf"); shareFiles(listOf(pdf), "application/pdf") }
        catch (t: Throwable) { toast("PDF export failed: ${t.message ?: "Unknown error"}") }
        finally { bitmaps.forEach { if (!it.isRecycled) it.recycle() } }
    }

    private fun shareSelected() {
        val files = selected.toList(); if (files.isEmpty()) { toast("Select files first"); return }; shareFiles(files)
    }

    private fun shareFiles(files: List<File>, mime: String = "image/jpeg") {
        try {
            val uris = ArrayList<Uri>()
            files.forEach { uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", it)) }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = if (files.size == 1) mime else "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(intent, "Share scans"))
        } catch (t: Throwable) { toast("Share failed: ${t.message ?: "No compatible app"}") }
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 12f; isAllCaps = false; setOnClickListener { action() } }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
