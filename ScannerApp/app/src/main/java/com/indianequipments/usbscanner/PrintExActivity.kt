package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PrintExActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var name: TextView
    private lateinit var pages: TextView
    private lateinit var status: TextView

    private var file: File? = null
    private var mime = "application/pdf"
    private var page = 0
    private var count = 0
    private var bitmap: Bitmap? = null

    private var copies = 1
    private var pageSelection = "All"
    private var multiPage = "Singly"
    private var duplex = "One side only"
    private var scaling = "Scale to fit"
    private var orientation = "Auto"
    private var margins = "By printable area"
    private var position = "Center"
    private var paper = "A4"
    private var outputMode = "Normal"
    private var reverseOrder = false
    private var brightness = 0
    private var contrast = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.setWindowAnimations(0)
        buildUi()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun buildUi() {
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(38), dp(18), dp(16)) }
        scroll.addView(root)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        shell.addView(navBar(), LinearLayout.LayoutParams(-1, dp(78)))
        setContentView(shell)

        val header = row()
        header.addView(txt("PRINTEX", 28f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(txt("⋮", 28f, MUTED, false).apply { setOnClickListener { showMenu() } })
        root.addView(header)
        root.addView(txt("PROFESSIONAL PRINT STUDIO", 11f, BLUE, true).apply { setPadding(0, dp(3), 0, dp(14)) })

        val statusCard = card()
        statusCard.addView(txt("PRINTER STATUS", 11f, MUTED, false))
        status = txt("", 18f, Color.WHITE, true)
        statusCard.addView(status)
        statusCard.addView(txt("Live USB printer detection • print settings apply to print job", 12f, MUTED, false))
        root.addView(statusCard, margin(0, 10))
        refreshStatus()

        root.addView(btn("▣  SELECT DOCUMENT") { pick(false) }, margin(0, 10))
        val quick = row()
        quick.addView(tile("▦", "Library") { pick(false) }, weight())
        quick.addView(tile("▣", "Files") { pick(false) }, weight())
        quick.addView(tile("▧", "Gallery") { pick(true) }, weight())
        quick.addView(tile("◎", "Scan") { openTab(MainActivity::class.java) }, weight())
        root.addView(quick, margin(0, 12))

        root.addView(section("DOCUMENT PREVIEW"))
        val previewCard = card()
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(18, 24, 34))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { openViewer() }
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(-1, dp(310)))
        val pageRow = row()
        pageRow.addView(sbtn("‹") { showPage(page - 1) }, LinearLayout.LayoutParams(dp(52), dp(42)))
        pages = txt("Page 0 / 0", 12f, MUTED, false).apply { gravity = Gravity.CENTER }
        pageRow.addView(pages, LinearLayout.LayoutParams(0, dp(42), 1f))
        pageRow.addView(sbtn("›") { showPage(page + 1) }, LinearLayout.LayoutParams(dp(52), dp(42)))
        previewCard.addView(pageRow)
        name = txt("No document selected", 16f, Color.WHITE, true)
        previewCard.addView(name)
        previewCard.addView(txt("Touch to open • pinch to zoom • drag to inspect", 11f, MUTED, false))
        root.addView(previewCard, margin(0, 10))

        root.addView(section("PRINT SETTINGS"))
        val settings = card()
        addSetting(settings, "Copies", copies.toString()) { copiesDialog() }
        addSetting(settings, "Pages", pageSelection) { choose("Pages", arrayOf("All", "Odd", "Even", "Selected", "Range"), pageSelection) { pageSelection = it; refreshUi() } }
        addSetting(settings, "Multi-Page Printing", multiPage) { choose("Multi-Page Printing", arrayOf("Singly", "Multiple", "Poster", "Booklet"), multiPage) { multiPage = it; refreshUi() } }
        addSetting(settings, "Double-Sided Printing", duplex) { choose("Double-Sided Printing", arrayOf("One side only", "Both sides", "Manually"), duplex) { duplex = it; refreshUi() } }
        addSetting(settings, "Size and Layout Options", "$scaling | $margins | $position") { showSizeLayout() }
        addSetting(settings, "Paper and Printing Options", "$paper | $outputMode") { showPaperPrinting() }
        addSetting(settings, "Default", "Reset Print Settings") { resetPrintSettings() }
        addSetting(settings, "Reverse Order", if (reverseOrder) "On" else "Off") { reverseOrder = !reverseOrder; refreshUi() }
        root.addView(settings, margin(0, 10))

        root.addView(section("IMAGE SETTINGS"))
        val imageSettings = card()
        addSetting(imageSettings, "Brightness", brightness.toString()) { slider("Brightness", -100, 100, brightness) { brightness = it; refreshUi() } }
        addSetting(imageSettings, "Contrast", "$contrast%") { slider("Contrast", 50, 200, contrast) { contrast = it; refreshUi() } }
        root.addView(imageSettings, margin(0, 10))

        root.addView(btn("▣  PRINT") { printDocument() }, margin(0, 8))
    }

    private fun refreshUi() {
        val f = file
        buildUi()
        if (f != null && f.exists()) { file = f; loadDocument() }
    }

    private fun txt(s: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(null, 1) }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = rounded(PANEL, 18) }
    private fun section(s: String) = txt(s, 11f, MUTED, true).apply { setPadding(dp(2), dp(4), 0, dp(7)) }
    private fun btn(s: String, action: () -> Unit) = Button(this).apply { text = s; textSize = 13f; setTypeface(null, 1); setTextColor(Color.WHITE); background = rounded(Color.rgb(34, 94, 215), 18); setOnClickListener { action() } }
    private fun sbtn(s: String, action: () -> Unit) = Button(this).apply { text = s; textSize = 20f; setTextColor(Color.WHITE); background = rounded(PANEL, 14); setOnClickListener { action() } }
    private fun tile(icon: String, label: String, action: () -> Unit) = Button(this).apply { text = "$icon\n$label"; textSize = 10f; setTextColor(Color.WHITE); background = rounded(PANEL, 14); setOnClickListener { action() } }
    private fun weight() = LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
    private fun margin(t: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(t), 0, dp(b)) }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(28, 38, 52)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun addSetting(parent: LinearLayout, label: String, value: String, action: () -> Unit) {
        val item = row().apply { setPadding(0, dp(6), 0, dp(6)); setOnClickListener { action() } }
        item.addView(txt(label, 13f, Color.rgb(205, 215, 230), false), LinearLayout.LayoutParams(0, dp(50), 1f))
        item.addView(txt("$value  ›", 11f, Color.WHITE, false).apply { gravity = Gravity.CENTER_VERTICAL })
        parent.addView(item)
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(28, 38, 52)) }, LinearLayout.LayoutParams(-1, 1))
    }

    private fun choose(title: String, options: Array<String>, selected: String, done: (String) -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options, options.indexOf(selected).coerceAtLeast(0)) { d, w -> done(options[w]); d.dismiss() }.setNegativeButton("CANCEL", null).show()
    }

    private fun showSizeLayout() {
        val labels = arrayOf("Page Scaling", "Paper Orientation", "Margins", "Position")
        val values = arrayOf(scaling, orientation, margins, position)
        AlertDialog.Builder(this).setTitle("Size and Layout Options").setItems(labels.mapIndexed { i, s -> "$s\n${values[i]}" }.toTypedArray()) { _, w ->
            when (w) {
                0 -> choose("Page Scaling", arrayOf("Actual size", "Scale to fit", "Shrink to fit", "Custom scaling"), scaling) { scaling = it; refreshUi() }
                1 -> choose("Paper Orientation", arrayOf("Auto", "Portrait", "Landscape"), orientation) { orientation = it; refreshUi() }
                2 -> choose("Margins", arrayOf("No margins", "By printable area", "Narrow margins", "Normal margins", "Wide margins"), margins) { margins = it; refreshUi() }
                3 -> choose("Position", arrayOf("Center", "Top left", "Top middle", "Top right", "Middle left", "Middle right", "Bottom left", "Bottom middle", "Bottom right"), position) { position = it; refreshUi() }
            }
        }.setNegativeButton("CLOSE", null).show()
    }

    private fun showPaperPrinting() {
        AlertDialog.Builder(this).setTitle("Paper and Printing Options").setItems(arrayOf("Paper Size\n$paper", "Output Mode\n$outputMode")) { _, w ->
            if (w == 0) choose("Paper Size", arrayOf("A3", "A4", "B4", "L, 3.5x5 inch", "Ledger", "Legal", "Letter", "Photo, 4x6 inch"), paper) { paper = it; refreshUi() }
            else choose("Output Mode", arrayOf("Draft", "Normal", "High"), outputMode) { outputMode = it; refreshUi() }
        }.setNegativeButton("CLOSE", null).show()
    }

    private fun resetPrintSettings() {
        copies = 1; pageSelection = "All"; multiPage = "Singly"; duplex = "One side only"; scaling = "Scale to fit"; orientation = "Auto"; margins = "By printable area"; position = "Center"; paper = "A4"; outputMode = "Normal"; reverseOrder = false
        refreshUi(); Toast.makeText(this, "Print settings restored to default", Toast.LENGTH_SHORT).show()
    }

    private fun copiesDialog() {
        val input = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(copies.toString()); selectAll() }
        AlertDialog.Builder(this).setTitle("Copies").setView(input).setPositiveButton("OK") { _, _ -> copies = (input.text.toString().toIntOrNull() ?: 1).coerceIn(1, 999); refreshUi() }.setNegativeButton("CANCEL", null).show()
    }

    private fun slider(title: String, minValue: Int, maxValue: Int, current: Int, done: (Int) -> Unit) {
        val bar = SeekBar(this).apply { min = minValue; max = maxValue; progress = current }
        AlertDialog.Builder(this).setTitle(title).setView(bar).setPositiveButton("OK") { _, _ -> done(bar.progress) }.setNegativeButton("CANCEL", null).show()
    }

    private fun pick(imageOnly: Boolean) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = if (imageOnly) "image/*" else "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")); addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        startActivityForResult(i, REQ_DOC); overridePendingTransition(0, 0)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DOC && resultCode == RESULT_OK) data?.data?.let { importDocument(it) }
        overridePendingTransition(0, 0)
    }

    private fun handleIntent(i: Intent?) { (i?.data ?: i?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { importDocument(it) } }

    private fun importDocument(uri: Uri) {
        val type = contentResolver.getType(uri) ?: "application/octet-stream"
        mime = if (type.contains("pdf", true) || uri.toString().contains(".pdf", true)) "application/pdf" else type
        val ext = if (mime == "application/pdf") "pdf" else "img"
        val dest = File(cacheDir, "printex_${System.currentTimeMillis()}.$ext")
        try {
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } } ?: error("Cannot read document")
            file?.delete(); file = dest; page = 0; name.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected document"; loadDocument()
        } catch (e: Throwable) { Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun loadDocument() {
        bitmap?.let { if (!it.isRecycled) it.recycle() }; bitmap = null
        val f = file ?: return
        if (mime == "application/pdf") {
            try { ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd -> PdfRenderer(pfd).use { count = it.pageCount } }; showPage(0) } catch (_: Throwable) { count = 0; pages.text = "Page 0 / 0" }
        } else {
            bitmap = BitmapFactory.decodeFile(f.absolutePath); count = if (bitmap != null) 1 else 0; preview.setImageBitmap(bitmap); pages.text = if (count == 1) "Page 1 / 1" else "Page 0 / 0"
        }
    }

    private fun showPage(index: Int) {
        if (file == null || count == 0) return
        page = index.coerceIn(0, count - 1)
        try {
            if (mime == "application/pdf") { bitmap?.let { if (!it.isRecycled) it.recycle() }; bitmap = renderPdfPage(file!!, page, 2.5f); preview.setImageBitmap(bitmap) } else preview.setImageBitmap(bitmap)
            pages.text = "Page ${page + 1} / $count"
        } catch (_: Throwable) {}
    }

    private fun renderPdfPage(f: File, pageIndex: Int, scale: Float): Bitmap {
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd -> PdfRenderer(pfd).use { renderer -> renderer.openPage(pageIndex).use { p ->
            val w = max(1200, (p.width * scale).roundToInt()); val h = max(1600, (p.height * scale).roundToInt())
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); b.eraseColor(Color.WHITE); p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); return b
        } } }
    }

    private fun openViewer() {
        val f = file ?: run { Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show(); return }
        val viewer = if (mime == "application/pdf") PdfZoomView(this, f, page) else ImageZoomView(this, bitmap)
        AlertDialog.Builder(this).setView(viewer).setNegativeButton("CLOSE", null).show()
    }

    private fun printDocument() {
        val f = file ?: run { Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show(); return }
        val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?, new: PrintAttributes?, signal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?) {
                callback?.onLayoutFinished(PrintDocumentInfo.Builder(name.text.toString()).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(if (count > 0) count else 1).build(), true)
            }
            override fun onWrite(ranges: Array<out PageRange>?, destination: ParcelFileDescriptor?, signal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
                try {
                    val output = destination?.fileDescriptor ?: error("No print destination")
                    FileOutputStream(output).use { out ->
                        if (mime == "application/pdf") f.inputStream().use { it.copyTo(out) } else createImagePdf(f).inputStream().use { it.copyTo(out) }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Throwable) { callback?.onWriteFailed(e.message) }
            }
        }
        val size = if (paper == "Letter") PrintAttributes.MediaSize.NA_LETTER else PrintAttributes.MediaSize.ISO_A4
        manager.print("USB Scanner - ${name.text}", adapter, PrintAttributes.Builder().setMediaSize(size).build())
    }

    private fun createImagePdf(f: File): File {
        val out = File(cacheDir, "print_${System.currentTimeMillis()}.pdf"); val doc = PdfDocument(); val b = BitmapFactory.decodeFile(f.absolutePath) ?: error("Image unavailable")
        val p = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); p.canvas.drawBitmap(b, null, RectF(20f, 20f, 575f, 822f), Paint(Paint.ANTI_ALIAS_FLAG)); doc.finishPage(p); FileOutputStream(out).use { doc.writeTo(it) }; doc.close(); b.recycle(); return out
    }

    private fun openTab(target: Class<*>) { startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); overridePendingTransition(0, 0) }

    private fun navBar(): View {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)); background = rounded(PANEL, 22) }
        navItem(bar, "⌂", "Home", MainActivity::class.java, false); navItem(bar, "◉", "Studio", MainActivity::class.java, false); navItem(bar, "▦", "Library", LibraryActivity::class.java, false); navItem(bar, "▣", "Printex", PrintExActivity::class.java, true); return bar
    }

    private fun navItem(bar: LinearLayout, icon: String, label: String, target: Class<*>, active: Boolean) {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { if (!active) openTab(target) } }
        item.addView(txt(icon, 20f, if (active) BLUE else MUTED, false)); item.addView(txt(label, 10f, if (active) BLUE else MUTED, active)); bar.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val usb = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val device = usb.deviceList.values.firstOrNull { it.interfaceCount > 0 }
        status.text = if (device != null) "${device.productName ?: "USB Printer"} Connected" else "Printer is not connected"
        status.setTextColor(if (device != null) Color.rgb(80, 210, 130) else Color.rgb(255, 170, 80))
    }

    private fun showMenu() { AlertDialog.Builder(this).setTitle("Printex").setItems(arrayOf("Refresh printer", "Print settings", "Image settings")) { _, which -> if (which == 0) refreshStatus() }.show() }

    private class ImageZoomView(context: Context, image: Bitmap?) : View(context) {
        private var bitmap = image; private var zoom = 1f; private var x = 0f; private var y = 0f; private var lx = 0f; private var ly = 0f; private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() { override fun onScale(d: ScaleGestureDetector): Boolean { zoom = (zoom * d.scaleFactor).coerceIn(1f, 8f); invalidate(); return true } })
        override fun onDraw(c: Canvas) { val b = bitmap ?: return; val s = min(width.toFloat()/b.width, height.toFloat()/b.height)*zoom; val l=(width-b.width*s)/2+x; val t=(height-b.height*s)/2+y; c.drawBitmap(b,null,RectF(l,t,l+b.width*s,t+b.height*s),paint) }
        override fun onTouchEvent(e: MotionEvent): Boolean { detector.onTouchEvent(e); when(e.actionMasked){MotionEvent.ACTION_DOWN->{lx=e.x;ly=e.y};MotionEvent.ACTION_MOVE->if(e.pointerCount==1){x+=e.x-lx;y+=e.y-ly;lx=e.x;ly=e.y;invalidate()}};return true }
    }

    private class PdfZoomView(context: Context, private val file: File, private val pageIndex: Int) : View(context) {
        private var descriptor: ParcelFileDescriptor? = null; private var renderer: PdfRenderer? = null; private var page: PdfRenderer.Page? = null; private var bitmap: Bitmap? = null; private var zoom=1f; private var x=0f; private var y=0f; private var lx=0f; private var ly=0f
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val detector=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(d:ScaleGestureDetector):Boolean{zoom=(zoom*d.scaleFactor).coerceIn(1f,10f);invalidate();return true}})
        init { try { descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY); renderer=PdfRenderer(descriptor!!); page=renderer!!.openPage(pageIndex); bitmap=Bitmap.createBitmap(max(1600,page!!.width*2),max(2200,page!!.height*2),Bitmap.Config.ARGB_8888); bitmap!!.eraseColor(Color.WHITE); page!!.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } catch(_:Throwable){} }
        override fun onDraw(c:Canvas){val b=bitmap?:return;val s=min(width.toFloat()/b.width,height.toFloat()/b.height)*zoom;val l=(width-b.width*s)/2+x;val t=(height-b.height*s)/2+y;c.drawBitmap(b,null,RectF(l,t,l+b.width*s,t+b.height*s),paint)}
        override fun onTouchEvent(e:MotionEvent):Boolean{detector.onTouchEvent(e);when(e.actionMasked){MotionEvent.ACTION_DOWN->{lx=e.x;ly=e.y};MotionEvent.ACTION_MOVE->if(e.pointerCount==1){x+=e.x-lx;y+=e.y-ly;lx=e.x;ly=e.y;invalidate()}};return true}
        override fun onDetachedFromWindow(){page?.close();renderer?.close();descriptor?.close();bitmap?.recycle();super.onDetachedFromWindow()}
    }

    companion object { private const val REQ_DOC=501; private val BG=Color.rgb(6,11,20); private val PANEL=Color.rgb(15,23,34); private val MUTED=Color.rgb(150,165,185); private val BLUE=Color.rgb(100,155,235) }
}
