package com.indianequipments.usbscanner

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var usb: UsbScannerManager
    private lateinit var status: TextView
    private lateinit var deviceText: TextView
    private lateinit var scanButton: Button
    private lateinit var pdfButton: Button
    private lateinit var jpgButton: Button
    private lateinit var rotateButton: Button
    private lateinit var grayButton: Button
    private lateinit var deleteButton: Button
    private lateinit var newButton: Button
    private lateinit var pagesText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var preview: ImageView
    private lateinit var dpi: Spinner
    private lateinit var color: CheckBox
    private var device: UsbDevice? = null
    private var protocol: ScannerProtocol? = null
    private val pages = mutableListOf<Bitmap>()
    private val dpis = mutableListOf<Int>()
    private lateinit var document: ScanDocument
    private val executor = Executors.newSingleThreadExecutor()
    private var scanning = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        document = ScanDocument(this)
        usb = UsbScannerManager(this)
        usb.onPermissionGranted = { connectDevice(it) }
        usb.onPermissionDenied = { runOnUiThread { status.text = "USB permission denied. Tap Connect again." } }
        setContentView(buildUi())
        usb.register()
        refreshDevices()
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        val title = TextView(this).apply {
            text = "USB SCANNER"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(18, 30, 46))
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Canon MF3010  •  Professional scan workspace"
            textSize = 14f
            setTextColor(Color.rgb(90, 104, 122))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(16))
        }
        root.addView(title)
        root.addView(sub)
        status = card("Waiting for scanner…", 15f)
        deviceText = card("Checking USB devices…", 13f)
        root.addView(status, lp(0, 8))
        root.addView(deviceText, lp(0, 10))

        val conn = row()
        conn.addView(smallButton("Refresh") { refreshDevices() }, weight(1f, 4))
        conn.addView(smallButton("Connect Scanner") { requestScanner() }, weight(1f, 4))
        root.addView(conn, lp(0, 16))

        root.addView(section("SCAN SETTINGS"))
        val settings = row().apply {
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = bg(Color.WHITE, 16)
        }
        dpi = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item,
                listOf("75 DPI", "150 DPI", "300 DPI", "600 DPI")).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(2)
        }
        color = CheckBox(this).apply { text = "Color"; textSize = 14f; isChecked = true }
        settings.addView(TextView(this).apply { text = "Resolution"; textSize = 13f }, weight(1f, 3))
        settings.addView(dpi, weight(1.3f, 3))
        settings.addView(color, weight(1f, 3))
        root.addView(settings, lp(0, 12))

        scanButton = mainButton("SCAN DOCUMENT") { scan() }
        scanButton.isEnabled = false
        root.addView(scanButton, lp(0, 10))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }
        progressText = TextView(this).apply { textSize = 13f; setTextColor(Color.DKGRAY); visibility = View.GONE }
        root.addView(progress)
        root.addView(progressText, lp(0, 12))

        val header = row()
        header.addView(section("CURRENT DOCUMENT"), weight(1f, 0))
        pagesText = TextView(this).apply { text = "0 pages"; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(21,101,192)) }
        header.addView(pagesText)
        root.addView(header, lp(0, 6))
        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
        root.addView(preview, lp(0, 10))

        val edit = row()
        rotateButton = smallButton("Rotate") { rotatePage() }
        grayButton = smallButton("Grayscale") { grayPage() }
        deleteButton = smallButton("Delete") { deletePage() }
        rotateButton.isEnabled = false; grayButton.isEnabled = false; deleteButton.isEnabled = false
        edit.addView(rotateButton, weight(1f, 3)); edit.addView(grayButton, weight(1f, 3)); edit.addView(deleteButton, weight(1f, 3))
        root.addView(edit, lp(0, 10))

        pdfButton = mainButton("SAVE ALL PAGES AS PDF") { savePdf() }
        jpgButton = smallButton("SAVE CURRENT PAGE AS JPG") { saveJpg() }
        newButton = smallButton("START NEW DOCUMENT") { newDocument() }
        pdfButton.isEnabled = false; jpgButton.isEnabled = false; newButton.isEnabled = false
        root.addView(pdfButton, lp(0, 8)); root.addView(jpgButton, lp(0, 8)); root.addView(newButton)
        return ScrollView(this).apply { isFillViewport = true; addView(root) }
    }

    private fun refreshDevices() {
        val all = usb.allDevices(); val scanners = usb.scannerDevices()
        deviceText.text = if (all.isEmpty()) "No USB device detected. Connect MF3010 using USB-OTG." else all.joinToString("\n") { d ->
            val mark = if (scanners.any { it.deviceId == d.deviceId }) "  •  SUPPORTED SCANNER" else ""
            "${d.productName ?: "USB device"}\nVID ${d.vendorId} / PID ${d.productId}$mark"
        }
        status.text = if (scanners.isEmpty()) "No supported scanner detected." else "${scanners.size} supported scanner device(s) detected."
        if (device == null) scanButton.isEnabled = false
    }

    private fun requestScanner() {
        val d = usb.scannerDevices().firstOrNull() ?: run { status.text = "No supported scanner detected."; return }
        if (usb.hasPermission(d)) connectDevice(d) else { status.text = "Requesting USB access…"; usb.requestPermission(d) }
    }

    private fun connectDevice(d: UsbDevice) {
        if (!usb.open(d)) { device = null; protocol = null; scanButton.isEnabled = false; status.text = "MF3010 USB interface could not be opened."; return }
        device = d
        protocol = ScannerProtocol(usb.connection()!!, usb.usbInterface()!!)
        if (!protocol!!.probe().supported) { status.text = "Scanner transport unavailable."; scanButton.isEnabled = false; return }
        status.text = "Connected: ${d.productName ?: "MF3010"}\nReady to scan."
        deviceText.text = "MF3010  •  VID ${d.vendorId} / PID ${d.productId}\nCanon USB scanner interface ready"
        scanButton.isEnabled = true
    }

    private fun scan() {
        val scanner = protocol ?: run { status.text = "Connect the scanner first."; return }
        if (scanning) return
        val selectedDpi = dpi.selectedItem.toString().substringBefore(" ").toInt()
        scanning = true; scanButton.isEnabled = false; pdfButton.isEnabled = false
        progress.progress = 0; progress.visibility = View.VISIBLE; progressText.visibility = View.VISIBLE; progressText.text = "Starting scan…"
        executor.execute {
            try {
                val result = scanner.scan(ScannerProtocol.ScanConfig(dpi = selectedDpi, color = color.isChecked)) { p, msg ->
                    runOnUiThread { progress.progress = p; progressText.text = msg }
                }
                runOnUiThread {
                    pages.add(result.bitmap); dpis.add(result.dpi); showPage()
                    status.text = "Scan complete  •  ${result.width} × ${result.height} px  •  ${result.dpi} DPI"
                    finishScanUi()
                }
            } catch (t: Throwable) {
                runOnUiThread { status.text = "Scan failed: ${t.message ?: "Unknown scanner error"}"; finishScanUi() }
            }
        }
    }

    private fun finishScanUi() { progress.visibility = View.GONE; progressText.visibility = View.GONE; scanning = false; scanButton.isEnabled = device != null; pdfButton.isEnabled = pages.isNotEmpty() }

    private fun showPage() {
        if (pages.isEmpty()) return
        preview.setImageBitmap(pages.last()); preview.visibility = View.VISIBLE
        pagesText.text = "${pages.size} page${if (pages.size == 1) "" else "s"}"
        rotateButton.isEnabled = true; grayButton.isEnabled = true; deleteButton.isEnabled = true; pdfButton.isEnabled = true; jpgButton.isEnabled = true; newButton.isEnabled = true
    }

    private fun rotatePage() {
        if (pages.isEmpty()) return
        val old = pages.removeAt(pages.lastIndex); pages.add(document.rotate(old, 90f)); old.recycle(); preview.setImageBitmap(pages.last()); status.text = "Page rotated 90°."
    }

    private fun grayPage() {
        if (pages.isEmpty()) return
        val old = pages.removeAt(pages.lastIndex); pages.add(document.grayscale(old)); old.recycle(); preview.setImageBitmap(pages.last()); status.text = "Page converted to grayscale."
    }

    private fun deletePage() {
        if (pages.isEmpty()) return
        pages.removeAt(pages.lastIndex).also { if (!it.isRecycled) it.recycle() }; dpis.removeAt(dpis.lastIndex)
        if (pages.isEmpty()) newDocument() else { showPage(); status.text = "Page deleted." }
    }

    private fun newDocument() {
        pages.forEach { if (!it.isRecycled) it.recycle() }; pages.clear(); dpis.clear(); preview.visibility = View.GONE; pagesText.text = "0 pages"
        rotateButton.isEnabled = false; grayButton.isEnabled = false; deleteButton.isEnabled = false; pdfButton.isEnabled = false; jpgButton.isEnabled = false; newButton.isEnabled = false; status.text = "New document ready."
    }

    private fun savePdf() {
        if (pages.isEmpty()) return
        status.text = "Creating PDF from ${pages.size} page(s)…"; pdfButton.isEnabled = false
        executor.execute { try { val f = document.savePdf(pages, dpis); runOnUiThread { status.text = "PDF saved successfully.\n${f.absolutePath}"; pdfButton.isEnabled = true } } catch (t: Throwable) { runOnUiThread { status.text = "PDF save failed: ${t.message ?: "Unknown error"}"; pdfButton.isEnabled = true } } }
    }

    private fun saveJpg() {
        if (pages.isEmpty()) return
        status.text = "Saving current page…"
        executor.execute { try { val f = document.savePng(pages.last(), "scan_${System.currentTimeMillis()}.png"); runOnUiThread { status.text = "Image saved successfully.\n${f.absolutePath}" } } catch (t: Throwable) { runOnUiThread { status.text = "Image save failed: ${t.message ?: "Unknown error"}" } } }
    }

    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun card(text: String, size: Float) = TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(55,65,80)); setPadding(dp(14),dp(12),dp(14),dp(12)); background = bg(Color.WHITE,16) }
    private fun section(text: String) = TextView(this).apply { this.text = text; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(92,105,122)); setPadding(dp(2),dp(4),0,dp(6)) }
    private fun mainButton(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); isAllCaps = false; background = bg(Color.rgb(21,101,192),16); setOnClickListener { action() } }
    private fun smallButton(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 13f; setTextColor(Color.rgb(30,65,105)); isAllCaps = false; background = bg(Color.WHITE,14); setOnClickListener { action() } }
    private fun bg(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun lp(top: Int, bottom: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (top != 0 || bottom != 0) setMargins(0,dp(top),0,dp(bottom)) }
    private fun weight(w: Float, margin: Int) = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,w).apply { if (margin != 0) setMargins(dp(margin),0,dp(margin),0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() { executor.shutdownNow(); usb.close(); usb.unregister(); pages.forEach { if (!it.isRecycled) it.recycle() }; super.onDestroy() }
}
