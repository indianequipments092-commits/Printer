package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
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
    private var currentPage = 0
    private var zoom = 1f
    private var rotation = 0f
    private lateinit var pageImage: ImageView
    private lateinit var pageLabel: TextView
    private lateinit var zoomLabel: TextView
    private lateinit var loading: ProgressBar

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(8,12,20)
        window.navigationBarColor = Color.rgb(8,12,20)
        buildUi()
        val incoming = intent?.data
        if (incoming != null) openUri(incoming) else choosePdf()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        closePdf()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(8,12,20)) }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10),dp(8),dp(10),dp(8)); background = panel() }
        toolbar.addView(button("‹",42) { finish() })
        toolbar.addView(TextView(this).apply { text="PDF Reader"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(dp(6),0,dp(6),0) }, LinearLayout.LayoutParams(0,dp(48),1f))
        toolbar.addView(button("↗",42) { sharePdf() })
        toolbar.addView(button("＋",42) { choosePdf() })
        root.addView(toolbar)
        val info = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(8),dp(14),dp(8)) }
        pageLabel = TextView(this).apply { text="Page — / —"; textSize=13f; setTextColor(Color.rgb(170,185,205)) }
        zoomLabel = TextView(this).apply { text="100%"; textSize=13f; setTextColor(Color.rgb(170,185,205)) }
        info.addView(pageLabel, LinearLayout.LayoutParams(0,-2,1f)); info.addView(zoomLabel); root.addView(info)
        val scroll = ScrollView(this).apply { setFillViewport(true); setBackgroundColor(Color.rgb(18,24,34)) }
        val center = FrameLayout(this).apply { setPadding(dp(8),dp(8),dp(8),dp(8)) }
        pageImage = ImageView(this).apply { adjustViewBounds=true; scaleType=ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE) }
        center.addView(pageImage, FrameLayout.LayoutParams(-1,-2,Gravity.CENTER))
        loading = ProgressBar(this).apply { visibility=View.GONE }
        center.addView(loading, FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER)); scroll.addView(center)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        val controls = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(8),dp(8),dp(8),dp(8)); background=panel() }
        val pageRow = LinearLayout(this).apply { gravity=Gravity.CENTER }
        pageRow.addView(button("‹",52) { showPage(currentPage-1) }); pageRow.addView(button("PAGE",64) { showPageJump() }); pageRow.addView(button("›",52) { showPage(currentPage+1) }); controls.addView(pageRow)
        val zoomRow = LinearLayout(this).apply { gravity=Gravity.CENTER }
        zoomRow.addView(button("−",52) { setZoom(zoom-0.1f) }); zoomRow.addView(button("FIT",64) { setZoom(1f) }); zoomRow.addView(button("+",52) { setZoom(zoom+0.1f) }); zoomRow.addView(button("↻",52) { rotation+=90f; pageImage.rotation=rotation }); controls.addView(zoomRow)
        root.addView(controls); setContentView(root)
    }

    private fun choosePdf() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/pdf" },9001) }
    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==9001 && resultCode==RESULT_OK && data?.data!=null) openUri(data.data!!) }

    private fun openUri(uri:Uri) {
        sourceUri=uri; loading.visibility=View.VISIBLE
        executor.execute {
            try {
                val fd = try { contentResolver.openFileDescriptor(uri,"r") } catch(_:Exception){ null }
                val local = if(fd==null) copyToCache(uri) else null
                runOnUiThread { closePdf(); descriptor=fd ?: ParcelFileDescriptor.open(local,ParcelFileDescriptor.MODE_READ_ONLY); tempFile=local; renderer=android.graphics.pdf.PdfRenderer(descriptor!!); currentPage=0; zoom=1f; rotation=0f; pageImage.rotation=0f; renderCurrentPage() }
            } catch(_:Throwable) { runOnUiThread { loading.visibility=View.GONE; Toast.makeText(this,"Unable to open this PDF",Toast.LENGTH_LONG).show() } }
        }
    }

    private fun copyToCache(uri:Uri):File { val file=File(cacheDir,"pdf_${System.currentTimeMillis()}.pdf"); contentResolver.openInputStream(uri).use { input -> requireNotNull(input); FileOutputStream(file).use { out -> input.copyTo(out) } }; return file }

    private fun renderCurrentPage() {
        val r=renderer ?: return; if(currentPage !in 0 until r.pageCount) return; loading.visibility=View.VISIBLE
        executor.execute {
            try {
                r.openPage(currentPage).use { page ->
                    val scale=max(1f,1500f/page.width.toFloat())
                    val bitmap=Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE); page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    runOnUiThread { pageImage.setImageBitmap(bitmap); pageLabel.text="Page ${currentPage+1} / ${r.pageCount}"; zoomLabel.text="${(zoom*100).toInt()}%"; pageImage.scaleX=zoom; pageImage.scaleY=zoom; loading.visibility=View.GONE }
                }
            } catch(_:Throwable) { runOnUiThread { loading.visibility=View.GONE; Toast.makeText(this,"Could not render this page",Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun showPage(page:Int) { val r=renderer ?: return; if(page !in 0 until r.pageCount) return; currentPage=page; renderCurrentPage() }
    private fun showPageJump() {
        val r=renderer ?: return; val input=EditText(this).apply { inputType=android.text.InputType.TYPE_CLASS_NUMBER; hint="1 - ${r.pageCount}"; setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Go to page").setView(input).setNegativeButton("Cancel",null).setPositiveButton("GO"){_,_-> input.text.toString().toIntOrNull()?.let{showPage(it-1)} }.show()
    }
    private fun setZoom(value:Float) { zoom=value.coerceIn(0.5f,2.5f); pageImage.scaleX=zoom; pageImage.scaleY=zoom; zoomLabel.text="${(zoom*100).toInt()}%" }
    private fun sharePdf() { val uri=sourceUri ?: return; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="application/pdf"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },"Share PDF")) }
    private fun closePdf() { renderer?.close(); renderer=null; descriptor?.close(); descriptor=null; tempFile?.delete(); tempFile=null }
    private fun panel()=android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(15,22,32)); cornerRadius=dp(18).toFloat(); setStroke(dp(1),Color.rgb(35,48,65)) }
    private fun button(label:String,width:Int,click:()->Unit)=Button(this).apply { text=label; textSize=12f; setTextColor(Color.WHITE); setOnClickListener{click()}; minWidth=dp(width); minimumWidth=dp(width); background=android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(30,40,55)); cornerRadius=dp(14).toFloat() } }
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}