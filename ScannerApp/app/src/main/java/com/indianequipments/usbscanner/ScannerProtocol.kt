package com.indianequipments.usbscanner

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/**
 * Scanner transport boundary. USB class 0x06 identifies imaging devices, but
 * the actual scan protocol is vendor/model specific. This class deliberately
 * does not send guessed commands to hardware.
 */
class ScannerProtocol(
    private val connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) {
    data class Capabilities(
        val hasBulkIn: Boolean,
        val hasBulkOut: Boolean,
        val hasInterruptIn: Boolean,
        val supported: Boolean,
        val note: String
    )

    fun probe(): Capabilities {
        val endpoints = (0 until usbInterface.endpointCount).map(usbInterface::getEndpoint)
        val bulkIn = endpoints.any { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
        val bulkOut = endpoints.any { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        val interruptIn = endpoints.any { it.type == UsbConstants.USB_ENDPOINT_XFER_INT && it.direction == UsbConstants.USB_DIR_IN }
        return Capabilities(
            hasBulkIn = bulkIn,
            hasBulkOut = bulkOut,
            hasInterruptIn = interruptIn,
            supported = bulkIn && bulkOut,
            note = if (bulkIn && bulkOut) "USB imaging transport detected; model-specific scan commands still need to be selected." else "USB imaging interface has no usable bulk IN/OUT pair."
        )
    }

    fun bulkInEndpoint(): UsbEndpoint? = (0 until usbInterface.endpointCount)
        .map(usbInterface::getEndpoint)
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }

    fun bulkOutEndpoint(): UsbEndpoint? = (0 until usbInterface.endpointCount)
        .map(usbInterface::getEndpoint)
        .firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }

    /** Low-level transfer helper for a verified device-specific backend. */
    fun transferOut(data: ByteArray, timeoutMs: Int = 3000): Int {
        val endpoint = bulkOutEndpoint() ?: return -1
        return connection.bulkTransfer(endpoint, data, data.size, timeoutMs)
    }

    fun transferIn(buffer: ByteArray, timeoutMs: Int = 3000): Int {
        val endpoint = bulkInEndpoint() ?: return -1
        return connection.bulkTransfer(endpoint, buffer, buffer.size, timeoutMs)
    }
}
