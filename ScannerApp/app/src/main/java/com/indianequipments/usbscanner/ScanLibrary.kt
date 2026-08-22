package com.indianequipments.usbscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

class ScanLibrary(private val context: Context) {
    private val root = File(context.getExternalFilesDir(null), "Scans").apply { mkdirs() }

    fun savePage(bitmap: Bitmap, name: String = "scan_${System.currentTimeMillis()}.jpg"): File {
        val file = File(root, name.removeSuffix(".png").removeSuffix(".jpeg").removeSuffix(".jpg") + ".jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        return file
    }

    fun list(): List<File> = root.listFiles { f -> f.isFile && f.extension.lowercase() in setOf("jpg", "jpeg", "png") }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun decode(file: File): Bitmap? = BitmapFactory.decodeFile(file.absolutePath)

    fun delete(file: File) { if (file.exists()) file.delete() }

    fun applyAdjustments(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val c = (contrast + 1f).coerceAtLeast(0.05f)
        val translate = (-0.5f * c + 0.5f) * 255f + brightness * 255f
        val matrix = ColorMatrix(floatArrayOf(
            c,0f,0f,0f,translate, 0f,c,0f,0f,translate, 0f,0f,c,0f,translate, 0f,0f,0f,1f,0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
