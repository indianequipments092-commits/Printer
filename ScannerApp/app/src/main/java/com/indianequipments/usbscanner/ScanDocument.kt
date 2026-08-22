package com.indianequipments.usbscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class ScanDocument(private val context: Context) {
    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun grayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun savePng(bitmap: Bitmap, name: String = "scan_${System.currentTimeMillis()}.png"): File {
        val file = File(context.getExternalFilesDir(null), name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun savePdf(pages: List<Bitmap>, name: String = "scan_${System.currentTimeMillis()}.pdf"): File {
        require(pages.isNotEmpty())
        val pdf = PdfDocument()
        pages.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
            pdf.finishPage(page)
        }
        val file = File(context.getExternalFilesDir(null), name)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }
}
