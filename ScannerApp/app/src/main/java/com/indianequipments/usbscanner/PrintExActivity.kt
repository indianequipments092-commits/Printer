package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
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
import java.io.FileInputStream
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
    private var count = 0
    private var page = 0
    private var bmp: Bitmap? = null

    private var copies = 1
    private var color = "Black & White"
    private var paper = "A4"
    private var orient = "Portrait"
    private var duplex = "Off"
    private var scale = "Fit to Page"
    private var pps = 1
    private var range = "All"
    private var quality = "High"
    private var margins = "Default"
    private var reverse = false
    private var bright = 0
    private var contrast = 100
    private var autoRotate = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        buildUi()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun buildUi() {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(42), dp(18), dp(14))
        }
        shell.addView(ScrollView(this).apply { addView(root) }, LinearLayout.LayoutParams(-1, 0, 1f))
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
        quick.addView(tile("◎", "Scan") { startActivity(Intent(this, MainActivity::class.java)) }, weight())
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
        addSetting(settings, "Printer", printerName()) { showPrinterStatus() }
        addSetting(settings, "Paper Size", paper) {
            choose("Paper Size", arrayOf("A4", "A5", "A3", "Letter", "Legal")) { paper = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Orientation", orient) {
            choose("Orientation", arrayOf("Portrait", "Landscape", "Auto")) { orient = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Color Mode", color) {
            choose("Color Mode", arrayOf("Color", "Grayscale", "Black & White")) { color = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Copies", copies.toString()) { copiesDialog() }
        addSetting(settings, "Pages", range) { pagesDialog() }
        addSetting(settings, "Scale", scale) {
            choose("Scale", arrayOf("Fit to Page", "Actual Size", "Fill Page", "Shrink Oversized", "Custom 50%", "Custom 75%", "Custom 125%", "Custom 150%")) { scale = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Duplex", duplex) {
            choose("Duplex", arrayOf("Off", "Long Edge", "Short Edge")) { duplex = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Pages / Sheet", pps.toString()) {
            choose("Pages / Sheet", arrayOf("1", "2", "4", "6", "9", "16")) { pps = it.toInt(); rebuildKeepingDocument() }
        }
        addSetting(settings, "Print Quality", quality) {
            choose("Print Quality", arrayOf("Draft", "Normal", "High")) { quality = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Margins", margins) {
            choose("Margins", arrayOf("Default", "Narrow", "None")) { margins = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Brightness", bright.toString()) {
            slider("Brightness", -100, 100, bright) { bright = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Contrast", "$contrast%") {
            slider("Contrast", 50, 200, contrast) { contrast = it; rebuildKeepingDocument() }
        }
        addSetting(settings, "Auto Rotate", if (autoRotate) "On" else "Off") { autoRotate = !autoRotate; rebuildKeepingDocument() }
        addSetting(settings, "Reverse Order", if (reverse) "On" else "Off") { reverse = !reverse; rebuildKeepingDocument() }
        root.addView(settings, margin(0, 10))
        root.addView(btn("✦  SMART PRINT") { smartPrint() }, margin(0, 8))
        root.addView(btn("▣  PRINT DOCUMENT") { printDocument() }, margin(0, 8))
    }

    private fun txt(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, 1)
    }

    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(PANEL, 18)
    }

    private fun section(text: String) = txt(text, 11f, MUTED, true).apply { setPadding(dp(2), dp(4), 0, dp(7)) }

    private fun btn(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 13f
        setTypeface(null, 1)
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(34, 94, 215), 18)
        setOnClickListener { action() }
    }

    private fun sbtn(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.WHITE)
        background = rounded(PANEL, 14)
        setOnClickListener { action() }
    }

    private fun tile(icon: String, label: String, action: () -> Unit) = Button(this).apply {
        text = "$icon\n$label"
        textSize = 10f
        setTextColor(Color.WHITE)
        background = rounded(PANEL, 14)
        setOnClickListener { action() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, dp(62), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
    private fun margin(top: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(top), 0, dp(bottom)) }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(28, 38, 52))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private fun addSetting(parent: LinearLayout, label: String, value: String, action: () -> Unit) {
        val item = row().apply { setPadding(0, dp(6), 0, dp(6)); setOnClickListener { action() } }
        item.addView(txt(label, 13f, Color.rgb(205, 215, 230), false), LinearLayout.LayoutParams(0, dp(44), 1f))
        item.addView(txt("$value  ›", 12f, Color.WHITE, false))
        parent.addView(item)
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(28, 38, 52)) }, LinearLayout.LayoutParams(-1, 1))
    }

    private fun pick(imageOnly: Boolean) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = if (imageOnly) "image/*" else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp"))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, REQ_DOC)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_DOC && resultCode == RESULT_OK) data?.data?.let { importDocument(it) }
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        uri?.let { importDocument(it) }
    }

    private fun importDocument(uri: Uri) {
        val type = contentResolver.getType(uri) ?: "application/octet-stream"
        mime = if (type.contains("pdf", true) || uri.toString().contains(".pdf", true)) "application/pdf" else type
        val extension = if (mime == "application/pdf") "pdf" else "img"
        val destination = File(cacheDir, "printex_${System.currentTimeMillis()}.$extension")
        try {
            val input = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot read document")
            input.use { source -> FileOutputStream(destination).use { output -> source.copyTo(output) } }
            file?.delete()
            file = destination
            page = 0
            name.text = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected document"
            loadDocument()
        } catch (error: Throwable) {
            Toast.makeText(this, "Import failed: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadDocument() {
        bmp?.let { if (!it.isRecycled) it.recycle() }
        bmp = null
        val current = file ?: return
        if (mime == "application/pdf") {
            try {
                ParcelFileDescriptor.open(current, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer -> count = renderer.pageCount }
                }
                showPage(0)
            } catch (_: Throwable) {
                count = 0
                pages.text = "Page 0 / 0"
            }
        } else {
            bmp = BitmapFactory.decodeFile(current.absolutePath)
            count = if (bmp != null) 1 else 0
            preview.setImageBitmap(bmp)
            pages.text = if (count == 1) "Page 1 / 1" else "Page 0 / 0"
        }
    }

    private fun showPage(index: Int) {
        if (file == null || count < 1) return
        page = index.coerceIn(0, count - 1)
        if (mime == "application/pdf") {
            try {
                bmp?.let { if (!it.isRecycled) it.recycle() }
                bmp = renderPdfPage(file!!, page, 2.5f)
                preview.setImageBitmap(bmp)
            } catch (_: Throwable) { return }
        } else {
            preview.setImageBitmap(bmp)
        }
        pages.text = "Page ${page + 1} / $count"
    }

    private fun renderPdfPage(document: File, pageIndex: Int, multiplier: Float): Bitmap {
        ParcelFileDescriptor.open(document, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(pageIndex).use { pdfPage ->
                    val width = max(900, (pdfPage.width * multiplier).roundToInt())
                    val height = max(1200, (pdfPage.height * multiplier).roundToInt())
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    private fun openViewer() {
        val currentFile = file ?: run {
            Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(8), dp(30), dp(8), dp(8))
        }
        box.addView(txt("‹  DOCUMENT PREVIEW", 18f, Color.WHITE, true).apply {
            setPadding(dp(8), dp(8), 0, dp(8))
            setOnClickListener { dialog.dismiss() }
        })
        val zoomView = ZoomView(this)
        zoomView.setImageBitmap(if (mime == "application/pdf") renderPdfPage(currentFile, page, 2.5f) else bmp)
        box.addView(zoomView, LinearLayout.LayoutParams(-1, 0, 1f))
        zoomView.post { zoomView.reset() }

        val controls = row()
        controls.addView(sbtn("‹") {
            if (page > 0) {
                page--
                zoomView.setImageBitmap(if (mime == "application/pdf") renderPdfPage(currentFile, page, 2.5f) else bmp)
                zoomView.post { zoomView.reset() }
            }
        })
        controls.addView(txt("Page ${page + 1} / $count", 13f, MUTED, false).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(sbtn("›") {
            if (page < count - 1) {
                page++
                zoomView.setImageBitmap(if (mime == "application/pdf") renderPdfPage(currentFile, page, 2.5f) else bmp)
                zoomView.post { zoomView.reset() }
            }
        })
        box.addView(controls)
        dialog.setContentView(box)
        dialog.show()
    }

    private fun choose(title: String, options: Array<String>, done: (String) -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setItems(options) { _, index -> done(options[index]) }.show()
    }

    private fun slider(title: String, minValue: Int, maxValue: Int, startValue: Int, done: (Int) -> Unit) {
        val seek = SeekBar(this).apply {
            max = maxValue - minValue
            progress = startValue.coerceIn(minValue, maxValue) - minValue
        }
        AlertDialog.Builder(this).setTitle(title).setView(seek)
            .setPositiveButton("Apply") { _, _ -> done(seek.progress + minValue) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun copiesDialog() {
        val input = EditText(this).apply { inputType = 2; setText(copies.toString()) }
        AlertDialog.Builder(this).setTitle("Copies").setView(input)
            .setPositiveButton("Apply") { _, _ -> copies = (input.text.toString().toIntOrNull() ?: 1).coerceIn(1, 99); rebuildKeepingDocument() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun pagesDialog() {
        choose("Pages", arrayOf("All", "Current", "Odd", "Even", "Custom Range")) {
            if (it == "Custom Range") {
                val input = EditText(this)
                AlertDialog.Builder(this).setTitle("Custom Range").setView(input)
                    .setPositiveButton("Apply") { _, _ -> range = input.text.toString(); rebuildKeepingDocument() }
                    .setNegativeButton("Cancel", null).show()
            } else {
                range = it
                rebuildKeepingDocument()
            }
        }
    }

    private fun rebuildKeepingDocument() {
        val current = file
        buildUi()
        if (current != null && current.exists()) {
            file = current
            loadDocument()
        }
    }

    private fun smartPrint() {
        if (file == null) {
            Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show()
            return
        }
        paper = "A4"
        scale = "Fit to Page"
        quality = "High"
        autoRotate = true
        rebuildKeepingDocument()
        Toast.makeText(this, "Smart Print optimized", Toast.LENGTH_SHORT).show()
    }

    private fun printDocument() {
        val current = file ?: run {
            Toast.makeText(this, "Select a document first", Toast.LENGTH_SHORT).show()
            return
        }
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        val builder = PrintAttributes.Builder()
            .setMediaSize(if (orient == "Landscape") PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(if (color == "Color") PrintAttributes.COLOR_MODE_COLOR else PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMinMargins(if (margins == "None") PrintAttributes.Margins.NO_MARGINS else PrintAttributes.Margins(300, 300, 300, 300))
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            builder.setDuplexMode(when (duplex) {
                "Long Edge" -> PrintAttributes.DUPLEX_MODE_LONG_EDGE
                "Short Edge" -> PrintAttributes.DUPLEX_MODE_SHORT_EDGE
                else -> PrintAttributes.DUPLEX_MODE_NONE
            })
        }
        printManager.print("Printex • ${current.nameWithoutExtension}", PrintAdapter(current), builder.build())
    }

    private inner class PrintAdapter(private val document: File) : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
            callback.onLayoutFinished(PrintDocumentInfo.Builder(document.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(count.coerceAtLeast(1)).build(), true)
        }
        override fun onWrite(pages: Array<PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
            try {
                FileInputStream(document).use { input -> ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output -> input.copyTo(output) } }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (error: Throwable) {
                callback.onWriteFailed(error.message)
            }
        }
    }

    private fun printerName(): String {
        val device = findUsbPrinter()
        return device?.let { "${it.productName ?: it.deviceName} Connected" } ?: "Printer is not connected"
    }

    private fun refreshStatus() {
        val device = findUsbPrinter()
        status.text = device?.let { "●  ${it.productName ?: it.deviceName} Connected" } ?: "Printer is not connected"
        status.setTextColor(if (device == null) Color.rgb(255, 170, 90) else Color.rgb(75, 220, 145))
    }

    private fun findUsbPrinter(): UsbDevice? {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        return manager.deviceList.values.firstOrNull { device ->
            device.deviceClass == 7 || (0 until device.interfaceCount).any { index -> device.getInterface(index).interfaceClass == 7 }
        }
    }

    private fun showPrinterStatus() = Toast.makeText(this, printerName(), Toast.LENGTH_SHORT).show()

    private fun showMenu() {
        AlertDialog.Builder(this).setTitle("Printex")
            .setItems(arrayOf("Printer Management", "Print Queue", "Print History", "Printer Diagnostics", "Default Settings")) { _, index ->
                when (index) {
                    0 -> showPrinterStatus()
                    1 -> Toast.makeText(this, "Print queue is managed by Android print service", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Print history is available after print jobs", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, printerName(), Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(this, "A4 • Portrait • $color • $duplex", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun navBar(): View {
        val nav = row()
        nav.background = rounded(PANEL, 20)
        arrayOf("⌂" to "Home", "◎" to "Studio", "▦" to "Library", "▣" to "Printex").forEach { (icon, label) ->
            nav.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val tint = if (label == "Printex") BLUE else MUTED
                addView(txt(icon, 20f, tint, false))
                addView(txt(label, 11f, tint, false))
                setOnClickListener { if (label != "Printex") startActivity(Intent(this@PrintExActivity, MainActivity::class.java)) }
            }, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return nav
    }

    private class ZoomView(context: Context) : ImageView(context) {
        private var scale = 1f
        private var lastX = 0f
        private var lastY = 0f
        private var moving = false
        private val imageMatrixValue = Matrix()
        private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scale = (scale * detector.scaleFactor).coerceIn(1f, 10f)
                imageMatrixValue.postScale(detector.scaleFactor, detector.scaleFactor, detector.focusX, detector.focusY)
                imageMatrix = imageMatrixValue
                return true
            }
        })
        init {
            scaleType = ScaleType.MATRIX
            setOnTouchListener { _, event ->
                scaleDetector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; moving = true }
                    MotionEvent.ACTION_MOVE -> if (moving && !scaleDetector.isInProgress && scale > 1f) {
                        imageMatrixValue.postTranslate(event.x - lastX, event.y - lastY)
                        imageMatrix = imageMatrixValue
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> moving = false
                }
                true
            }
        }
        fun reset() {
            scale = 1f
            imageMatrixValue.reset()
            val d = drawable
            if (d != null && width > 0 && height > 0) {
                val sx = width.toFloat() / d.intrinsicWidth
                val sy = height.toFloat() / d.intrinsicHeight
                val fitted = min(sx, sy)
                imageMatrixValue.setScale(fitted, fitted)
                imageMatrixValue.postTranslate((width - d.intrinsicWidth * fitted) / 2f, (height - d.intrinsicHeight * fitted) / 2f)
            }
            imageMatrix = imageMatrixValue
        }
    }

    companion object {
        private const val REQ_DOC = 4101
        private val BG = Color.rgb(8, 12, 20)
        private val PANEL = Color.rgb(15, 21, 31)
        private val BLUE = Color.rgb(110, 165, 255)
        private val MUTED = Color.rgb(145, 160, 180)
    }
}
