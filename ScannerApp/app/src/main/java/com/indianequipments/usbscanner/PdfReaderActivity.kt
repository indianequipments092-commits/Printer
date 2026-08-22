package com.indianequipments.usbscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.max

class PdfReaderActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var renderer: android.graphics.pdf.PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var sourceUri: Uri? = null
    private var tempFile: File? = null
    private var zoom = 1f
    private var rotation = 0f
    private lateinit var pageScroll: ScrollView
    private lateinit var pageColumn: LinearLayout
    private lateinit var pageRail: LinearLayout
    private lateinit var pageLabel: TextView
    private lateinit var zoomLabel: TextView
    private lateinit var loading: ProgressBar
    private var pageViews = mutableListOf<ImageView>()
    private var scaleDetector: ScaleGestureDetector? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(8, 12, 20)
        window.navigationBarColor = Color.rgb(8, 12, 20)
        window.decorView.systemUiVisibility = 0
        buildUi()
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                setZoom(zoom * detector.scaleFactor)
                return true
            }
        })
        val incoming = intent?.data
        if (incoming != null) openUri(incoming) else choosePdf()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        closePdf()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 12, 20))
            setPadding(dp(0), dp(28), dp(0), dp(0))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = panel()
        }
        toolbar.addView(button("‹", 44) { finish() })
        toolbar.addView(TextView(this).apply {
            text = "PDF Reader"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(8), 0, dp(6), 0)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        toolbar.addView(button("↗", 44) { sharePdf() })
        toolbar.addView(button("＋", 44) { choosePdf() })
        root.addView(toolbar)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        pageLabel = TextView(this).apply {
            text = "Page 0 / 0"
            textSize = 13f
            setTextColor(Color.rgb(170, 185, 205))
        }
        zoomLabel = TextView(this).apply {
            text = "100% • Pinch to zoom"
            textSize = 13f
            setTextColor(Color.rgb(170, 185, 205))
        }
        info.addView(pageLabel, LinearLayout.LayoutParams(0, -2, 1f))
        info.addView(zoomLabel)
        root.addView(info)

        val viewerFrame = FrameLayout(this).apply { setBackgroundColor(Color.rgb(18, 24, 34)) }
        pageScroll = ScrollView(this).apply {
            isFillViewport = false
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setPadding(dp(8), dp(8), dp(72), dp(8))
            setOnTouchListener { _, event -> scaleDetector?.onTouchEvent(event); false }
        }
        pageColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        pageScroll.addView(pageColumn, LinearLayout.LayoutParams(-1, -2))
        viewerFrame.addView(pageScroll, FrameLayout.LayoutParams(-1, -1))

        val railScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(12, 18, 28))
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        pageRail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        railScroll.addView(pageRail, LinearLayout.LayoutParams(-1, -2))
        viewerFrame.addView(railScroll, FrameLayout.LayoutParams(dp(60), -1, Gravity.END))
        loading = ProgressBar(this).apply { visibility = View.GONE }
        viewerFrame.addView(loading, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER))
        root.addView(viewerFrame, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = panel()
        }
        bottom.addView(button("FIT", 72) { setZoom(1f) })
        bottom.addView(button("ROTATE", 84) {
            rotation = (rotation + 90f) % 360f
            pageViews.forEach { it.rotation = rotation }
        })
        bottom.addView(button("SHARE", 78) { sharePdf() })
        root.addView(bottom)
        setContentView(root)
    }

    private fun choosePdf() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }, 9001)
    }

    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9001 && resultCode == RESULT_OK && data?.data != null) openUri(data.data!!)
    }

    private fun openUri(uri: Uri) {
        sourceUri = uri
        loading.visibility = View.VISIBLE
        executor.execute {
            try {
                val fd = try { contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
                val local = if (fd == null) copyToCache(uri) else null
                runOnUiThread {
                    closePdf()
                    descriptor = fd ?: ParcelFileDescriptor.open(local, ParcelFileDescriptor.MODE_READ_ONLY)
                    tempFile = local
                    renderer = android.graphics.pdf.PdfRenderer(descriptor!!)
                    zoom = 1f
                    rotation = 0f
                    renderAllPages()
                }
            } catch (_: Throwable) {
                runOnUiThread {
                    loading.visibility = View.GONE
                    Toast.makeText(this, "Unable to open this PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun copyToCache(uri: Uri): File {
        val file = File(cacheDir, "pdf_${System.currentTimeMillis()}.pdf")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            FileOutputStream(file).use { out -> input.copyTo(out) }
        }
        return file
    }

    private fun renderAllPages() {
        val r = renderer ?: return
        loading.visibility = View.VISIBLE
        pageColumn.removeAllViews()
        pageRail.removeAllViews()
        pageViews.clear()
        pageLabel.text = "Page 1 / ${r.pageCount}"
        executor.execute {
            try {
                for (index in 0 until r.pageCount) {
                    val bitmap = r.openPage(index).use { page ->
                        val scale = max(1f, 1400f / page.width.toFloat())
                        val bitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                    runOnUiThread {
                        addPageView(index, bitmap)
                        if (index == r.pageCount - 1) {
                            loading.visibility = View.GONE
                            pageColumn.post { updatePageLabel() }
                        }
                    }
                }
            } catch (_: Throwable) {
                runOnUiThread {
                    loading.visibility = View.GONE
                    Toast.makeText(this, "Could not render this PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addPageView(index: Int, bitmap: Bitmap) {
        val image = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
            rotation = this@PdfReaderActivity.rotation
            setOnTouchListener { _, event -> scaleDetector?.onTouchEvent(event); false }
        }
        pageColumn.addView(image, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(4), dp(4), dp(4), dp(10))
        })
        pageViews.add(image)
        val number = Button(this).apply {
            text = (index + 1).toString()
            textSize = 11f
            setTextColor(Color.WHITE)
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, dp(8), 0, dp(8))
            background = rounded(Color.rgb(30, 40, 55), 12)
            setOnClickListener { scrollToPage(index) }
        }
        pageRail.addView(number, LinearLayout.LayoutParams(dp(48), dp(46)).apply {
            setMargins(0, dp(3), 0, dp(3))
        })
    }

    private fun scrollToPage(index: Int) {
        if (index !in pageViews.indices) return
        pageScroll.post { pageScroll.smoothScrollTo(0, max(0, pageViews[index].top)) }
        pageLabel.text = "Page ${index + 1} / ${pageViews.size}"
    }

    private fun updatePageLabel() {
        if (pageViews.isEmpty()) return
        val scrollY = pageScroll.scrollY
        var best = 0
        var bestDistance = Int.MAX_VALUE
        pageViews.forEachIndexed { i, view ->
            val distance = kotlin.math.abs(view.top - scrollY)
            if (distance < bestDistance) { bestDistance = distance; best = i }
        }
        pageLabel.text = "Page ${best + 1} / ${pageViews.size}"
        zoomLabel.text = "${(zoom * 100).toInt()}% • Pinch to zoom"
    }

    private fun setZoom(value: Float) {
        zoom = value.coerceIn(0.5f, 100f)
        pageColumn.pivotX = 0f
        pageColumn.pivotY = 0f
        pageColumn.scaleX = zoom
        pageColumn.scaleY = zoom
        zoomLabel.text = "${(zoom * 100).toInt()}% • Pinch to zoom"
    }

    private fun sharePdf() {
        val uri = sourceUri ?: return
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share PDF"))
        } catch (_: Throwable) {
            Toast.makeText(this, "Unable to share this PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closePdf() {
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
        tempFile?.delete()
        tempFile = null
    }

    private fun panel() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(15, 22, 32))
        cornerRadius = dp(18).toFloat()
        setStroke(dp(1), Color.rgb(35, 48, 65))
    }

    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun button(label: String, width: Int, click: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(Color.WHITE)
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(8), dp(8), dp(8), dp(8))
        background = rounded(Color.rgb(30, 40, 55), 14)
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(dp(width), dp(48)).apply { setMargins(dp(4), 0, dp(4), 0) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
