package com.indianequipments.usbscanner

import android.graphics.Bitmap

interface ScanEngine {
    suspend fun scan(): Bitmap
}

/**
 * Entry point for verified scanner backends. A backend must be selected for
 * the exact scanner model before scan() is enabled. This prevents unsafe or
 * incorrect USB commands from being sent to an unknown device.
 */
class UnsupportedScannerBackend : ScanEngine {
    override suspend fun scan(): Bitmap {
        throw UnsupportedOperationException(
            "No model-specific scanner backend is installed for this device."
        )
    }
}
