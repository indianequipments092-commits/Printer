from pathlib import Path

main_path = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
manifest_path = Path('ScannerApp/app/src/main/AndroidManifest.xml')
reader_path = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PdfReaderActivity.kt')

main = main_path.read_text(encoding='utf-8')
manifest = manifest_path.read_text(encoding='utf-8')

old_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)'''
new_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)\n        navButton("▤", "PDF Reader", 4)'''
if 'navButton("▤", "PDF Reader", 4)' not in main:
    if old_nav not in main: raise SystemExit('navigation block not found')
    main = main.replace(old_nav, new_nav, 1)

old_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            else -> renderTools()\n        }'''
new_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            3 -> renderTools()\n            4 -> renderPdfReaderHome()\n        }'''
if '4 -> renderPdfReaderHome()' not in main:
    if old_when not in main: raise SystemExit('tab switch block not found')
    main = main.replace(old_when, new_when, 1)

if 'private fun renderPdfReaderHome()' not in main:
    marker = '    private fun renderTools() {'
    reader_home = '''    private fun renderPdfReaderHome() {\n        content.addView(title("PDF READER", "ADVANCED LOCAL PDF VIEWER • WHATSAPP READY"))\n        val hero = card()\n        hero.addView(TextView(this).apply {\n            text = "READ PDF DOCUMENTS"\n            textSize = 14f\n            typeface = Typeface.DEFAULT_BOLD\n            setTextColor(Color.rgb(130,160,200))\n        })\n        hero.addView(TextView(this).apply {\n            text = "Open any PDF from your phone, WhatsApp, Files or another app."\n            textSize = 20f\n            typeface = Typeface.DEFAULT_BOLD\n            setTextColor(Color.WHITE)\n            setPadding(0,dp(8),0,dp(4))\n        })\n        hero.addView(TextView(this).apply {\n            text = "Multi-page reading • Page jump • Zoom • Rotate • Share"\n            textSize = 12f\n            setTextColor(Color.rgb(150,165,185))\n        })\n        hero.addView(actionButton("OPEN PDF") {\n            startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {\n                addCategory(Intent.CATEGORY_OPENABLE)\n                type = "application/pdf"\n            })\n        }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0,dp(18),0,0) })\n        content.addView(hero, margin(0,12))\n        val info = card()\n        info.addView(section("ADVANCED READER"))\n        listOf(\n            "✓ Open PDFs received from WhatsApp and other apps",\n            "✓ Previous / next page and direct page jump",\n            "✓ Zoom in / zoom out and fit-to-screen",\n            "✓ Rotate pages while reading",\n            "✓ Share the original PDF without re-scanning",\n            "✓ Works locally without automatic upload"\n        ).forEach { line -> info.addView(TextView(this).apply {\n            text = line; textSize = 13f; setTextColor(Color.rgb(185,198,215)); setPadding(0,dp(7),0,dp(7))\n        }) }\n        content.addView(info, margin(0,10))\n        content.addView(actionButton("OPEN FROM WHATSAPP / FILES") {\n            startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {\n                addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"\n            })\n        }, margin(0,10))\n    }\n\n'''
    if marker not in main: raise SystemExit('renderTools marker not found')
    main = main.replace(marker, reader_home + marker, 1)

manifest_activity = '''        <activity android:name=".PdfReaderActivity" android:exported="true">\n            <intent-filter>\n                <action android:name="android.intent.action.VIEW" />\n                <category android:name="android.intent.category.DEFAULT" />\n                <category android:name="android.intent.category.BROWSABLE" />\n                <data android:mimeType="application/pdf" />\n            </intent-filter>\n        </activity>\n'''
if '.PdfReaderActivity' not in manifest:
    anchor = '        <activity android:name=".LibraryActivity" android:exported="false" />\n'
    if anchor not in manifest: raise SystemExit('manifest anchor not found')
    manifest = manifest.replace(anchor, manifest_activity + anchor, 1)

reader = r'''package com.indianequipments.usbscanner

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
        val scroll = ScrollView(this).apply { fillViewport=true; setBackgroundColor(Color.rgb(18,24,34)) }
        val center = FrameLayout(this).apply { setPadding(dp(8),dp(8),dp(8),dp(8)); gravity=Gravity.CENTER }
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
'''
reader_path.write_text(reader, encoding='utf-8')
main_path.write_text(main, encoding='utf-8')
manifest_path.write_text(manifest, encoding='utf-8')
print('PDF Reader patch ready')
