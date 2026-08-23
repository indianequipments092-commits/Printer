from pathlib import Path

PRINT = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PrintExActivity.kt')
s = PRINT.read_text(encoding='utf-8')

# Context is already imported by the current Printex activity. Only inject
# UsbManager here; adding Context again makes Kotlin report an ambiguous import.
if 'import android.hardware.usb.UsbManager' not in s:
    s = s.replace('import android.graphics.pdf.PdfRenderer\n', 'import android.graphics.pdf.PdfRenderer\nimport android.hardware.usb.UsbManager\n', 1)

# Keep the existing Printex implementation as the source of truth. These
# guarded edits are retained for older source revisions used by the workflow.
if 'private lateinit var printerStatusView: TextView' not in s:
    s = s.replace('private var generatedPrintFile: File? = null', 'private var generatedPrintFile: File? = null\n    private lateinit var printerStatusView: TextView\n    private val prefs by lazy { getSharedPreferences("printex_prefs", MODE_PRIVATE) }', 1)

s = s.replace('setPadding(dp(18), dp(38), dp(18), dp(16))', 'setPadding(dp(18), dp(40), dp(18), dp(16))', 1)
s = s.replace('shell.addView(ScrollView(this).apply { isFillViewport = true; addView(root) }, LinearLayout.LayoutParams(-1, 0, 1f))\n        setContentView(shell)', 'shell.addView(ScrollView(this).apply { isFillViewport = true; addView(root) }, LinearLayout.LayoutParams(-1, 0, 1f))\n        shell.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, dp(78)))\n        setContentView(shell)', 1)

old_status = '''        status.addView(TextView(this).apply { text = "●  Android Print System"; textSize = 18f; setTypeface(null, 1); setTextColor(Color.rgb(75,220,145)); setPadding(0,dp(6),0,0) })\n        status.addView(TextView(this).apply { text = "Print settings are applied to the print job"; textSize = 12f; setTextColor(MUTED); setPadding(0,dp(4),0,0) })'''
new_status = '''        printerStatusView = TextView(this).apply {\n            textSize = 18f\n            setTypeface(null, 1)\n            setPadding(0,dp(6),0,0)\n        }\n        status.addView(printerStatusView)\n        status.addView(TextView(this).apply { text = "Live USB printer detection • print settings apply to the job"; textSize = 12f; setTextColor(MUTED); setPadding(0,dp(4),0,0) })'''
if old_status in s:
    s = s.replace(old_status, new_status, 1)

s = s.replace('settingRow(settings,"Printer","System / USB printer") { printerChooser() }', 'settingRow(settings,"Printer",printerDisplayName()) { printerChooser() }', 1)

old_badge = 'header.addView(TextView(this).apply { text = "PRO"; textSize = 10f; setTextColor(BLUE); gravity = Gravity.CENTER; background = rounded(PANEL, 10); setPadding(dp(10),dp(5),dp(10),dp(5)) })'
new_badge = 'header.addView(TextView(this).apply { text = "⋮"; textSize = 28f; setTextColor(MUTED); gravity = Gravity.CENTER; setPadding(dp(8),0,dp(4),0); setOnClickListener { showMoreMenu() } })'
if old_badge in s:
    s = s.replace(old_badge, new_badge, 1)

if 'override fun onResume()' not in s:
    marker = '    override fun onNewIntent(i: Intent?) {\n        super.onNewIntent(i)\n        setIntent(i)\n        handleIntent(i)\n    }\n'
    addition = marker + '\n    override fun onResume() {\n        super.onResume()\n        if (::printerStatusView.isInitialized) refreshPrinterStatus()\n    }\n'
    if marker in s:
        s = s.replace(marker, addition, 1)

s = s.replace('            pm.print("Printex • ${fileName.text}",object:PrintDocumentAdapter(){', '            recordPrintHistory(file.name)\n            pm.print("Printex • ${fileName.text}",object:PrintDocumentAdapter(){', 1)

