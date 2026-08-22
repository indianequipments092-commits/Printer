package com.indianequipments.usbscanner

import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import kotlin.math.min

/**
 * Canon i-SENSYS MF3010 USB scan transport.
 *
 * The MF3010 is a Canon imageCLASS/i-SENSYS generation-2 scanner and uses the
 * iClass/Pixma-family bulk protocol. The implementation follows the published
 * SANE pixma_imageclass backend for this exact VID/PID instead of sending
 * guessed commands.
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

    data class ScanConfig(
        val dpi: Int = 300,
        val color: Boolean = true
    )

    data class ScanResult(
        val bitmap: Bitmap,
        val dpi: Int,
        val color: Boolean,
        val width: Int,
        val height: Int,
        val bytesReceived: Long
    )

    private data class ImageBlock(val info: Int, val data: ByteArray)

    companion object {
        private const val CMD_START_SESSION = 0xDB20
        private const val CMD_SELECT_SOURCE = 0xDD20
        private const val CMD_SCAN_PARAM = 0xDE20
        private const val CMD_STATUS = 0xF320
        private const val CMD_ABORT_SESSION = 0xEF20
        private const val CMD_READ_IMAGE2 = 0xD460

        private const val STATUS_OK = 0x0606
        private const val HEADER_LEN = 10
        private const val IMAGE_HEADER_LEN = 8
        private const val IMAGE_RESPONSE_LEN = 512
        private const val MAX_IMAGE_READ = 16 * 1024
        private const val COMMAND_TIMEOUT_MS = 8_000
        private const val IMAGE_TIMEOUT_MS = 15_000
        private const val SCANNER_WIDTH_AT_75_DPI = 640
        private const val SCANNER_HEIGHT_AT_75_DPI = 877
    }

    private val endpoints = (0 until usbInterface.endpointCount).map(usbInterface::getEndpoint)

    private val bulkIn: UsbEndpoint? = endpoints.firstOrNull {
        it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
            it.direction == UsbConstants.USB_DIR_IN
    }

    private val bulkOut: UsbEndpoint? = endpoints.firstOrNull {
        it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
            it.direction == UsbConstants.USB_DIR_OUT
    }

    private val interruptIn: UsbEndpoint? = endpoints.firstOrNull {
        it.type == UsbConstants.USB_ENDPOINT_XFER_INT &&
            it.direction == UsbConstants.USB_DIR_IN
    }

    fun probe(): Capabilities {
        val supported = bulkIn != null && bulkOut != null
        return Capabilities(
            hasBulkIn = bulkIn != null,
            hasBulkOut = bulkOut != null,
            hasInterruptIn = interruptIn != null,
            supported = supported,
            note = if (supported) {
                "USB imaging transport ready; Canon MF3010 scan protocol is available."
            } else {
                "USB imaging interface has no usable bulk IN/OUT pair."
            }
        )
    }

    fun bulkInEndpoint(): UsbEndpoint? = bulkIn
    fun bulkOutEndpoint(): UsbEndpoint? = bulkOut

    /** Low-level verified bulk transfer helper. */
    fun transferOut(data: ByteArray, timeoutMs: Int = COMMAND_TIMEOUT_MS): Int {
        val endpoint = bulkOut ?: return -1
        return connection.bulkTransfer(endpoint, data, data.size, timeoutMs)
    }

    /** Low-level verified bulk transfer helper. */
    fun transferIn(buffer: ByteArray, timeoutMs: Int = COMMAND_TIMEOUT_MS): Int {
        val endpoint = bulkIn ?: return -1
        return connection.bulkTransfer(endpoint, buffer, buffer.size, timeoutMs)
    }

    fun scan(
        config: ScanConfig,
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): ScanResult {
        require(config.dpi in setOf(75, 150, 300, 600)) { "Unsupported DPI" }
        require(bulkIn != null && bulkOut != null) { "Scanner bulk endpoints are unavailable" }

        val channels = if (config.color) 3 else 1
        val width = SCANNER_WIDTH_AT_75_DPI * config.dpi / 75
        val height = SCANNER_HEIGHT_AT_75_DPI * config.dpi / 75
        val rawWidth = alignUp(width, 32)
        val lineSize = rawWidth * channels
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val rowPixels = IntArray(width)
        val pending = ByteArray(lineSize)
        var pendingLen = 0
        var rowsWritten = 0
        var bytesReceived = 0L
        var lastBlock = false

        try {
            onProgress(2, "Preparing scanner…")
            queryStatus()

            executeSimple(CMD_START_SESSION)
            executeSelectSource()
            executeScanParams(config.dpi, width, height, rawWidth, channels)

            onProgress(5, "Scanner warming up…")

            // The first image request is a generation-2 request. Preserve any
            // image bytes returned with the header; normally this first reply
            // is only the 8-byte header.
            var block = requestImageBlock(0)
            lastBlock = (block.info and 0x38) != 0

            while (true) {
                val data = block.data
                var offset = 0
                while (offset < data.size) {
                    val copy = min(lineSize - pendingLen, data.size - offset)
                    System.arraycopy(data, offset, pending, pendingLen, copy)
                    pendingLen += copy
                    offset += copy
                    bytesReceived += copy

                    while (pendingLen >= lineSize && rowsWritten < height) {
                        decodeRow(
                            pending,
                            width,
                            channels,
                            rowPixels
                        )
                        bitmap.setPixels(rowPixels, 0, width, 0, rowsWritten, width, 1)
                        rowsWritten++
                        pendingLen -= lineSize
                        if (pendingLen > 0) {
                            System.arraycopy(pending, lineSize, pending, 0, pendingLen)
                        }
                        val percent = 5 + ((rowsWritten * 90) / height)
                        if (rowsWritten == 1 || rowsWritten % 16 == 0 || rowsWritten == height) {
                            onProgress(percent.coerceAtMost(95), "Scanning… $rowsWritten / $height lines")
                        }
                    }
                }

                if (rowsWritten >= height || lastBlock) break
                block = requestImageBlock(4)
                lastBlock = (block.info and 0x38) != 0
            }

            if (rowsWritten == 0) {
                throw ScannerException("The scanner returned no image data.")
            }

            onProgress(98, "Finishing scan…")
            onProgress(100, "Scan complete")
            return ScanResult(bitmap, config.dpi, config.color, width, height, bytesReceived)
        } catch (t: Throwable) {
            bitmap.recycle()
            throw t
        } finally {
            finishSession()
        }
    }

    private fun decodeRow(raw: ByteArray, width: Int, channels: Int, out: IntArray) {
        if (channels == 1) {
            for (x in 0 until width) {
                val g = raw[x].toInt() and 0xFF
                out[x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
        } else {
            for (x in 0 until width) {
                val p = x * 3
                val r = raw[p].toInt() and 0xFF
                val g = raw[p + 1].toInt() and 0xFF
                val b = raw[p + 2].toInt() and 0xFF
                out[x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun alignUp(value: Int, alignment: Int): Int =
        ((value + alignment - 1) / alignment) * alignment

    private fun executeSimple(command: Int) {
        val packet = makeCommand(command, 0)
        val response = transact(packet, 2)
        checkStatus(response)
    }

    private fun executeSelectSource() {
        val packet = makeCommand(CMD_SELECT_SOURCE, 10)
        packet[HEADER_LEN] = 1 // flatbed
        packet[HEADER_LEN + 5] = 0
        finalizeChecksum(packet)
        checkStatus(transact(packet, 2))
    }

    private fun executeScanParams(
        dpi: Int,
        width: Int,
        height: Int,
        rawWidth: Int,
        channels: Int
    ) {
        val packet = makeCommand(CMD_SCAN_PARAM, 0x2E)
        val data = HEADER_LEN
        putBe16((dpi or 0x1000), packet, data + 0x04)
        putBe16((dpi or 0x1000), packet, data + 0x06)
        putBe32(0, packet, data + 0x08)
        putBe32(0, packet, data + 0x0C)
        putBe32(rawWidth, packet, data + 0x10)
        putBe32(height, packet, data + 0x14)
        packet[data + 0x18] = if (channels == 1) 0x04 else 0x08
        packet[data + 0x19] = (channels * 8).toByte()
        packet[data + 0x1F] = 0x7F.toByte()
        packet[data + 0x20] = 0xFF.toByte()
        packet[data + 0x23] = 0x81.toByte()
        finalizeChecksum(packet)
        checkStatus(transact(packet, 2))
    }

    private fun requestImageBlock(flag: Int): ImageBlock {
        val packet = ByteArray(11)
        putBe16(CMD_READ_IMAGE2, packet, 0)
        packet[8] = flag.toByte()
        packet[10] = 0x06

        val response = transact(packet, IMAGE_RESPONSE_LEN)
        if (response.size < IMAGE_HEADER_LEN) {
            throw ScannerException("Invalid image response from MF3010 (only ${response.size} bytes).")
        }
        checkStatus(response)

        val info = response[2].toInt() and 0xFF
        val initialLength = response.size - IMAGE_HEADER_LEN
        val remaining = if (response.size >= IMAGE_RESPONSE_LEN) {
            val total = getBe32(response, 4).toLong()
            val rest = total - initialLength
            if (rest < 0 || rest > 0x200000) {
                throw ScannerException("Invalid image block length reported by scanner: $total")
            }
            rest.toInt()
        } else {
            getBe16(response, 6)
        }

        val image = ByteArray(initialLength + remaining)
        if (initialLength > 0) {
            System.arraycopy(response, IMAGE_HEADER_LEN, image, 0, initialLength)
        }
        if (remaining > 0) {
            readFully(image, initialLength, remaining, IMAGE_TIMEOUT_MS)
        }
        return ImageBlock(info, image)
    }

    private fun queryStatus() {
        val response = transact(makeCommand(CMD_STATUS, 0), 14)
        checkStatus(response)
    }

    private fun finishSession() {
        runCatching { executeSimple(CMD_ABORT_SESSION) }
        runCatching { queryStatus() }
    }

    private fun makeCommand(command: Int, dataLength: Int): ByteArray {
        val packet = ByteArray(HEADER_LEN + dataLength)
        putBe16(command, packet, 0)
        putBe16(dataLength, packet, 7)
        return packet
    }

    private fun finalizeChecksum(packet: ByteArray) {
        if (packet.size <= HEADER_LEN) return
        var sum = 0
        for (i in HEADER_LEN until packet.size - 1) {
            sum = (sum + (packet[i].toInt() and 0xFF)) and 0xFF
        }
        packet[packet.lastIndex] = ((-sum) and 0xFF).toByte()
    }

    private fun transact(command: ByteArray, expectedLength: Int): ByteArray {
        val out = bulkOut ?: throw ScannerException("USB bulk OUT endpoint unavailable.")
        val input = bulkIn ?: throw ScannerException("USB bulk IN endpoint unavailable.")

        val written = connection.bulkTransfer(out, command, command.size, COMMAND_TIMEOUT_MS)
        if (written != command.size) {
            throw ScannerException("USB command write failed ($written/${command.size}).")
        }

        val response = ByteArray(expectedLength)
        var offset = 0
        var zeroReads = 0
        while (offset < expectedLength) {
            val n = connection.bulkTransfer(input, response, offset, expectedLength - offset, COMMAND_TIMEOUT_MS)
            if (n < 0) {
                throw ScannerException("Scanner response read failed (USB error $n).")
            }
            if (n == 0) {
                if (++zeroReads >= 3) throw ScannerException("Scanner response timed out.")
                continue
            }
            zeroReads = 0
            offset += n
        }
        return response
    }

    private fun readFully(buffer: ByteArray, offset: Int, length: Int, timeoutMs: Int) {
        val input = bulkIn ?: throw ScannerException("USB bulk IN endpoint unavailable.")
        var position = offset
        val end = offset + length
        while (position < end) {
            val request = min(MAX_IMAGE_READ, end - position)
            val n = connection.bulkTransfer(input, buffer, position, request, timeoutMs)
            if (n < 0) throw ScannerException("Image data read failed (USB error $n).")
            if (n == 0) throw ScannerException("Scanner stopped returning image data.")
            position += n
        }
    }

    private fun checkStatus(response: ByteArray) {
        if (response.size < 2) throw ScannerException("Scanner returned an empty response.")
        val status = getBe16(response, 0)
        if (status != STATUS_OK) {
            throw ScannerException("Canon scanner returned status 0x${status.toString(16)}.")
        }
    }

    private fun putBe16(value: Int, target: ByteArray, offset: Int) {
        target[offset] = (value ushr 8).toByte()
        target[offset + 1] = value.toByte()
    }

    private fun putBe32(value: Int, target: ByteArray, offset: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun getBe16(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xFF) shl 8) or (source[offset + 1].toInt() and 0xFF)

    private fun getBe32(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF)
}

class ScannerException(message: String) : Exception(message)
