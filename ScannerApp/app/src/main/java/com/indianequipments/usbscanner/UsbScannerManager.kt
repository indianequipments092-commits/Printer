package com.indianequipments.usbscanner

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build

class UsbScannerManager(private val context: Context) {
    companion object { private const val ACTION_USB_PERMISSION = "com.indianequipments.usbscanner.USB_PERMISSION" }
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var interfaceClaimed: UsbInterface? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) onPermissionGranted?.invoke(device)
                else onPermissionDenied?.invoke()
            }
        }
    }

    var onPermissionGranted: ((UsbDevice) -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    fun register() {
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION))
    }

    fun unregister() { runCatching { context.unregisterReceiver(receiver) } }

    fun scannerDevices(): List<UsbDevice> = usbManager.deviceList.values.filter { device -> device.interfaces.any { it.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE } }
    fun allDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()

    fun requestPermission(device: UsbDevice) {
        val intent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE)
        usbManager.requestPermission(device, intent)
    }

    fun open(device: UsbDevice): Boolean {
        close()
        val intf = device.interfaces.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE } ?: return false
        val conn = usbManager.openDevice(device) ?: return false
        if (!conn.claimInterface(intf, true)) { conn.close(); return false }
        connection = conn; interfaceClaimed = intf; return true
    }

    fun connection(): UsbDeviceConnection? = connection
    fun usbInterface(): UsbInterface? = interfaceClaimed

    fun close() {
        interfaceClaimed?.let { connection?.releaseInterface(it) }
        connection?.close(); interfaceClaimed = null; connection = null
    }
}