old_bitmap_add = 'bitmaps.add(transformBitmap(b))'
new_bitmap_add = 'bitmaps.add(if(autoRotate) autoRotateBitmap(transformBitmap(b)) else transformBitmap(b))'
s = s.replace(old_bitmap_add, new_bitmap_add)

# Fix Kotlin smart-cast restrictions by never using the mutable bitmap property
# directly at the PdfRenderer call site.
old_pdf_render = 'page!!.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)'
new_pdf_render = 'bitmap?.let { renderedBitmap -> page!!.render(renderedBitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }'
if old_pdf_render in s:
    s = s.replace(old_pdf_render, new_pdf_render, 1)
# Also repair the intermediate form from the previous failed patch if present.
s = s.replace('val renderedBitmap = bitmap ?: return; page!!.render(renderedBitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)', new_pdf_render, 1)

# The current Printex implementation already provides its own bottom navigation,
# printer status and settings. Do not inject the legacy helper block into it.
if 'private fun buildBottomNav()' not in s and 'private fun navBar()' not in s:
    anchor = '    private fun handleIntent(i: Intent?) {'
    block = r'''    private fun buildBottomNav(): View {
        val nav = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(8),dp(6),dp(8),dp(6)); background=rounded(PANEL,20) }
        nav.addView(navButton("⌂", "Home") { openScannerTab(0) }, navWeight())
        nav.addView(navButton("◎", "Studio") { openScannerTab(1) }, navWeight())
        nav.addView(navButton("▦", "Library") { openScannerTab(2) }, navWeight())
        nav.addView(navButton("▣", "Printex") { }, navWeight())
        return nav
    }
    private fun navButton(icon:String,label:String,click:()->Unit)=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setOnClickListener{click()}
        addView(TextView(this@PrintExActivity).apply{text=icon;textSize=20f;gravity=Gravity.CENTER;setTextColor(if(label=="Printex") BLUE else MUTED)})
        addView(TextView(this@PrintExActivity).apply{text=label;textSize=11f;gravity=Gravity.CENTER;setTextColor(if(label=="Printex") BLUE else MUTED)})
    }
    private fun navWeight()=LinearLayout.LayoutParams(0,-1,1f)
    private fun openScannerTab(tab:Int){ startActivity(Intent(this,MainActivity::class.java).putExtra("open_tab",tab)); finish() }
    private fun connectedUsbPrinter(): android.hardware.usb.UsbDevice? {
        val manager = getSystemService(Context.USB_SERVICE) as? UsbManager ?: return null
        return manager.deviceList.values.firstOrNull { device -> device.deviceClass == 7 || (0 until device.interfaceCount).any { device.getInterface(it).interfaceClass == 7 } }
    }
    private fun printerDisplayName(): String {
        val d = connectedUsbPrinter() ?: return "Printer is not connected"
        val name = d.productName?.takeIf { it.isNotBlank() } ?: d.manufacturerName?.takeIf { it.isNotBlank() } ?: d.deviceName
        return "$name Connected"
    }
    private fun refreshPrinterStatus() {
        if (!::printerStatusView.isInitialized) return
        val d = connectedUsbPrinter()
        if (d == null) { printerStatusView.text = "Printer is not connected"; printerStatusView.setTextColor(Color.rgb(255,170,90)) }
        else { val name = d.productName?.takeIf { it.isNotBlank() } ?: d.manufacturerName?.takeIf { it.isNotBlank() } ?: d.deviceName; printerStatusView.text = "● $name Connected"; printerStatusView.setTextColor(Color.rgb(75,220,145)) }
    }
    private fun showMoreMenu() {
        val items = arrayOf("Printer Management", "Print Queue", "Print History", "Default Print Settings", "Printer Diagnostics", "App Settings")
        AlertDialog.Builder(this).setTitle("Printex Menu").setItems(items) { _, which -> when (which) { 0 -> printerChooser(); 1 -> printerChooser(); 2 -> showPrintHistory(); 3 -> showDefaultPrintSettings(); 4 -> showPrinterDiagnostics(); 5 -> showAppSettings() } }.show()
    }
    private fun recordPrintHistory(fileName: String) {
        val current = prefs.getStringSet("history", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add("${System.currentTimeMillis()} • $fileName • $copies copies • $paperSize • $orientation • $colorMode • $scale")
        prefs.edit().putStringSet("history", current.takeLast(20).toSet()).apply()
    }
    private fun showPrintHistory() { val history = prefs.getStringSet("history", emptySet())?.toList()?.sortedDescending() ?: emptyList(); AlertDialog.Builder(this).setTitle("Print History").setMessage(if (history.isEmpty()) "No print jobs recorded yet." else history.joinToString("\n\n")).setPositiveButton("OK", null).show() }
    private fun showDefaultPrintSettings() { AlertDialog.Builder(this).setTitle("Default Print Settings").setMessage("Paper: $paperSize\nOrientation: $orientation\nColor: $colorMode\nCopies: $copies\nScale: $scale\nDuplex: $duplex").setPositiveButton("OK", null).show() }
    private fun showPrinterDiagnostics() { val d = connectedUsbPrinter(); val message = if (d == null) "Printer is not connected.\nConnect the USB printer and try again." else "Printer detected.\nName: ${d.productName ?: d.deviceName}\nUSB VID: ${d.vendorId}\nUSB PID: ${d.productId}\nInterfaces: ${d.interfaceCount}"; AlertDialog.Builder(this).setTitle("Printer Diagnostics").setMessage(message).setPositiveButton("OK", null).show() }
    private fun showAppSettings() { AlertDialog.Builder(this).setTitle("Printex App Settings").setMessage("Preview opens at fit-to-page. Pinch to zoom, drag to inspect, and use page controls to move between pages. Print settings are applied to the generated print document.").setPositiveButton("OK", null).show() }

'''
    if anchor in s:
        s = s.replace(anchor, block + anchor, 1)

