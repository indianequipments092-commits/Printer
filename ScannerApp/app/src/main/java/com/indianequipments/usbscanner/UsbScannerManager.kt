package com.indianequipments.usbscanner

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build

class UsbScannerManager(private val context: Context) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.indianequipments.usbscanner.USB_PERMISSION"
        private const val CANON_VID = 0x04A9
        private const val MF3010_PID = 0x2759
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connection: UsbDeviceConnection? = null
    private var interfaceClaimed: UsbInterface? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return

            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                onPermissionGranted?.invoke(device)
            } else {
                onPermissionDenied?.invoke()
            }
        }
    }

    var onPermissionGranted: ((UsbDevice) -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    fun register() {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                receiver,
                IntentFilter(ACTION_USB_PERMISSION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION))
        }
    }

    fun unregister() = runCatching { context.unregisterReceiver(receiver) }

    private fun interfaces(device: UsbDevice): List<UsbInterface> =
        (0 until device.interfaceCount).map { device.getInterface(it) }

    private fun endpoints(intf: UsbInterface): List<UsbEndpoint> =
        (0 until intf.endpointCount).map { intf.getEndpoint(it) }

    private fun hasBulkInOut(intf: UsbInterface): Boolean {
        val eps = endpoints(intf)
        val inEndpoint = eps.any {
            it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == UsbConstants.USB_DIR_IN
        }
        val outEndpoint = eps.any {
            it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                it.direction == UsbConstants.USB_DIR_OUT
        }
        return inEndpoint && outEndpoint
    }

    fun isKnownMf3010(device: UsbDevice): Boolean =
        device.vendorId == CANON_VID && device.productId == MF3010_PID

    fun scannerDevices(): List<UsbDevice> = usbManager.deviceList.values.filter { device ->
        isKnownMf3010(device) || interfaces(device).any {
            it.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE && hasBulkInOut(it)
        }
    }

    fun allDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    /**
     * Android adds EXTRA_DEVICE and EXTRA_PERMISSION_GRANTED to the permission
     * result PendingIntent. On Android 12+ the PendingIntent must be mutable so
     * the system can attach those result extras.
     */
    fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            @Suppress("DEPRECATION") 0
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            device.deviceId,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    fun open(device: UsbDevice): Boolean {
        close()
        val intf = interfaces(device).firstOrNull {
            (it.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE && hasBulkInOut(it)) ||
                (isKnownMf3010(device) && hasBulkInOut(it))
        } ?: return false
        val conn = usbManager.openDevice(device) ?: return false
        if (!conn.claimInterface(intf, true)) {
            conn.close()
            return false
        }
        connection = conn
        interfaceClaimed = intf
        return true
    }

    fun connection(): UsbDeviceConnection? = connection
    fun usbInterface(): UsbInterface? = interfaceClaimed

    fun close() {
        interfaceClaimed?.let { connection?.releaseInterface(it) }
        connection?.close()
        interfaceClaimed = null
        connection = null
    }
}
