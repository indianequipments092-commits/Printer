package com.indianequipments.usbscanner

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PrintExActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var fileName: TextView
    private lateinit var pageInfo: TextView
    private var sourceFile: File? = null
    private var mimeType = "application/pdf"
    private var pageCount = 0
    private var currentPage = 0
    private var previewBitmap: Bitmap? = null
    private var copies = 1
    private var colorMode = "Color"
    private var paperSize = "A4"
    private var orientation = "Portrait"
    private var duplex = "Off"
    private var scaleMode = "Fit to Page"
    private var pagesPerSheet = 1
    private var pageRange = "All"
    private var quality = "High"
    private var margins = "Default"
    private var reverseOrder = false
    private var brightness = 0
    private var contrast = 100
    private var autoRotate = true
    private var generatedPrintFile: File? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        buildUi()
        handleIntent(intent)
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        setIntent(i)
        handleIntent(i)
    }

    private fun buildUi() {
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(38), dp(18), dp(16)) }
        shell.addView(ScrollView(this).apply { isFillViewport = true; addView(root) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(shell)

        val header = row()
        header.addView(TextView(this).apply { text = "PRINTEX"; textSize = 28f; setTypeface(null, 1); setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply { text = "PRO"; textSize = 10f; setTextColor(BLUE); gravity = Gravity.CENTER; background = rounded(PANEL, 10); setPadding(dp(10),dp(5),dp(10),dp(5)) })
        root.addView(header)
        root.addView(TextView(this).apply { text = "PROFESSIONAL PRINT STUDIO"; textSize = 11f; setTypeface(null,1); setTextColor(BLUE); setPadding(0,dp(3),0,dp(14)) })

        val status = card()
        status.addView(TextView(this).apply { text = "PRINTER STATUS"; textSize = 11f; setTextColor(MUTED) })
        status.addView(TextView(this).apply { text = "●  Android Print System"; textSize = 18f; setTypeface(null,1); setTextColor(Color.rgb(75,220,145)); setPadding(0,dp(6),0,0) })
        status.addView(TextView(this).apply { text = "Print settings are applied to the print job"; textSize = 12f; setTextColor(MUTED); setPadding(0,dp(4),0,0) })
        root.addView(status, margin(0,10))

        root.addView(actionButton("▣  SELECT DOCUMENT") { chooseDocument() }, LinearLayout.LayoutParams(-1,dp(52)).apply { setMargins(0,0,0,dp(10)) })
        val quick = row()
        quick.addView(tile("▦","Library") { Toast.makeText(this,"Library documents can be selected from Files",Toast.LENGTH_SHORT).show(); chooseDocument() }, weight())
        quick.addView(tile("▣","Files") { chooseDocument() }, weight())
        quick.addView(tile("▧","Gallery") { chooseImage() }, weight())
        quick.addView(tile("◎","Scan") { startActivity(Intent(this,MainActivity::class.java)) }, weight())
        root.addView(quick, margin(0,12))

        root.addView(section("DOCUMENT PREVIEW"))
        val pcard = card()
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(18,24,34))
            minimumHeight = dp(300)
            setPadding(dp(10),dp(10),dp(10),dp(10))
            setOnClickListener { openDocumentViewer() }
        }
        pcard.addView(preview, LinearLayout.LayoutParams(-1,dp(310)))
        val pagesRow = row()
        pagesRow.addView(smallButton("‹") { showPage(currentPage-1) }, LinearLayout.LayoutParams(dp(52),dp(42)))
        pageInfo = TextView(this).apply { text="Page 0 / 0"; gravity=Gravity.CENTER; textSize=12f; setTextColor(MUTED) }
        pagesRow.addView(pageInfo, LinearLayout.LayoutParams(0,dp(42),1f))
        pagesRow.addView(smallButton("›") { showPage(currentPage+1) }, LinearLayout.LayoutParams(dp(52),dp(42)))
        pcard.addView(pagesRow)
        fileName = TextView(this).apply { text="No document selected"; textSize=16f; setTypeface(null,1); setTextColor(Color.WHITE); setPadding(0,dp(8),0,0) }
        pcard.addView(fileName)
        pcard.setOnClickListener { openDocumentViewer() }
        root.addView(pcard, margin(0,10))

        root.addView(section("PRINT SETTINGS"))
        val settings = card()
        settingRow(settings,"Printer","System / USB printer") { printerChooser() }
        settingRow(settings,"Paper Size",paperSize) { chooseValue("Paper Size",arrayOf("A4","A5","A3","Letter","Legal")){paperSize=it;rebuild()} }
        settingRow(settings,"Orientation",orientation) { chooseValue("Orientation",arrayOf("Portrait","Landscape","Auto")){orientation=it;rebuild()} }
        settingRow(settings,"Color Mode",colorMode) { chooseValue("Color Mode",arrayOf("Color","Grayscale","Black & White")){colorMode=it;rebuild()} }
        settingRow(settings,"Copies",copies.toString()) { chooseCopies() }
        settingRow(settings,"Pages",pageRange) { choosePages() }
        settingRow(settings,"Scale",scaleMode) { chooseValue("Scale",arrayOf("Fit to Page","Actual Size","Fill Page","Shrink Oversized","Custom 50%","Custom 75%","Custom 125%","Custom 150%")){scaleMode=it;rebuild()} }
        settingRow(settings,"Duplex",duplex) { chooseValue("Duplex",arrayOf("Off","Long Edge","Short Edge")){duplex=it;rebuild()} }
        settingRow(settings,"Pages / Sheet",pagesPerSheet.toString()) { chooseValue("Pages / Sheet",arrayOf("1","2","4","6","9","16")){pagesPerSheet=it.toInt();rebuild()} }
        settingRow(settings,"Print Quality",quality) { chooseValue("Print Quality",arrayOf("Draft","Normal","High")){quality=it;rebuild()} }
        settingRow(settings,"Margins",margins) { chooseValue("Margins",arrayOf("Default","Narrow","None")){margins=it;rebuild()} }
        settingRow(settings,"Brightness",if(brightness>=0) "+$brightness" else brightness.toString()) { chooseSlider("Brightness",-100,100,brightness){brightness=it;rebuild()} }
        settingRow(settings,"Contrast","$contrast%") { chooseSlider("Contrast",50,200,contrast){contrast=it;rebuild()} }
        settingRow(settings,"Auto Rotate",if(autoRotate)"On" else "Off") { autoRotate=!autoRotate; rebuild() }
        settingRow(settings,"Reverse Order",if(reverseOrder)"On" else "Off") { reverseOrder=!reverseOrder; rebuild() }
        root.addView(settings, margin(0,10))

        root.addView(actionButton("✦  SMART PRINT") { smartPrint() }, margin(0,8))
        root.addView(actionButton("▣  PRINT DOCUMENT") { printDocument() }, margin(0,8))
        root.addView(TextView(this).apply { text="Touch the preview to open the full document viewer • Pinch to zoom • Drag to inspect"; textSize=11f; gravity=Gravity.CENTER; setTextColor(Color.rgb(115,130,150)); setPadding(0,dp(5),0,dp(8)) })
    }

    private fun handleIntent(i: Intent?) {
        val uri = when(i?.action){ Intent.ACTION_SEND,Intent.ACTION_SEND_MULTIPLE -> i.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM); Intent.ACTION_VIEW -> i.data; else -> null }
        uri?.let { importUri(it) }
    }

    private fun chooseDocument(){ startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/pdf","image/jpeg","image/png","image/webp"));addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},REQ_DOC) }
    private fun chooseImage(){ startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},REQ_DOC) }
    override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==REQ_DOC&&c==RESULT_OK)d?.data?.let{importUri(it)}}

    private fun importUri(uri:android.net.Uri){
        try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Throwable){}
        val type=contentResolver.getType(uri)?:"application/octet-stream"
        mimeType=if(type.contains("pdf",true)||uri.toString().contains(".pdf",true))"application/pdf" else type
        val ext=if(mimeType=="application/pdf")"pdf" else "img"
        val out=File(cacheDir,"printex_${System.currentTimeMillis()}.$ext")
        try{contentResolver.openInputStream(uri).use{input->if(input==null)throw IllegalStateException("Cannot read document");FileOutputStream(out).use{output->input.copyTo(output)}};sourceFile?.delete();sourceFile=out;fileName.text=uri.lastPathSegment?.substringAfterLast('/')?:"Selected document";currentPage=0;loadPreview(out)}catch(t:Throwable){Toast.makeText(this,"Import failed: ${t.message}",Toast.LENGTH_LONG).show()}
    }

    private fun loadPreview(file:File){
        previewBitmap?.let{if(!it.isRecycled)it.recycle()};previewBitmap=null
        if(mimeType=="application/pdf"){
            try{ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use{pfd->PdfRenderer(pfd).use{pageCount=max(1,it.pageCount)}};showPage(0)}catch(t:Throwable){pageCount=0;Toast.makeText(this,"PDF preview failed",Toast.LENGTH_SHORT).show()}
        }else{previewBitmap=BitmapFactory.decodeFile(file.absolutePath);pageCount=1;preview.setImageBitmap(previewBitmap);pageInfo.text="Page 1 / 1"}
    }

    private fun showPage(index:Int){if(sourceFile==null||pageCount==0)return;currentPage=index.coerceIn(0,pageCount-1);if(mimeType=="application/pdf"){try{previewBitmap?.let{if(!it.isRecycled)it.recycle()};previewBitmap=renderPdfPage(sourceFile!!,currentPage,2.5f);preview.setImageBitmap(previewBitmap)}catch(_:Throwable){}}else preview.setImageBitmap(previewBitmap);pageInfo.text="Page ${currentPage+1} / $pageCount"}

    private fun renderPdfPage(file:File,index:Int,scale:Float):Bitmap{ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use{pfd->PdfRenderer(pfd).use{renderer->renderer.openPage(index).use{page->val w=max(1000,(page.width*scale).roundToInt());val h=max(1400,(page.height*scale).roundToInt());val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);page.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);return b}}}}

    private fun openDocumentViewer(){
        val file=sourceFile?:run{Toast.makeText(this,"Select a document first",Toast.LENGTH_SHORT).show();return}
        val dialog=Dialog(this,android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        val shell=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(4,7,12));setPadding(dp(8),dp(28),dp(8),dp(8))}
        val header=row();header.addView(TextView(this).apply{text="‹  DOCUMENT PREVIEW";textSize=17f;setTypeface(null,1);setTextColor(Color.WHITE);setOnClickListener{dialog.dismiss()}},LinearLayout.LayoutParams(0,dp(48),1f));val zoomText=TextView(this).apply{text="100%";gravity=Gravity.CENTER;textSize=12f;setTextColor(Color.WHITE);background=rounded(PANEL,12);setPadding(dp(8),0,dp(8),0)};header.addView(zoomText,LinearLayout.LayoutParams(dp(64),dp(40)));shell.addView(header)
        val zoom=ZoomImageView(this);zoom.setImageBitmap(if(mimeType=="application/pdf")renderPdfPage(file,currentPage,4f) else previewBitmap);zoom.onScaleChanged={zoomText.text="${(it*100).roundToInt()}%"};shell.addView(zoom,LinearLayout.LayoutParams(-1,0,1f))
        val controls=row();lateinit var indicator:TextView
        controls.addView(smallButton("‹"){if(currentPage>0){currentPage--;indicator.text="Page ${currentPage+1} / $pageCount";zoom.setImageBitmap(if(mimeType=="application/pdf")renderPdfPage(file,currentPage,4f)else previewBitmap);zoom.resetZoom()}},LinearLayout.LayoutParams(dp(58),dp(48)))
        indicator=TextView(this).apply{text="Page ${currentPage+1} / $pageCount";gravity=Gravity.CENTER;textSize=13f;setTextColor(MUTED)};controls.addView(indicator,LinearLayout.LayoutParams(0,dp(48),1f))
        controls.addView(smallButton("›"){if(currentPage<pageCount-1){currentPage++;indicator.text="Page ${currentPage+1} / $pageCount";zoom.setImageBitmap(if(mimeType=="application/pdf")renderPdfPage(file,currentPage,4f)else previewBitmap);zoom.resetZoom()}},LinearLayout.LayoutParams(dp(58),dp(48)));shell.addView(controls)
        shell.addView(TextView(this).apply{text="Pinch to zoom • Drag to inspect • Page ${currentPage+1} / $pageCount";textSize=11f;gravity=Gravity.CENTER;setTextColor(MUTED);setPadding(0,0,0,dp(6))})
        dialog.setContentView(shell);dialog.show()
    }

    private fun choosePages(){if(pageCount==0)return;AlertDialog.Builder(this).setTitle("Pages").setSingleChoiceItems(arrayOf("All pages","Current page","Custom range","Odd pages","Even pages"),when(pageRange){"All"->0,"Current"->1,"Odd"->3,"Even"->4 else->2}){d,w->when(w){0->{pageRange="All";d.dismiss()};1->{pageRange="Current";d.dismiss()};2->{d.dismiss();customRange()};3->{pageRange="Odd";d.dismiss()};4->{pageRange="Even";d.dismiss()}};rebuild()}.show()}
    private fun customRange(){val e=EditText(this).apply{hint="Example: 1-5,8,10";setTextColor(Color.WHITE);setHintTextColor(MUTED);setSingleLine()};AlertDialog.Builder(this).setTitle("Custom page range").setView(e).setPositiveButton("Apply"){_,_->val v=e.text.toString().trim();if(v.isNotEmpty()){pageRange=v;rebuild()}}.setNegativeButton("Cancel",null).show()}
    private fun chooseCopies(){val e=EditText(this).apply{inputType=2;setText(copies.toString());setSelectAllOnFocus(true);setTextColor(Color.WHITE)};AlertDialog.Builder(this).setTitle("Copies").setView(e).setPositiveButton("Apply"){_,_->copies=(e.text.toString().toIntOrNull()?:1).coerceIn(1,99);rebuild()}.setNegativeButton("Cancel",null).show()}
    private fun chooseSlider(title:String,minV:Int,maxV:Int,start:Int,done:(Int)->Unit){val seek=SeekBar(this).apply{max=maxV-minV;progress=start-minV};AlertDialog.Builder(this).setTitle(title).setView(seek).setPositiveButton("Apply"){_,_->done(seek.progress+minV)}.setNegativeButton("Cancel",null).show()}
    private fun chooseValue(title:String,values:Array<String>,done:(String)->Unit){AlertDialog.Builder(this).setTitle(title).setItems(values){_,w->done(values[w])}.show()}
    private fun printerChooser(){try{startActivity(Intent("android.settings.PRINT_SETTINGS"))}catch(_:Throwable){Toast.makeText(this,"Open Android print settings from device settings",Toast.LENGTH_SHORT).show()}}

    private fun smartPrint(){if(sourceFile==null){Toast.makeText(this,"Select a document first",Toast.LENGTH_SHORT).show();return};paperSize="A4";orientation="Auto";scaleMode="Fit to Page";quality="High";autoRotate=true;rebuild();Toast.makeText(this,"Smart Print optimized for this document",Toast.LENGTH_LONG).show()}

    private fun printDocument(){
        val file=sourceFile?:run{Toast.makeText(this,"Select a document first",Toast.LENGTH_SHORT).show();return}
        try{
            generatedPrintFile?.delete();generatedPrintFile=File(cacheDir,"printex_print_${System.currentTimeMillis()}.pdf");createPrintPdf(file,generatedPrintFile!!)
            val pm=getSystemService(PRINT_SERVICE) as PrintManager
            val media=when(paperSize){"A5"->PrintAttributes.MediaSize.ISO_A5;"A3"->PrintAttributes.MediaSize.ISO_A3;"Letter"->PrintAttributes.MediaSize.NA_LETTER;"Legal"->PrintAttributes.MediaSize.NA_LEGAL;else->PrintAttributes.MediaSize.ISO_A4}
            val oriented=if(orientation=="Landscape")media.asLandscape()else media
            val builder=PrintAttributes.Builder().setMediaSize(oriented).setResolution(PrintAttributes.Resolution("printex","Printex",if(quality=="Draft")150 else if(quality=="Normal")300 else 600,if(quality=="Draft")150 else if(quality=="Normal")300 else 600)).setMinMargins(if(margins=="None")PrintAttributes.Margins.NO_MARGINS else PrintAttributes.Margins(500,500,500,500))
            builder.setColorMode(if(colorMode=="Color")PrintAttributes.COLOR_MODE_COLOR else PrintAttributes.COLOR_MODE_MONOCHROME)
            builder.setDuplexMode(if(duplex=="Long Edge")PrintAttributes.DUPLEX_MODE_LONG_EDGE else if(duplex=="Short Edge")PrintAttributes.DUPLEX_MODE_SHORT_EDGE else PrintAttributes.DUPLEX_MODE_NONE)
            pm.print("Printex • ${fileName.text}",object:PrintDocumentAdapter(){override fun onLayout(old:PrintAttributes?,new:PrintAttributes,signal:android.os.CancellationSignal,cb:LayoutResultCallback,extras:android.os.Bundle?){cb.onLayoutFinished(PrintDocumentInfo.Builder("Printex.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(PageRange.ALL_PAGES.let{generatedPageCount}).build(),true)};override fun onWrite(ranges:Array<PageRange>,dest:ParcelFileDescriptor,signal:android.os.CancellationSignal,cb:WriteResultCallback){try{FileOutputStream(dest.fileDescriptor).use{out->generatedPrintFile!!.inputStream().use{it.copyTo(out)}};cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))}catch(t:Throwable){cb.onWriteFailed(t.message)}}},builder.build())
        }catch(t:Throwable){Toast.makeText(this,"Print preparation failed: ${t.message}",Toast.LENGTH_LONG).show()}
    }

    private var generatedPageCount=1

    private fun createPrintPdf(file:File,out:File){
        val bitmaps=ArrayList<Bitmap>()
        if(mimeType=="application/pdf"){
            ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use{pfd->PdfRenderer(pfd).use{renderer->val indices=selectedIndices(renderer.pageCount);for(i in indices){if(isFinishing)break;renderer.openPage(i).use{p->val scale=if(quality=="Draft")1.8f else if(quality=="Normal")2.7f else 4f;val b=Bitmap.createBitmap(max(800,(p.width*scale).roundToInt()),max(1100,(p.height*scale).roundToInt()),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);bitmaps.add(transformBitmap(b))}}}}
        }else{BitmapFactory.decodeFile(file.absolutePath)?.let{bitmaps.add(transformBitmap(it))}}
        if(reverseOrder)bitmaps.reverse()
        val expanded=ArrayList<Bitmap>();repeat(copies.coerceIn(1,99)){expanded.addAll(bitmaps)}
        val doc=android.graphics.pdf.PdfDocument();val sheet=paperPoints();val cols=when(pagesPerSheet){2->2;4->2;6->2;9->3;16->4;else->1};val rows=when(pagesPerSheet){2->1;4->2;6->3;9->3;16->4;else->1};var index=0;generatedPageCount=0
        while(index<expanded.size){val page=doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(sheet.first,sheet.second,generatedPageCount).create());val c=page.canvas;c.drawColor(Color.WHITE);val cellW=sheet.first.toFloat()/cols;val cellH=sheet.second.toFloat()/rows;for(slot in 0 until pagesPerSheet){if(index>=expanded.size)break;val b=expanded[index++];val col=slot%cols;val row=slot/cols;drawScaled(c,b,col*cellW,row*cellH,cellW,cellH);b.recycle()};doc.finishPage(page);generatedPageCount++};FileOutputStream(out).use{doc.writeTo(it)};doc.close()
    }

    private fun selectedIndices(total:Int):List<Int>{val all=(0 until total).toList();return when{pageRange=="All"->all;pageRange=="Current"->listOf(currentPage.coerceIn(0,total-1));pageRange=="Odd"->all.filter{(it+1)%2==1};pageRange=="Even"->all.filter{(it+1)%2==0};else->parseRange(pageRange,total)}}
    private fun parseRange(s:String,total:Int):List<Int>{val out=LinkedHashSet<Int>();s.split(',').forEach{part->val p=part.trim();if(p.contains('-')){val a=p.substringBefore('-').toIntOrNull();val b=p.substringAfter('-').toIntOrNull();if(a!=null&&b!=null)for(x in min(a,b)..max(a,b))if(x in 1..total)out.add(x-1)}else p.toIntOrNull()?.let{if(it in 1..total)out.add(it-1)}};return out.toList().ifEmpty{(0 until total).toList()}}

    private fun transformBitmap(src:Bitmap):Bitmap{var b=src;if(colorMode!="Color"){val out=Bitmap.createBitmap(b.width,b.height,Bitmap.Config.ARGB_8888);val c=Canvas(out);val p=Paint();p.colorFilter=android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply{setSaturation(0f)});c.drawBitmap(b,0f,0f,p);b.recycle();b=out};if(brightness!=0||contrast!=100){val out=Bitmap.createBitmap(b.width,b.height,Bitmap.Config.ARGB_8888);val cm=android.graphics.ColorMatrix();val a=contrast/100f;val t=brightness.toFloat();cm.set(floatArrayOf(a,0f,0f,0f,t,0f,a,0f,0f,t,0f,0f,a,0f,t,0f,0f,0f,1f,0f));Canvas(out).drawBitmap(b,0f,0f,Paint().apply{colorFilter=android.graphics.ColorMatrixColorFilter(cm)});b.recycle();b=out};return b}
    private fun drawScaled(c:Canvas,b:Bitmap,x:Float,y:Float,w:Float,h:Float){val pad=if(margins=="None")0f else min(w,h)*0.04f;val availW=w-2*pad;val availH=h-2*pad;val sx=availW/b.width;val sy=availH/b.height;val s=when{scaleMode=="Actual Size"->1f;scaleMode=="Fill Page"->max(sx,sy);scaleMode.contains("50") -> .5f;scaleMode.contains("75")->.75f;scaleMode.contains("125")->1.25f;scaleMode.contains("150")->1.5f;else->min(sx,sy)};val dw=b.width*s;val dh=b.height*s;val left=x+(w-dw)/2;val top=y+(h-dh)/2;c.drawBitmap(b,null,android.graphics.RectF(left,top,left+dw,top+dh),Paint(Paint.ANTI_ALIAS_FLAG))}
    private fun paperPoints():Pair<Int,Int>{val p=when(paperSize){"A5"->Pair(420,595);"A3"->Pair(842,1191);"Letter"->Pair(612,792);"Legal"->Pair(612,1008);else->Pair(595,842)};return if(orientation=="Landscape")Pair(p.second,p.first)else p}

    private fun rebuild(){if(sourceFile!=null){pageInfo.text="Page ${currentPage+1} / $pageCount"}}

    private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(16));background=rounded(PANEL,20)}
    private fun section(t:String)=TextView(this).apply{text=t;textSize=11f;setTypeface(null,1);setTextColor(MUTED);setPadding(0,dp(6),0,dp(7))}
    private fun settingRow(parent:LinearLayout,label:String,value:String,onClick:()->Unit){val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(7),0,dp(7));setOnClickListener{onClick()}};r.addView(TextView(this).apply{text=label;textSize=14f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(48),1f));r.addView(TextView(this).apply{text=value+"  ›";textSize=13f;setTextColor(BLUE);gravity=Gravity.END},LinearLayout.LayoutParams(dp(170),dp(48)));parent.addView(r)}
    private fun smallButton(t:String,action:()->Unit)=Button(this).apply{text=t;textSize=22f;setTextColor(Color.WHITE);background=rounded(PANEL,14);setOnClickListener{action()}}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).roundToInt()
    private fun weight(w:Float=1f)=LinearLayout.LayoutParams(0,dp(82),w).apply{setMargins(dp(4),0,dp(4),0)}
    private fun margin(l:Int,t:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(l),dp(t))}
    private fun rounded(color:Int,r:Int)=android.graphics.drawable.GradientDrawable().apply{setColor(color);cornerRadius=dp(r).toFloat()}
    private fun actionButton(text:String,action:()->Unit)=Button(this).apply{this.text=text;textSize=13f;setTypeface(null,1);setTextColor(Color.WHITE);background=rounded(Color.rgb(35,90,190),16);setOnClickListener{action()}}
    private fun tile(icon:String,text:String,action:()->Unit)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(4),dp(8),dp(4),dp(8));background=rounded(PANEL,16);setOnClickListener{action()};addView(TextView(this@PrintExActivity).apply{this.text=icon;textSize=22f;gravity=Gravity.CENTER;setTextColor(BLUE)});addView(TextView(this@PrintExActivity).apply{this.text=text;textSize=10f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setPadding(0,dp(4),0,0)})}

    private class ZoomImageView(ctx:android.content.Context):ImageView(ctx){
        private var scale=1f;private var lastX=0f;private var lastY=0f;private var mode=0;private val detector=ScaleGestureDetector(ctx,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(d:ScaleGestureDetector):Boolean{scale=(scale*d.scaleFactor).coerceIn(1f,6f);scaleX=scale;scaleY=scale;onScaleChanged?.invoke(scale);return true}});var onScaleChanged:((Float)->Unit)?=null
        init{scaleType=ScaleType.CENTER;setOnTouchListener{_,e->detector.onTouchEvent(e);when(e.actionMasked){MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;mode=1};MotionEvent.ACTION_MOVE->{if(mode==1&&scale>1f){translationX+=e.x-lastX;translationY+=e.y-lastY;lastX=e.x;lastY=e.y}};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->mode=0};true}}
        fun resetZoom(){scale=1f;scaleX=1f;scaleY=1f;translationX=0f;translationY=0f;onScaleChanged?.invoke(1f)}
    }
    companion object{private const val REQ_DOC=9001;private const val BG=0xFF080C14.toInt();private const val PANEL=0xFF121924.toInt();private const val MUTED=0xFF8796AA.toInt();private const val BLUE=0xFF5B9CFF.toInt()}
}
