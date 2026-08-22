package com.indianequipments.usbscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class ScanDocument(private val context: Context) {
    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun grayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix().apply { setSaturation(0f) }
            )
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun savePng(bitmap: Bitmap, name: String = "scan_${System.currentTimeMillis()}.png"): File {
        val file = File(context.getExternalFilesDir(null), name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun savePdf(
        pages: List<Bitmap>,
        dpis: List<Int>,
        name: String = "scan_${System.currentTimeMillis()}.pdf"
    ): File {
        require(pages.isNotEmpty())
        require(dpis.size == pages.size)

        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, bitmap ->
                val dpi = dpis[index].coerceAtLeast(75)
                val pageWidth = (bitmap.width * 72f / dpi).roundToInt().coerceAtLeast(1)
                val pageHeight = (bitmap.height * 72f / dpi).roundToInt().coerceAtLeast(1)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                val destination = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                page.canvas.drawBitmap(bitmap, null, destination, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                pdf.finishPage(page)
            }

            val file = File(context.getExternalFilesDir(null), name)
            FileOutputStream(file).use { pdf.writeTo(it) }
            return file
        } finally {
            pdf.close()
        }
    }
}