start = s.find('class ZoomImageView(')
if start != -1:
    s = s[:start] + r'''class ZoomImageView(context:android.content.Context):android.widget.ImageView(context){
    private val matrix=Matrix(); private var fitScale=1f; private var currentScale=1f; private var lastX=0f; private var lastY=0f; private var dragging=false
    private val detector=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){ override fun onScale(d:ScaleGestureDetector):Boolean{ val next=(currentScale*d.scaleFactor).coerceIn(fitScale,fitScale*10f); val applied=next/currentScale; currentScale=next; matrix.postScale(applied,applied,d.focusX,d.focusY); imageMatrix=matrix; return true } })
    init{scaleType=ScaleType.MATRIX;setBackgroundColor(Color.rgb(18,24,34));isClickable=true}
    override fun setImageBitmap(bm:Bitmap?){super.setImageBitmap(bm);post{resetZoom()}}
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);post{if(drawable!=null)resetZoom()}}
    override fun onTouchEvent(event:MotionEvent):Boolean{ detector.onTouchEvent(event); when(event.actionMasked){ MotionEvent.ACTION_DOWN->{lastX=event.x;lastY=event.y;dragging=true;return true}; MotionEvent.ACTION_MOVE->{if(dragging&&!detector.isInProgress){matrix.postTranslate(event.x-lastX,event.y-lastY);imageMatrix=matrix;lastX=event.x;lastY=event.y};return true}; MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{dragging=false;return true} }; return true }
    fun resetZoom(){ val d=drawable?:return; val vw=(width-paddingLeft-paddingRight).coerceAtLeast(1).toFloat(); val vh=(height-paddingTop-paddingBottom).coerceAtLeast(1).toFloat(); fitScale=minOf(vw/d.intrinsicWidth.toFloat(),vh/d.intrinsicHeight.toFloat()).coerceAtLeast(0.01f); currentScale=fitScale; val dx=(vw-d.intrinsicWidth*fitScale)/2f+paddingLeft; val dy=(vh-d.intrinsicHeight*fitScale)/2f+paddingTop; matrix.reset();matrix.setScale(fitScale,fitScale);matrix.postTranslate(dx,dy);imageMatrix=matrix }
}
'''

PRINT.write_text(s, encoding='utf-8')
print('Applied safe Printex patch and fixed repeat-build Kotlin errors')
