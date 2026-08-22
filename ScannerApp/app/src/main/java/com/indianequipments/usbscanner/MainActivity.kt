package com.indianequipments.usbscanner

import android.app.Activity
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var usb: UsbScannerManager
    private lateinit var status: TextView
    private lateinit var deviceText: TextView
    private var connectedDevice: UsbDevice? = null
    private val pages = mutableListOf<Bitmap>()
    private lateinit var document: ScanDocument

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        document = ScanDocument(this)
        usb = UsbScannerManager(this)
        usb.onPermissionGranted = { device -> connectDevice(device) }
        usb.onPermissionDenied = { if (::status.isInitialized) status.text = "USB permission denied." }
        setContentView(buildUi())
        usb.register()
        refreshDevices()
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 36, 32, 36)
        }
        val title = TextView(this).apply {
            text = "USB Scanner"
            textSize = 30f
            gravity = Gravity.CENTER_HORIZONTAL
        }
        status = TextView(this).apply { text = "Waiting for scanner…"; textSize = 16f; setPadding(0, 18, 0, 18) }
        deviceText = TextView(this).apply { textSize = 15f }
        val refresh = Button(this).apply { text = "Refresh USB devices"; setOnClickListener { refreshDevices() } }
        val connect = Button(this).apply { text = "Connect scanner"; setOnClickListener { requestFirstScanner() } }
        val scan = Button(this).apply { text = "SCAN"; setOnClickListener { runScan() } }
        val savePdf = Button(this).apply { text = "Save scanned pages as PDF"; setOnClickListener { savePdf() } }
        root.addView(title); root.addView(status); root.addView(deviceText)
        root.addView(refresh); root.addView(connect); root.addView(scan); root.addView(savePdf)
        return ScrollView(this).apply { addView(root) }
    }

    private fun refreshDevices() {
        val all = usb.allDevices()
        val scanners = usb.scannerDevices()
        deviceText.text = if (all.isEmpty()) {
            "No USB device detected. Connect the scanner using a USB-OTG adapter."
        } else {
            all.joinToString("\n") { d ->
                val scanner = if (scanners.any { it.deviceId == d.deviceId }) " [SUPPORTED SCANNER]" else ""
                "${d.productName ?: "USB device"}  VID ${d.vendorId} / PID ${d.productId}$scanner"
            }
        }
        status.text = "${scanners.size} supported scanner device(s) detected."
    }

    private fun requestFirstScanner() {
        val device = usb.scannerDevices().firstOrNull()
        if (device == null) {
            status.text = "No supported scanner detected."
            return
        }

        if (usb.hasPermission(device)) {
            status.text = "USB permission already granted. Connecting…"
            connectDevice(device)
            return
        }

        status.text = "Requesting USB permission…"
        usb.requestPermission(device)
    }

    private fun connectDevice(device: UsbDevice) {
        if (!usb.open(device)) {
            status.text = "MF3010 detected, but no usable bulk USB interface was found."
            return
        }
        connectedDevice = device
        val protocol = ScannerProtocol(usb.connection()!!, usb.usbInterface()!!)
        val cap = protocol.probe()
        status.text = "Connected: ${device.productName ?: "USB scanner"}\n${cap.note}"
    }

    private fun runScan() {
        if (connectedDevice == null) {
            status.text = "Connect the scanner first."
            return
        }
        status.text = "MF3010 connected. Real scan backend is the next step; no unverified scan command is being sent."
    }

    private fun savePdf() {
        if (pages.isEmpty()) { status.text = "No scanned pages available."; return }
        val file = document.savePdf(pages)
        status.text = "PDF saved: ${file.absolutePath}"
    }

    override fun onDestroy() {
        usb.close(); usb.unregister(); super.onDestroy()
    }
}
