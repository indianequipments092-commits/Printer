package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var usb: UsbScannerManager
    private lateinit var document: ScanDocument
    private lateinit var library: ScanLibrary
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var nav: LinearLayout
    private var device: UsbDevice? = null
    private var protocol: ScannerProtocol? = null
    private val pages = mutableListOf<Bitmap>()
    private val dpis = mutableListOf<Int>()
    private val selectedLibrary = mutableSetOf<String>()
    private val executor = Executors.newSingleThreadExecutor()
    private var scanning = false
    private var previewing = false
    private var currentTab = 0
    private var currentPage = -1
    private var brightness = 0f
    private var contrast = 1f
    private var grayscale = false
    private var imagePreview: ImageView? = null
    private var pageCount: TextView? = null
    private var dpiSpinner: Spinner? = null
    private var colorSwitch: CheckBox? = null
    private var scanButton: Button? = null
    private var previewButton: Button? = null
    private var progress: ProgressBar? = null
    private var progressText: TextView? = null
    private var previewBitmap: Bitmap? = null
    private var pendingTempPdf: File? = null

    private val PDF_FOLDER_REQUEST = 7001
    private val prefs by lazy { getSharedPreferences("scanner_prefs", MODE_PRIVATE) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(8, 12, 20)
        window.navigationBarColor = Color.rgb(8, 12, 20)
        window.decorView.systemUiVisibility = 0
        document = ScanDocument(this)
        library = ScanLibrary(this)
        usb = UsbScannerManager(this)
        usb.onPermissionGranted = { connectDevice(it) }
        usb.onPermissionDenied = { runOnUiThread { setStatus("USB access denied • tap Connect to retry") } }
        buildShell()
        usb.register()
        refreshUsb()
    }

    override fun onBackPressed() {
        if (scanning || previewing) {
            setStatus("Please wait for the current scanner operation to finish")
            return
        }
        if (currentTab != 0) {
            renderTab(0)
            return
        }
        super.onBackPressed()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 12, 20))
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = panel()
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(82)))
        setContentView(root)
        renderTab(0)
    }

    private fun renderTab(tab: Int) {
        currentTab = tab
        content.removeAllViews()
        nav.removeAllViews()
        navButton("⌂", "Home", 0)
        navButton("◎", "Studio", 1)
        navButton("▦", "Library", 2)
        navButton("⚙", "Tools", 3)
        when (tab) {
            0 -> renderHome()
            1 -> renderStudio()
            2 -> renderLibrary()
            else -> renderTools()
        }
    }

    private fun renderHome() {
        content.addView(title("USB SCANNER", "MAHA ADVANCED+ • PROFESSIONAL SCAN STUDIO"))
        val deviceCard = card()
        deviceCard.addView(TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(145,160,180))
            text = "SCANNER STATUS"
        })
        status = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            text = if (device == null) "MF3010 Disconnected" else "MF3010 Connected"
            setPadding(0,dp(5),0,0)
        }
        deviceCard.addView(status)
        deviceCard.addView(TextView(this).apply {
            text = "USB • Canon MF3010 • VID 1193 / PID 10073"
            textSize = 12f
            setTextColor(Color.rgb(120,136,158))
            setPadding(0,dp(4),0,0)
        })
        deviceCard.addView(actionButton(if (device == null) "CONNECT SCANNER" else "REFRESH USB") {
            if (device == null) requestScanner() else refreshUsb()
        }, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0,dp(14),0,0) })
        content.addView(deviceCard, margin(0,10))

        val hero = card()
        hero.addView(TextView(this).apply {
            text = "SCAN STUDIO"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(130,160,200))
        })
        hero.addView(TextView(this).apply {
            text = "Turn paper into a polished digital document"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0,dp(6),0,dp(2))
        })
        hero.addView(TextView(this).apply {
            text = "Multi-page • Preview • Enhance • Edit • Export • Share"
            textSize = 12f
            setTextColor(Color.rgb(150,165,185))
        })
        hero.addView(actionButton("＋  NEW SCAN") { renderTab(1) }, LinearLayout.LayoutParams(-1, dp(54)).apply {
            setMargins(0,dp(18),0,0)
        })
        content.addView(hero, margin(0,12))

        val stats = row()
        stats.addView(statCard("${library.list().size}", "ALL SCANS"), weight(1f,5))
        stats.addView(statCard("${pages.size}", "CURRENT PAGES"), weight(1f,5))
        stats.addView(statCard("${selectedLibrary.size}", "SELECTED"), weight(1f,5))
        content.addView(stats, margin(0,12))
        content.addView(section("QUICK ACTIONS"))
        val quick = row()
        quick.addView(tile("▦", "All Scans") { renderTab(2) }, weight(1f,6))
        quick.addView(tile("✦", "Enhance") { if (pages.isNotEmpty()) { autoEnhance(); renderTab(1) } else renderTab(1) }, weight(1f,6))
        quick.addView(tile("↗", "Export") { showShareFormatDialog() }, weight(1f,6))
        content.addView(quick, margin(0,12))
        content.addView(section("RECENT DOCUMENTS"))
        val recent = library.list().take(3)
        if (recent.isEmpty()) content.addView(emptyCard("No scans yet", "Your scanned pages will appear here."), margin(0,8))
        recent.forEach { file -> content.addView(fileRow(file), margin(0,8)) }
    }

    private fun renderStudio() {
        content.addView(title("SCAN STUDIO", "LIVE CONTROL • FAST PREVIEW + HIGH QUALITY SCAN"))
        val top = card()
        top.addView(TextView(this).apply {
            text = if (device == null) "○ Scanner offline" else "● MF3010 ready"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (device == null) Color.rgb(255,170,90) else Color.rgb(85,220,160))
        })
        top.addView(TextView(this).apply {
            text = "Current document"
            textSize = 12f
            setTextColor(Color.rgb(140,155,175))
            setPadding(0,dp(10),0,0)
        })
        pageCount = TextView(this).apply {
            text = "${pages.size} pages"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        top.addView(pageCount)
        content.addView(top, margin(0,10))

        val previewCard = card()
        imagePreview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(18,24,34))
            setPadding(dp(6),dp(6),dp(6),dp(6))
            minimumHeight = dp(220)
        }
        if (currentPage >= 0 && currentPage < pages.size) imagePreview!!.setImageBitmap(processedPreview())
        else if (previewBitmap != null && !previewBitmap!!.isRecycled) imagePreview!!.setImageBitmap(previewBitmap)
        previewCard.addView(imagePreview, LinearLayout.LayoutParams(-1, dp(270)))
        content.addView(previewCard, margin(0,10))

        val settings = card()
        settings.addView(section("SCAN SETTINGS"))
        val srow = row()
        dpiSpinner = Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, listOf("150 DPI", "300 DPI", "600 DPI")) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.textSize = 18f
                    v.setPadding(dp(8),0,dp(8),0)
                    return v
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setBackgroundColor(Color.rgb(18,25,36))
                    v.textSize = 17f
                    v.setPadding(dp(12),dp(10),dp(12),dp(10))
                    return v
                }
            }
            setSelection(1)
        }
        colorSwitch = CheckBox(this).apply {
            text = "Color"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = true
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(80,150,255))
        }
        srow.addView(dpiSpinner, weight(1f,4))
        srow.addView(colorSwitch, weight(1f,4))
        settings.addView(srow)
        settings.addView(slider("Brightness", -100, 100, 0) { brightness = it / 100f; updatePreview() })
        settings.addView(slider("Contrast", 50, 200, 100) { contrast = it / 100f; updatePreview() })
        settings.addView(TextView(this).apply {
            text = "Auto Enhance • Background Cleanup • Noise Reduction • Deskew • Auto Rotation"
            textSize = 12f
            setTextColor(Color.rgb(135,150,170))
            setPadding(0,dp(8),0,0)
        })
        content.addView(settings, margin(0,10))

        val scanRow = row()
        previewButton = actionButton("⚡ PREVIEW") { previewScan() }
        previewButton!!.isEnabled = device != null && !scanning && !previewing
        scanButton = actionButton("SCAN PAGE") { scan() }
        scanButton!!.isEnabled = device != null && !scanning && !previewing
        scanRow.addView(previewButton, weight(1f,4))
        scanRow.addView(scanButton, weight(1f,4))
        content.addView(scanRow, margin(0,10))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }
        progressText = TextView(this).apply { textSize = 12f; setTextColor(Color.rgb(150,165,185)); visibility = View.GONE }
        content.addView(progress)
        content.addView(progressText)

        content.addView(section("PAGE TOOLS"), margin(0,14))
        val tools = row()
        tools.addView(tile("↻", "Rotate") { rotatePage() }, weight(1f,4))
        tools.addView(tile("◐", "Grayscale") { grayPage() }, weight(1f,4))
        tools.addView(tile("✦", "Auto") { autoEnhance() }, weight(1f,4))
        tools.addView(tile("↺", "Reset") { resetEdits() }, weight(1f,4))
        content.addView(tools)

        val exports = row()
        exports.addView(actionButton("EXPORT PDF") { savePdf() }, weight(1f,4))
        exports.addView(actionButton("SHARE") { showShareFormatDialog() }, weight(1f,4))
        content.addView(exports, margin(0,12))

        if (pages.isNotEmpty()) {
            content.addView(section("PAGE STRIP"))
            val strip = HorizontalScrollView(this)
            val stripRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pages.forEachIndexed { i, bmp ->
                stripRow.addView(thumbnail(bmp, i == currentPage) {
                    currentPage = i
                    previewBitmap = null
                    updatePreview()
                }, LinearLayout.LayoutParams(dp(92), dp(120)).apply { setMargins(dp(4),dp(4),dp(4),dp(4)) })
            }
            strip.addView(stripRow)
            content.addView(strip)
        }
    }

    private fun renderLibrary() {
        content.addView(title("ALL SCANS", "YOUR LOCAL DOCUMENT LIBRARY"))
        val search = EditText(this).apply {
            hint = "Search scans…"
            hintTextColor = Color.rgb(110,125,145)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine()
            background = rounded(Color.rgb(18,25,36),18)
            setPadding(dp(16),0,dp(16),0)
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0,0,0,dp(10)) })
        val controls = row()
        controls.addView(tile("☑", "Select all") {
            selectedLibrary.clear()
            library.list().forEach { selectedLibrary.add(it.absolutePath) }
            renderTab(2)
        }, weight(1f,4))
        controls.addView(tile("↕", "Newest") { renderTab(2) }, weight(1f,4))
        controls.addView(tile("⌗", "Grid/List") { renderTab(2) }, weight(1f,4))
        controls.addView(tile("↗", "Share") { showLibraryShareFormatDialog() }, weight(1f,4))
        content.addView(controls, margin(0,8))
        val list = library.list().filter { search.text.isNullOrBlank() || it.name.contains(search.text.toString(), true) }
        if (list.isEmpty()) content.addView(emptyCard("Library is empty", "Scan a page and it will be saved here automatically."))
        list.forEach { file -> content.addView(fileRow(file, true), margin(0,8)) }
        if (selectedLibrary.isNotEmpty()) content.addView(actionButton("EXPORT ${selectedLibrary.size} SELECTED") { showLibraryShareFormatDialog() }, margin(0,12))
    }

    private fun renderTools() {
        content.addView(title("TOOLS", "EXPORT • PDF STUDIO • RECOVERY • SETTINGS"))
        toolCard("PDF Studio", "Merge, reorder, rotate, compress and create PDFs") { showPdfStudio() }
        toolCard("Image Editor", "Brightness, contrast, rotate, grayscale, auto enhance and reset") { showImageEditor() }
        toolCard("Export Center", "Choose PDF • JPG • JPEG • PNG and share") { showShareFormatDialog() }
        toolCard("OCR", "Text extraction entry point for scanned pages") { showOcrInfo() }
        toolCard("History & Recovery", "Review local scans and recover the current interrupted session") { showHistoryRecovery() }
        toolCard("Privacy", "Local-first storage • no automatic upload") { showPrivacy() }
        content.addView(actionButton("START NEW DOCUMENT") { newDocument() }, margin(0,12))
        content.addView(actionButton("REFRESH SCANNER") { refreshUsb() })
    }

    private fun toolCard(title: String, subtitle: String, action: () -> Unit) {
        val c = card().apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        c.addView(TextView(this).apply { text = title; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        c.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(Color.rgb(135,150,170)); setPadding(0,dp(5),0,0) })
        c.addView(TextView(this).apply { text = "TAP TO OPEN  ›"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(85,160,255)); setPadding(0,dp(10),0,0) })
        content.addView(c, margin(0,8))
    }

    private fun showPdfStudio() {
        AlertDialog.Builder(this).setTitle("PDF Studio • Advanced")
            .setItems(arrayOf("Create PDF from current document", "Merge selected library scans into PDF", "Rotate current page", "Reorder pages")) { _, which ->
                when (which) {
                    0 -> savePdf()
                    1 -> exportSelectedPdf()
                    2 -> rotatePage()
                    3 -> showReorderDialog()
                }
            }.show()
    }

    private fun showImageEditor() {
        AlertDialog.Builder(this).setTitle("Image Editor • Advanced")
            .setItems(arrayOf("Brightness / Contrast", "Rotate 90°", "Grayscale", "Auto Enhance", "Reset edits")) { _, which ->
                when (which) {
                    0 -> renderTab(1)
                    1 -> rotatePage()
                    2 -> grayPage()
                    3 -> autoEnhance()
                    4 -> resetEdits()
                }
            }.show()
    }

    private fun showOcrInfo() {
        AlertDialog.Builder(this)
            .setTitle("OCR")
            .setMessage("OCR entry is available here. The current project does not include an OCR engine, so no fake text result is generated.")
            .setPositiveButton("OPEN SCAN STUDIO") { _, _ -> renderTab(1) }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun showHistoryRecovery() {
        val files = library.list()
        val message = if (files.isEmpty()) "No saved scans yet. Your local library will appear here after scanning." else "Local scans: ${files.size}\nCurrent document pages: ${pages.size}\n\nUse the Library to reopen saved pages, or Start New Document to clear the current session."
        AlertDialog.Builder(this).setTitle("History & Recovery").setMessage(message)
            .setPositiveButton("OPEN LIBRARY") { _, _ -> renderTab(2) }
            .setNegativeButton("CLOSE", null).show()
    }

    private fun showPrivacy() {
        AlertDialog.Builder(this).setTitle("Privacy")
            .setMessage("Scans are kept in local device storage. This app does not automatically upload your scan files.")
            .setPositiveButton("OK", null).show()
    }

    private fun showReorderDialog() {
        if (pages.size < 2) { setStatus("At least 2 pages are needed to reorder"); return }
        val labels = pages.indices.map { "Page ${it + 1}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Reorder Pages")
            .setItems(labels) { _, which -> showMovePageDialog(which) }.show()
    }

    private fun showMovePageDialog(index: Int) {
        val actions = arrayOf("Move up", "Move down", "Cancel")
        AlertDialog.Builder(this).setTitle("Page ${index + 1}").setItems(actions) { _, which ->
            when (which) {
                0 -> movePage(index, index - 1)
                1 -> movePage(index, index + 1)
            }
        }.show()
    }

    private fun movePage(from: Int, to: Int) {
        if (from !in pages.indices || to !in pages.indices) return
        val b = pages.removeAt(from)
        pages.add(to, b)
        val dpi = dpis.removeAt(from)
        dpis.add(to, dpi)
        currentPage = to
        renderTab(1)
        setStatus("Pages reordered")
    }

    private fun scan() {
        val scanner = protocol ?: run { requestScanner(); return }
        if (scanning || previewing) return
        val selectedDpi = (dpiSpinner?.selectedItem?.toString()?.substringBefore(" ")?.toIntOrNull() ?: 300)
        scanning = true
        setScannerButtonsEnabled(false)
        progress?.visibility = View.VISIBLE
        progressText?.visibility = View.VISIBLE
        progress?.progress = 0
        progressText?.text = "Preparing MF3010…"
        executor.execute {
            try {
                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                library.savePage(result.bitmap, "scan_${System.currentTimeMillis()}.jpg")
                runOnUiThread {
                    pages.add(result.bitmap)
                    dpis.add(result.dpi)
                    currentPage = pages.lastIndex
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = null
                    brightness = 0f
                    contrast = 1f
                    grayscale = false
                    setStatus("Scan complete • ${result.width} × ${result.height} • ${result.dpi} DPI")
                    finishScan()
                    renderTab(1)
                }
            } catch (t: Throwable) {
                runOnUiThread { setStatus("Scan failed • ${t.message ?: "Unknown scanner error"}"); finishScan() }
            }
        }
    }

    private fun previewScan() {
        val scanner = protocol ?: run { requestScanner(); return }
        if (scanning || previewing) return
        previewing = true
        setScannerButtonsEnabled(false)
        progress?.visibility = View.VISIBLE
        progressText?.visibility = View.VISIBLE
        progress?.progress = 0
        progressText?.text = "Fast preview • 150 DPI…"
        executor.execute {
            try {
                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = 150, color = colorSwitch?.isChecked ?: true)) { p, msg ->
                    runOnUiThread { progress?.progress = p; progressText?.text = msg }
                }
                runOnUiThread {
                    previewBitmap?.let { if (!it.isRecycled) it.recycle() }
                    previewBitmap = result.bitmap
                    brightness = 0f
                    contrast = 1f
                    grayscale = false
                    currentPage = -1
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    previewing = false
                    setScannerButtonsEnabled(true)
                    imagePreview?.setImageBitmap(previewBitmap)
                    setStatus("Fast preview ready • ${result.width} × ${result.height} • 150 DPI. Adjust settings, then tap SCAN PAGE.")
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    previewing = false
                    progress?.visibility = View.GONE
                    progressText?.visibility = View.GONE
                    setScannerButtonsEnabled(true)
                    setStatus("Preview failed • ${t.message ?: "Unknown scanner error"}")
                }
            }
        }
    }

    private fun setScannerButtonsEnabled(enabled: Boolean) {
        previewButton?.isEnabled = enabled && device != null
        scanButton?.isEnabled = enabled && device != null
    }

    private fun finishScan() {
        scanning = false
        progress?.visibility = View.GONE
        progressText?.visibility = View.GONE
        setScannerButtonsEnabled(true)
    }

    private fun refreshUsb() {
        val scanners = usb.scannerDevices()
        if (scanners.isEmpty()) {
            device = null
            protocol = null
            setStatus("MF3010 Disconnected • connect using USB-OTG")
        } else {
            setStatus("${scanners.size} supported scanner device(s) detected")
            if (device == null) requestScanner()
        }
        if (currentTab == 0 || currentTab == 1) renderTab(currentTab)
    }

    private fun requestScanner() {
        val d = usb.scannerDevices().firstOrNull() ?: run { setStatus("No supported scanner detected"); return }
        if (usb.hasPermission(d)) connectDevice(d) else { setStatus("Requesting USB access…"); usb.requestPermission(d) }
    }

    private fun connectDevice(d: UsbDevice) {
        if (!usb.open(d)) {
            device = null; protocol = null
            setStatus("MF3010 USB interface could not be opened")
            return
        }
        val p = ScannerProtocol(usb.connection()!!, usb.usbInterface()!!)
        if (!p.probe().supported) {
            device = null; protocol = null
            setStatus("Scanner transport unavailable")
            return
        }
        device = d
        protocol = p
        setStatus("MF3010 Connected • Ready to scan")
        runOnUiThread { renderTab(currentTab) }
    }

    private fun rotatePage() {
        if (!hasPage()) return
        replaceCurrent(document.rotate(pages[currentPage],90f))
        setStatus("Page rotated 90°")
    }

    private fun grayPage() {
        if (!hasPage()) return
        grayscale = true
        updatePreview()
        setStatus("Grayscale preview enabled")
    }

    private fun autoEnhance() {
        if (!hasPage()) return
        brightness = 0.03f
        contrast = 1.12f
        grayscale = false
        updatePreview()
        setStatus("Auto enhancement applied")
    }

    private fun resetEdits() {
        brightness = 0f
        contrast = 1f
        grayscale = false
        updatePreview()
        setStatus("Edits reset • original scan preserved")
    }

    private fun replaceCurrent(newBitmap: Bitmap) {
        val old = pages[currentPage]
        pages[currentPage] = newBitmap
        if (!old.isRecycled) old.recycle()
        updatePreview()
    }

    private fun processedPreview(): Bitmap {
        var out = pages[currentPage]
        if (brightness != 0f || contrast != 1f) out = library.applyAdjustments(out, brightness, contrast)
        if (grayscale) {
            val g = document.grayscale(out)
            if (out !== pages[currentPage]) out.recycle()
            out = g
        }
        return out
    }

    private fun updatePreview() {
        imagePreview?.setImageBitmap(if (hasPage()) processedPreview() else previewBitmap)
        pageCount?.text = "${pages.size} pages"
    }

    private fun hasPage() = currentPage in pages.indices

    private fun savePdf() {
        if (pages.isEmpty()) { setStatus("No pages to export"); return }
        setStatus("Creating PDF • ${pages.size} pages…")
        executor.execute {
            try {
                val tempFile = document.savePdf(pages, dpis)
                runOnUiThread { startPdfSaveFlow(tempFile) }
            } catch (t: Throwable) {
                runOnUiThread { setStatus("PDF export failed • ${t.message ?: "Unknown error"}") }
            }
        }
    }

    private fun startPdfSaveFlow(tempFile: File) {
        val savedTree = prefs.getString("pdf_tree_uri", null)
        val alwaysUse = prefs.getBoolean("pdf_always_use", false)
        if (alwaysUse && !savedTree.isNullOrBlank()) {
            saveFileToTree(Uri.parse(savedTree), tempFile, true)
            return
        }
        pendingTempPdf = tempFile
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, PDF_FOLDER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PDF_FOLDER_REQUEST || resultCode != RESULT_OK || data?.data == null) {
            if (requestCode == PDF_FOLDER_REQUEST) {
                pendingTempPdf?.delete()
                pendingTempPdf = null
                setStatus("PDF save cancelled")
            }
            return
        }
        val treeUri = data.data!!
        try { contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Throwable) {}
        val temp = pendingTempPdf ?: return
        pendingTempPdf = null
        AlertDialog.Builder(this)
            .setTitle("Save PDF here")
            .setMessage("Use this folder for this PDF, or remember it for future exports?")
            .setPositiveButton("ALWAYS USE THIS FOLDER") { _, _ ->
                prefs.edit().putString("pdf_tree_uri", treeUri.toString()).putBoolean("pdf_always_use", true).apply()
                saveFileToTree(treeUri, temp, true)
            }
            .setNeutralButton("SAVE HERE ONCE") { _, _ -> saveFileToTree(treeUri, temp, true) }
            .setNegativeButton("ALWAYS ASK") { _, _ ->
                prefs.edit().putBoolean("pdf_always_use", false).apply()
                saveFileToTree(treeUri, temp, true)
            }.show()
    }

    private fun saveFileToTree(treeUri: Uri, source: File, openAfter: Boolean) {
        executor.execute {
            try {
                val name = "USB_Scanner_${System.currentTimeMillis()}.pdf"
                val documentUri = DocumentsContract.createDocument(contentResolver, treeUri, "application/pdf", name)
                    ?: throw IllegalStateException("Could not create PDF in selected folder")
                source.inputStream().use { input ->
                    contentResolver.openOutputStream(documentUri)?.use { output -> input.copyTo(output) }
                        ?: throw IllegalStateException("Could not open selected folder")
                }
                source.delete()
                runOnUiThread {
                    setStatus("PDF saved • $name")
                    if (openAfter) openFile(documentUri, "application/pdf")
                }
            } catch (t: Throwable) { runOnUiThread { setStatus("PDF save failed • ${t.message ?: "Unknown error"}") } }
        }
    }

    private fun showShareFormatDialog() {
        if (!hasPage() && pages.isEmpty()) { setStatus("No page available to share"); return }
        val options = arrayOf("PDF", "JPG", "JPEG", "PNG")
        AlertDialog.Builder(this).setTitle("Share as").setItems(options) { _, which ->
            when (which) { 0 -> sharePdf(); 1 -> shareImage("jpg"); 2 -> shareImage("jpeg"); 3 -> shareImage("png") }
        }.show()
    }

    private fun showLibraryShareFormatDialog() {
        if (selectedLibrary.isEmpty()) { setStatus("Select at least one scan first"); return }
        val options = arrayOf("PDF", "PNG", "JPEG", "JPG")
        AlertDialog.Builder(this).setTitle("Share selected as").setItems(options) { _, which ->
            when (which) { 0 -> exportSelectedPdf(); 1 -> exportSelectedImages("png"); 2 -> exportSelectedImages("jpeg"); 3 -> exportSelectedImages("jpg") }
        }.show()
    }

    private fun sharePdf() {
        if (pages.isEmpty()) return
        executor.execute {
            try {
                val file = document.savePdf(pages, dpis)
                runOnUiThread { shareFiles(listOf(file), "application/pdf") }
            } catch (t: Throwable) { runOnUiThread { setStatus("PDF share failed • ${t.message ?: "Unknown error"}") } }
        }
    }

    private fun shareImage(format: String) {
        if (!hasPage()) { setStatus("Select a page first"); return }
        executor.execute {
            try {
                val bmp = processedPreview()
                val ext = if (format == "png") "png" else "jpg"
                val mime = if (format == "png") "image/png" else "image/jpeg"
                val file = File(cacheDir, "USB_Scanner_share_${System.currentTimeMillis()}.$ext")
                FileOutputStream(file).use { out -> bmp.compress(if (format == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, out) }
                runOnUiThread { shareFiles(listOf(file), mime) }
            } catch (t: Throwable) { runOnUiThread { setStatus("Image share failed • ${t.message ?: "Unknown error"}") } }
        }
    }

    private fun exportSelectedPdf() {
        val files = library.list().filter { selectedLibrary.contains(it.absolutePath) }
        if (files.isEmpty()) { setStatus("No selected scans"); return }
        executor.execute {
            val bitmaps = mutableListOf<Bitmap>()
            try {
                files.take(50).forEach { library.decode(it)?.let { b -> bitmaps.add(b) } }
                if (bitmaps.isEmpty()) throw IllegalStateException("Could not read selected scans")
                val file = document.savePdf(bitmaps, List(bitmaps.size) { 300 })
                runOnUiThread { shareFiles(listOf(file), "application/pdf") }
            } catch (t: Throwable) {
                runOnUiThread { setStatus("Selected PDF share failed • ${t.message ?: "Unknown error"}") }
            } finally { bitmaps.forEach { if (!it.isRecycled) it.recycle() } }
        }
    }

    private fun exportSelectedImages(format: String) {
        val files = library.list().filter { selectedLibrary.contains(it.absolutePath) }
        if (files.isEmpty()) { setStatus("No selected scans"); return }
        executor.execute {
            val output = mutableListOf<File>()
            try {
                files.take(20).forEach { source ->
                    val bmp = library.decode(source) ?: return@forEach
                    val ext = if (format == "png") "png" else format
                    val mime = if (format == "png") "image/png" else "image/jpeg"
                    val outFile = File(cacheDir, "USB_Scanner_${System.currentTimeMillis()}_${output.size}.$ext")
                    FileOutputStream(outFile).use { out -> bmp.compress(if (format == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, out) }
                    if (!bmp.isRecycled) bmp.recycle()
                    output.add(outFile)
                    if (output.size == 20) return@forEach
                }
                if (output.isEmpty()) throw IllegalStateException("Could not read selected scans")
                val mime = if (format == "png") "image/png" else "image/jpeg"
                runOnUiThread { shareFiles(output, mime) }
            } catch (t: Throwable) { runOnUiThread { setStatus("Image share failed • ${t.message ?: "Unknown error"}") } }
        }
    }

    private fun openFile(uri: Uri, mime: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
        } catch (_: Throwable) { setStatus("No app available to open this PDF") }
    }

    private fun shareFiles(files: List<File>, mime: String) {
        try {
            val uris = ArrayList<Uri>()
            files.forEach { uris.add(FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)) }
            val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uris[0]) }
            else Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = mime; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Share scan"))
        } catch (t: Throwable) { setStatus("Sharing unavailable • ${t.message ?: "No compatible app"}") }
    }

    private fun newDocument() {
        pages.forEach { if (!it.isRecycled) it.recycle() }
        pages.clear(); dpis.clear(); currentPage = -1
        previewBitmap?.let { if (!it.isRecycled) it.recycle() }
        previewBitmap = null; brightness = 0f; contrast = 1f; grayscale = false
        renderTab(1)
        setStatus("New document ready")
    }

    private fun setStatus(text: String) { if (::status.isInitialized) status.text = text }

    private fun navButton(icon: String, label: String, tab: Int) {
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(0,dp(2),0,dp(2))
            setOnClickListener { renderTab(tab) }
        }
        holder.addView(TextView(this).apply {
            text = icon
            gravity = Gravity.CENTER
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (tab == currentTab) Color.rgb(85,160,255) else Color.rgb(125,140,160))
        })
        holder.addView(TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (tab == currentTab) Color.rgb(85,160,255) else Color.rgb(125,140,160))
        })
        nav.addView(holder, weight(1f,2))
    }

    private fun title(main: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@MainActivity).apply { text = main; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        addView(TextView(this@MainActivity).apply { text = sub; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(105,145,190)); setPadding(0,dp(4),0,dp(14)) })
    }

    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(15),dp(16),dp(15)); background = panel() }

    private fun emptyCard(a: String,b: String) = card().apply {
        addView(TextView(this@MainActivity).apply { text = a; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        addView(TextView(this@MainActivity).apply { text = b; textSize = 12f; setTextColor(Color.rgb(130,145,165)); setPadding(0,dp(5),0,0) })
    }

    private fun section(text: String) = TextView(this).apply { this.text = text; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(120,145,175)); setPadding(dp(2),dp(4),0,dp(7)) }

    private fun actionButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); isAllCaps = false
        background = rounded(Color.rgb(28,108,205),18); setOnClickListener { action() }; minHeight = 0; minimumHeight = 0
    }

    private fun tile(icon: String, text: String, action: () -> Unit) = Button(this).apply {
        this.text = "$icon\n$text"; textSize = 11f; setTextColor(Color.rgb(215,225,238)); isAllCaps = false
        background = rounded(Color.rgb(18,25,36),16); setOnClickListener { action() }; minHeight = 0; minimumHeight = 0
    }

    private fun statCard(value: String, label: String) = card().apply {
        gravity = Gravity.CENTER
        addView(TextView(this@MainActivity).apply { text = value; textSize = 21f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        addView(TextView(this@MainActivity).apply { text = label; textSize = 9f; setTextColor(Color.rgb(120,140,165)); gravity = Gravity.CENTER })
    }

    private fun fileRow(file: File, selectable: Boolean = false): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(10),dp(12),dp(10)); background = panel() }
        val img = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(58),dp(58)); scaleType = ImageView.ScaleType.CENTER_CROP; setImageBitmap(library.decode(file)) }
        r.addView(img)
        val t = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10),0,0,0) }
        t.addView(TextView(this).apply { text = file.name; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        t.addView(TextView(this).apply { text = "${(file.length()/1024).coerceAtLeast(1)} KB • ${file.extension.uppercase()}"; textSize = 11f; setTextColor(Color.rgb(125,140,160)); setPadding(0,dp(4),0,0) })
        r.addView(t, LinearLayout.LayoutParams(0,-2,1f))
        if (selectable) {
            val cb = CheckBox(this).apply {
                isChecked = selectedLibrary.contains(file.absolutePath)
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(80,150,255))
                setOnCheckedChangeListener { _, checked -> if (checked) selectedLibrary.add(file.absolutePath) else selectedLibrary.remove(file.absolutePath) }
            }
            r.addView(cb)
        } else {
            r.setOnClickListener {
                executor.execute {
                    val b = library.decode(file)
                    if (b != null) runOnUiThread {
                        pages.add(b); dpis.add(300); currentPage = pages.lastIndex; previewBitmap = null; renderTab(1)
                    }
                }
            }
        }
        return r
    }

    private fun thumbnail(bmp: Bitmap, selected: Boolean, click: () -> Unit) = ImageButton(this).apply {
        setImageBitmap(bmp); scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(if (selected) Color.rgb(28,108,205) else Color.rgb(20,28,40),12)
        setPadding(dp(4),dp(4),dp(4),dp(4)); setOnClickListener { click() }
    }

    private fun slider(label: String, min: Int, max: Int, initial: Int, changed: (Int) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val header = row()
        header.addView(TextView(this@MainActivity).apply { text = label; textSize = 12f; setTextColor(Color.rgb(170,185,205)); setPadding(0,dp(7),0,0) }, weight(1f,0))
        val value = TextView(this@MainActivity).apply { text = initial.toString(); textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(dp(8),dp(4),dp(8),0) }
        header.addView(value, LinearLayout.LayoutParams(dp(48),dp(28)))
        addView(header)
        val controls = row()
        val minus = Button(this@MainActivity).apply { text = "−"; textSize = 18f; minHeight = 0; minimumHeight = 0; setTextColor(Color.WHITE); background = rounded(Color.rgb(18,25,36),12) }
        val plus = Button(this@MainActivity).apply { text = "+"; textSize = 18f; minHeight = 0; minimumHeight = 0; setTextColor(Color.WHITE); background = rounded(Color.rgb(18,25,36),12) }
        val s = SeekBar(this@MainActivity).apply {
            this.max = max - min
            progress = initial - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(v: SeekBar?, p: Int, fromUser: Boolean) { val n = p + min; value.text = n.toString(); changed(n) }
                override fun onStartTrackingTouch(v: SeekBar?) {}
                override fun onStopTrackingTouch(v: SeekBar?) {}
            })
        }
        minus.setOnClickListener { s.progress = (s.progress - 1).coerceAtLeast(0) }
        plus.setOnClickListener { s.progress = (s.progress + 1).coerceAtMost(s.max) }
        controls.addView(minus, LinearLayout.LayoutParams(dp(44),dp(42)).apply { setMargins(0,dp(2),dp(6),0) })
        controls.addView(s, weight(1f,0))
        controls.addView(plus, LinearLayout.LayoutParams(dp(44),dp(42)).apply { setMargins(dp(6),dp(2),0,0) })
        addView(controls)
    }

    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun panel() = rounded(Color.rgb(15,21,31),18)
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(28,38,52)) }
    private fun margin(top: Int,bottom: Int) = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(top),0,dp(bottom)) }
    private fun weight(w: Float,m: Int) = LinearLayout.LayoutParams(0,-2,w).apply { if (m > 0) setMargins(dp(m),0,dp(m),0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        executor.shutdownNow()
        usb.close()
        usb.unregister()
        previewBitmap?.let { if (!it.isRecycled) it.recycle() }
        pages.forEach { if (!it.isRecycled) it.recycle() }
        super.onDestroy()
    }
}
