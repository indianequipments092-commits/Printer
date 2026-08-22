package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class PrintExActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var fileName: TextView
    private lateinit var pageInfo: TextView
    private var sourceFile: File? = null
    private var mimeType = "application/pdf"
    private var pageCount = 1
    private var currentPage = 0
    private var previewBitmap: Bitmap? = null
    private var copies = 1
    private var colorMode = "Black & White"
    private var paperSize = "A4 (210 × 297 mm)"
    private var orientation = "Portrait"
    private var duplex = "Off"
    private var scale = "Fit to Page"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        buildUi()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(18), dp(38), dp(18), dp(14))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)

        val header = row()
        header.addView(TextView(this).apply {
            text = "PRINTEX"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply { text = "⋮"; textSize = 28f; setTextColor(MUTED) })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "PROFESSIONAL PRINT STUDIO"
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(BLUE)
            setPadding(0, dp(3), 0, dp(16))
        })

        val status = card()
        status.addView(TextView(this).apply { text = "PRINTER STATUS"; textSize = 11f; setTextColor(MUTED) })
        status.addView(TextView(this).apply {
            text = "●  Android Print System"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(75,220,145))
            setPadding(0, dp(6), 0, 0)
        })
        status.addView(TextView(this).apply {
            text = "Ready • USB / system print services"
            textSize = 12f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(status, margin(0,10))

        root.addView(actionButton("▣  SELECT DOCUMENT") { chooseDocument() }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0,0,0,dp(10)) })
        val quick = row()
        quick.addView(tile("▦", "Library") { finish() }, weight())
        quick.addView(tile("▣", "Files") { chooseDocument() }, weight())
        quick.addView(tile("▧", "Gallery") { chooseImage() }, weight())
        quick.addView(tile("◎", "Scan") { finish(); startActivity(Intent(this, MainActivity::class.java)) }, weight())
        root.addView(quick, margin(0,12))

        root.addView(section("DOCUMENT PREVIEW"))
        val pcard = card()
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(18,24,34))
            minimumHeight = dp(300)
            setPadding(dp(10),dp(10),dp(10),dp(10))
            isClickable = true
            setOnClickListener { openDocumentViewer() }
        }
        pcard.addView(preview, LinearLayout.LayoutParams(-1, dp(310)))
        val pageControls = row()
        pageControls.addView(smallButton("‹") { showPage(currentPage - 1) }, LinearLayout.LayoutParams(dp(52), dp(42)))
        pageInfo = TextView(this).apply {
            text = "Page 1 / 1"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(MUTED)
        }
        pageControls.addView(pageInfo, LinearLayout.LayoutParams(0, dp(42), 1f))
        pageControls.addView(smallButton("›") { showPage(currentPage + 1) }, LinearLayout.LayoutParams(dp(52), dp(42)))
        pcard.addView(pageControls)
        fileName = TextView(this).apply { text = "No document selected"; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0,dp(8),0,0) }
        pcard.addView(fileName)
        root.addView(pcard, margin(0,10))

        root.addView(section("PRINT SETTINGS"))
        val settings = card()
        settingRow(settings, "Printer", "System / USB printer") { printerChooser() }
        settingRow(settings, "Paper Size", paperSize) { chooseValue("Paper Size", arrayOf("A4 (210 × 297 mm)", "A5 (148 × 210 mm)", "Letter", "Legal")) { paperSize = it; rebuild() } }
        settingRow(settings, "Orientation", orientation) { chooseValue("Orientation", arrayOf("Portrait", "Landscape", "Auto")) { orientation = it; rebuild() } }
        settingRow(settings, "Color Mode", colorMode) { chooseValue("Color Mode", arrayOf("Color", "Grayscale", "Black & White")) { colorMode = it; rebuild() } }
        settingRow(settings, "Copies", copies.toString()) { chooseCopies() }
        settingRow(settings, "Pages", "All Pages (1-$pageCount)") { }
        settingRow(settings, "Scale", scale) { chooseValue("Scale", arrayOf("Fit to Page", "Actual Size", "Fill Page")) { scale = it; rebuild() } }
        settingRow(settings, "Duplex", duplex) { chooseValue("Duplex", arrayOf("Off", "Long Edge", "Short Edge")) { duplex = it; rebuild() } }
        root.addView(settings, margin(0,10))

        root.addView(actionButton("✦  SMART PRINT") { smartPrint() }, margin(0,8))
        root.addView(actionButton("▣  PRINT DOCUMENT") { printDocument() }, margin(0,8))
        root.addView(TextView(this).apply {
            text = "Local-first • Share a PDF from any app directly into Printex"
            textSize = 11f
            setTextColor(Color.rgb(115,130,150))
            gravity = Gravity.CENTER
            setPadding(0,dp(5),0,dp(8))
        })
    }

    private fun handleIntent(i: Intent?) {
        val uri = when (i?.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> i.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            Intent.ACTION_VIEW -> i.data
            else -> null
        }
        if (uri != null) importUri(uri)
    }

    private fun chooseDocument() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png", "image/jpg"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, REQ_DOC)
    }

    private fun chooseImage() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, REQ_DOC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DOC && resultCode == RESULT_OK) data?.data?.let { importUri(it) }
    }

    private fun importUri(uri: android.net.Uri) {
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {}
        val type = contentResolver.getType(uri) ?: "application/octet-stream"
        val pdf = type.contains("pdf", true) || uri.toString().lowercase().contains(".pdf")
        mimeType = if (pdf) "application/pdf" else type
        val ext = if (pdf) "pdf" else if (type.contains("png")) "png" else "jpg"
        val out = File(cacheDir, "printex_${System.currentTimeMillis()}.$ext")
        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IllegalStateException("Cannot read document")
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            sourceFile?.delete()
            sourceFile = out
            currentPage = 0
            fileName.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected document"
            loadPreview(out)
        } catch (t: Throwable) {
            Toast.makeText(this, "Import failed: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadPreview(file: File) {
        previewBitmap?.let { if (!it.isRecycled) it.recycle() }
        previewBitmap = null
        if (mimeType == "application/pdf") {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount.coerceAtLeast(1)
                    }
                }
                showPage(0)
            } catch (_: Throwable) {
                pageCount = 1
                showPage(0)
            }
        } else {
            previewBitmap = BitmapFactory.decodeFile(file.absolutePath)
            pageCount = 1
            preview.setImageBitmap(previewBitmap)
        }
        pageInfo.text = "Page ${currentPage + 1} / $pageCount"
    }

    private fun showPage(index: Int) {
        if (sourceFile == null) return
        currentPage = index.coerceIn(0, pageCount - 1)
        if (mimeType != "application/pdf") {
            preview.setImageBitmap(previewBitmap)
            pageInfo.text = "Page 1 / 1"
            return
        }
        val file = sourceFile ?: return
        try {
            previewBitmap?.let { if (!it.isRecycled) it.recycle() }
            previewBitmap = renderPdfPage(file, currentPage, 2.5f)
            preview.setImageBitmap(previewBitmap)
            pageInfo.text = "Page ${currentPage + 1} / $pageCount"
        } catch (_: Throwable) {
            pageInfo.text = "Page ${currentPage + 1} / $pageCount"
        }
    }

    private fun renderPdfPage(file: File, index: Int, scale: Float): Bitmap {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.openPage(index).use { page ->
                    val w = (page.width * scale).roundToInt().coerceAtLeast(1200)
                    val h = (page.height * scale).roundToInt().coerceAtLeast(1600)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bmp
                }
            }
        }
    }

    private fun openDocumentViewer() {
        val file = sourceFile ?: run {
            Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show()
            return
        }
        val viewer = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        viewer.window?.setBackgroundDrawableResource(android.R.color.black)
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(4,7,12))
            setPadding(dp(8), dp(28), dp(8), dp(10))
        }

        val header = row()
        header.addView(TextView(this).apply {
            text = "‹  DOCUMENT PREVIEW"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(4), 0, dp(4))
            setOnClickListener { viewer.dismiss() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        val reset = TextView(this).apply {
            text = "100%"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(18,25,36), 12)
            setPadding(dp(10),0,dp(10),0)
        }
        header.addView(reset, LinearLayout.LayoutParams(dp(64), dp(40)))

        val zoom = ZoomImageView(this)
        zoom.setBackgroundColor(Color.rgb(10,14,22))
        zoom.setImageBitmap(if (mimeType == "application/pdf") renderPdfPage(file, currentPage, 4f) else previewBitmap)
        shell.addView(zoom, LinearLayout.LayoutParams(-1, 0, 1f))
        zoom.onScaleChanged = { value -> reset.text = "${(value * 100).roundToInt()}%" }

        val controls = row().apply { setPadding(dp(4), dp(8), dp(4), dp(4)) }
        lateinit var pageIndicatorInViewer: TextView
        controls.addView(smallButton("‹") {
            if (currentPage > 0) {
                currentPage--
                pageIndicatorInViewer.text = "Page ${currentPage + 1} / $pageCount"
                zoom.setImageBitmap(if (mimeType == "application/pdf") renderPdfPage(file, currentPage, 4f) else previewBitmap)
                zoom.resetZoom()
            }
        }, LinearLayout.LayoutParams(dp(58), dp(48)))
        pageIndicatorInViewer = TextView(this).apply {
            text = "Page ${currentPage + 1} / $pageCount"
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(MUTED)
        }
        controls.addView(pageIndicatorInViewer, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(smallButton("›") {
            if (currentPage < pageCount - 1) {
                currentPage++
                pageIndicatorInViewer.text = "Page ${currentPage + 1} / $pageCount"
                zoom.setImageBitmap(if (mimeType == "application/pdf") renderPdfPage(file, currentPage, 4f) else previewBitmap)
                zoom.resetZoom()
            }
        }, LinearLayout.LayoutParams(dp(58), dp(48)))
        shell.addView(controls)

        shell.addView(TextView(this).apply {
            text = "Pinch to zoom • Drag to inspect • Tap page arrows to change pages"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(120,135,155))
            setPadding(0,0,0,dp(6))
        })

        viewer.setContentView(shell)
        viewer.show()
    }

    private fun smartPrint() {
        if (sourceFile == null) { Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show(); return }
        scale = "Fit to Page"
        paperSize = "A4 (210 × 297 mm)"
        if (mimeType == "application/pdf" && colorMode == "Color") colorMode = "Black & White"
        rebuild()
        Toast.makeText(this, "Smart Print optimized • A4 • Fit to Page", Toast.LENGTH_LONG).show()
    }

    private fun printDocument() {
        val file = sourceFile ?: run { Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show(); return }
        val pm = getSystemService(PRINT_SERVICE) as PrintManager
        val media = if (orientation == "Landscape") PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4
        val attrs = PrintAttributes.Builder().setMediaSize(media).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()
        pm.print(file.nameWithoutExtension.ifBlank { "Printex Document" }, LocalPrintAdapter(file, pageCount), attrs)
    }

    private fun printerChooser() {
        Toast.makeText(this, "Printer selection opens in the Android print system", Toast.LENGTH_SHORT).show()
    }

    private fun chooseCopies() {
        val values = (1..20).map { it.toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Copies").setItems(values) { _, which -> copies = which + 1; rebuild() }.show()
    }

    private fun chooseValue(title: String, values: Array<String>, done: (String) -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setItems(values) { _, which -> done(values[which]) }.show()
    }

    private fun rebuild() {
        val f = sourceFile
        val old = previewBitmap
        buildUi()
        if (f != null && f.exists()) {
            sourceFile = f
            previewBitmap = old
            if (mimeType == "application/pdf") showPage(currentPage) else preview.setImageBitmap(old)
            pageInfo.text = "Page ${currentPage + 1} / $pageCount"
        }
    }

    private fun settingRow(parent: LinearLayout, label: String, value: String, click: () -> Unit) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0,dp(7),0,dp(7))
            setOnClickListener { click() }
        }
        r.addView(TextView(this).apply { text = label; textSize = 13f; setTextColor(Color.rgb(205,215,230)) }, LinearLayout.LayoutParams(0,-2,1f))
        r.addView(TextView(this).apply { text = "$value  ›"; textSize = 12f; setTextColor(Color.WHITE) })
        parent.addView(r)
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(28,38,52)) }, LinearLayout.LayoutParams(-1,1))
    }

    private inner class LocalPrintAdapter(private val file: File, private val pages: Int) : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: android.os.CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages.coerceAtLeast(1)).build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }
        override fun onWrite(pageRanges: Array<android.print.PageRange>, destination: ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal, callback: WriteResultCallback) {
            try {
                if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { out -> file.inputStream().use { it.copyTo(out) } }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (t: Throwable) { callback.onWriteFailed(t.message) }
        }
    }

    private class ZoomImageView(context: android.content.Context) : ImageView(context) {
        private var scaleFactor = 1f
        private var lastX = 0f
        private var lastY = 0f
        private var detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                scaleFactor = (scaleFactor * d.scaleFactor).coerceIn(1f, 8f)
                applyTransform()
                onScaleChanged?.invoke(scaleFactor)
                return true
            }
        })
        var onScaleChanged: ((Float) -> Unit)? = null

        init {
            scaleType = ImageView.ScaleType.MATRIX
            setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (scaleFactor > 1f && event.pointerCount == 1) {
                            val dx = event.x - lastX
                            val dy = event.y - lastY
                            imageMatrix.postTranslate(dx, dy)
                            imageMatrix = imageMatrix
                            lastX = event.x
                            lastY = event.y
                        }
                    }
                }
                true
            }
        }

        fun resetZoom() {
            scaleFactor = 1f
            applyTransform()
            onScaleChanged?.invoke(1f)
        }

        private fun applyTransform() {
            val d = drawable ?: return
            val vw = width.toFloat()
            val vh = height.toFloat()
            if (vw <= 0f || vh <= 0f) return
            val dw = d.intrinsicWidth.toFloat()
            val dh = d.intrinsicHeight.toFloat()
            val base = minOf(vw / dw, vh / dh)
            val matrix = Matrix()
            val scaledW = dw * base * scaleFactor
            val scaledH = dh * base * scaleFactor
            val left = (vw - scaledW) / 2f
            val top = (vh - scaledH) / 2f
            matrix.setScale(base * scaleFactor, base * scaleFactor)
            matrix.postTranslate(left, top)
            imageMatrix = matrix
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            post { applyTransform() }
        }
    }

    private fun smallButton(text: String, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 22f
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(18,25,36), 14)
        setOnClickListener { click() }
        minHeight = 0
        minimumHeight = 0
    }

    private fun card() = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14)); background=rounded(Color.rgb(15,21,31),18) }
    private fun section(s:String)=TextView(this).apply { text=s; textSize=11f; setTypeface(null,android.graphics.Typeface.BOLD); setTextColor(Color.rgb(120,145,175)); setPadding(dp(2),dp(4),0,dp(7)) }
    private fun actionButton(s:String,a:()->Unit)=Button(this).apply { text=s; textSize=13f; setTypeface(null,android.graphics.Typeface.BOLD); setTextColor(Color.WHITE); background=rounded(Color.rgb(34,94,215),18); setOnClickListener{a()}; minHeight=0; minimumHeight=0 }
    private fun tile(i:String,s:String,a:()->Unit)=Button(this).apply { text="$i\n$s"; textSize=10f; setTextColor(Color.rgb(215,225,238)); background=rounded(Color.rgb(18,25,36),14); setOnClickListener{a()}; minHeight=0; minimumHeight=0 }
    private fun row()=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
    private fun weight()=LinearLayout.LayoutParams(0,dp(62),1f).apply { setMargins(dp(3),0,dp(3),0) }
    private fun margin(t:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(t),0,dp(b)) }
    private fun rounded(c:Int,r:Int)=android.graphics.drawable.GradientDrawable().apply { setColor(c); cornerRadius=dp(r).toFloat(); setStroke(dp(1),Color.rgb(28,38,52)) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()

    override fun onDestroy() { sourceFile?.delete(); previewBitmap?.let { if(!it.isRecycled) it.recycle() }; super.onDestroy() }

    companion object { private const val REQ_DOC=9101; private val BG=Color.rgb(8,12,20); private val BLUE=Color.rgb(110,165,255); private val MUTED=Color.rgb(145,160,180) }
}
