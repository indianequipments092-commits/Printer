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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var usb: UsbScannerManager
    private lateinit var status: TextView
    private lateinit var deviceText: TextView
    private lateinit var scanButton: Button
    private lateinit var savePdfButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var preview: ImageView
    private lateinit var dpiSpinner: Spinner
    private lateinit var colorCheck: CheckBox

    private var connectedDevice: UsbDevice? = null
    private var protocol: ScannerProtocol? = null
    private val pages = mutableListOf<Bitmap>()
    private val pageDpis = mutableListOf<Int>()
    private lateinit var document: ScanDocument
    private val executor = Executors.newSingleThreadExecutor()
    private var scanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        document = ScanDocument(this)
        usb = UsbScannerManager(this)
        usb.onPermissionGranted = { device -> connectDevice(device) }
        usb.onPermissionDenied = {
            runOnUiThread { status.text = "USB permission denied. Tap Connect scanner and allow access." }
        }
        setContentView(buildUi())
        usb.register()
        refreshDevices()
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "USB Scanner"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 35, 50))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val subtitle = TextView(this).apply {
            text = "Canon MF3010 • USB scanning"
            textSize = 14f
            setTextColor(Color.rgb(100, 110, 125))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(4), 0, dp(18))
        }

        status = cardText("Waiting for scanner…", 15f)
        deviceText = cardText("Checking USB devices…", 14f)

        val refresh = actionButton("REFRESH USB DEVICES") { refreshDevices() }
        val connect = actionButton("CONNECT SCANNER") { requestFirstScanner() }
        scanButton = actionButton("SCAN PAGE") { runScan() }
        savePdfButton = actionButton("SAVE SCANNED PAGES AS PDF") { savePdf() }
        scanButton.isEnabled = false
        savePdfButton.isEnabled = false

        dpiSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                listOf("75 DPI", "150 DPI", "300 DPI", "600 DPI")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(2)
        }
        colorCheck = CheckBox(this).apply {
            text = "Color scan"
            textSize = 15f
            isChecked = true
        }

        val settingsTitle = sectionTitle("SCAN SETTINGS")
        val settingsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        settingsRow.addView(dpiSpinner, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
            setMargins(0, 0, dp(8), 0)
        })
        settingsRow.addView(colorCheck, LinearLayout.LayoutParams(0, dp(52), 1f))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }
        progressText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(80, 90, 105))
            visibility = View.GONE
            setPadding(0, dp(6), 0, dp(8))
        }

        val previewTitle = sectionTitle("LAST SCANNED PAGE")
        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setBackgroundColor(Color.rgb(245, 247, 250))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status, marginParams(bottom = 10))
        root.addView(deviceText, marginParams(bottom = 14))
        root.addView(refresh, marginParams(bottom = 8))
        root.addView(connect, marginParams(bottom = 16))
        root.addView(settingsTitle)
        root.addView(settingsRow, marginParams(bottom = 10))
        root.addView(progress, marginParams(bottom = 0))
        root.addView(progressText)
        root.addView(scanButton, marginParams(bottom = 8))
        root.addView(savePdfButton, marginParams(bottom = 18))
        root.addView(previewTitle)
        root.addView(preview, marginParams())

        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun refreshDevices() {
        val all = usb.allDevices()
        val scanners = usb.scannerDevices()
        deviceText.text = if (all.isEmpty()) {
            "No USB device detected. Connect the MF3010 with a USB-OTG adapter."
        } else {
            all.joinToString("\n") { d ->
                val scanner = if (scanners.any { it.deviceId == d.deviceId }) "  •  SUPPORTED SCANNER" else ""
                "${d.productName ?: "USB device"}\nVID ${d.vendorId} / PID ${d.productId}$scanner"
            }
        }
        status.text = if (scanners.isEmpty()) {
            "No supported scanner detected."
        } else {
            "${scanners.size} supported scanner device(s) detected."
        }
        if (connectedDevice == null) scanButton.isEnabled = false
    }

    private fun requestFirstScanner() {
        val device = usb.scannerDevices().firstOrNull()
        if (device == null) {
            status.text = "No supported scanner detected."
            return
        }

        if (usb.hasPermission(device)) {
            status.text = "USB access already granted. Connecting…"
            connectDevice(device)
            return
        }

        status.text = "Requesting USB access…"
        usb.requestPermission(device)
    }

    private fun connectDevice(device: UsbDevice) {
        if (!usb.open(device)) {
            connectedDevice = null
            protocol = null
            scanButton.isEnabled = false
            status.text = "MF3010 detected, but its scanner USB interface could not be opened."
            return
        }

        connectedDevice = device
        protocol = ScannerProtocol(usb.connection()!!, usb.usbInterface()!!)
        val cap = protocol!!.probe()
        if (!cap.supported) {
            status.text = "Scanner connected, but its bulk scan transport is unavailable."
            scanButton.isEnabled = false
            return
        }

        status.text = "Connected: ${device.productName ?: "MF3010"}\nReady to scan."
        deviceText.text = "MF3010  •  VID ${device.vendorId} / PID ${device.productId}\nCanon USB scanner interface ready"
        scanButton.isEnabled = true
        savePdfButton.isEnabled = pages.isNotEmpty()
    }

    private fun runScan() {
        val scanner = protocol
        if (scanner == null || connectedDevice == null) {
            status.text = "Connect the scanner first."
            return
        }
        if (scanning) return

        val dpi = dpiSpinner.selectedItem.toString().substringBefore(" ").toInt()
        val color = colorCheck.isChecked
        scanning = true
        scanButton.isEnabled = false
        savePdfButton.isEnabled = false
        progress.progress = 0
        progress.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        progressText.text = "Starting scan…"

        executor.execute {
            try {
                val result = scanner.scan(
                    ScannerProtocol.ScanConfig(dpi = dpi, color = color)
                ) { percent, message ->
                    runOnUiThread {
                        progress.progress = percent
                        progressText.text = message
                    }
                }

                runOnUiThread {
                    pages.add(result.bitmap)
                    pageDpis.add(result.dpi)
                    preview.setImageBitmap(result.bitmap)
                    preview.visibility = View.VISIBLE
                    status.text = "Scan complete • ${result.width} × ${result.height} px • ${result.dpi} DPI"
                    savePdfButton.isEnabled = true
                    progress.visibility = View.GONE
                    progressText.visibility = View.GONE
                    scanButton.isEnabled = connectedDevice != null
                    scanning = false
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    status.text = "Scan failed: ${t.message ?: "Unknown scanner error"}"
                    progress.visibility = View.GONE
                    progressText.visibility = View.GONE
                    scanButton.isEnabled = connectedDevice != null
                    savePdfButton.isEnabled = pages.isNotEmpty()
                    scanning = false
                }
            }
        }
    }

    private fun savePdf() {
        if (pages.isEmpty()) {
            status.text = "No scanned pages available."
            return
        }
        savePdfButton.isEnabled = false
        status.text = "Creating PDF…"
        executor.execute {
            try {
                val file = document.savePdf(pages, pageDpis)
                runOnUiThread {
                    status.text = "PDF saved successfully.\n${file.absolutePath}"
                    savePdfButton.isEnabled = true
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    status.text = "PDF save failed: ${t.message ?: "Unknown error"}"
                    savePdfButton.isEnabled = true
                }
            }
        }
    }

    private fun cardText(value: String, size: Float): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.rgb(55, 65, 80))
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = rounded(Color.rgb(245, 247, 250), 14)
    }

    private fun sectionTitle(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(100, 110, 125))
        setPadding(dp(2), dp(4), 0, dp(6))
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = rounded(Color.rgb(21, 101, 192), 14)
        setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun marginParams(bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            if (bottom != 0) setMargins(0, 0, 0, dp(bottom))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        executor.shutdownNow()
        usb.close()
        usb.unregister()
        pages.forEach { if (!it.isRecycled) it.recycle() }
        super.onDestroy()
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
