from pathlib import Path
p=Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
s=p.read_text()

def meth(s,n,new):
 m='    private fun '+n+'('; a=s.find(m); b=s.find('{',a)
 if a<0 or b<0: raise SystemExit('missing '+n)
 d=0
 for i in range(b,len(s)):
  if s[i]=='{': d+=1
  elif s[i]=='}':
   d-=1
   if d==0:return s[:a]+new+s[i+1:]
 raise SystemExit('unclosed '+n)

s=s.replace('    private var printexRotation = 0\n','').replace('    private var printexZoom = 1f\n','')
s=s.replace('    private var printexPages = "All pages"\n','    private var printexPages = "All pages"\n    private var printexPageSpec = ""\n    private var printexPickingFromLibrary = false\n',1)
s=s.replace('source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f,4))\n        source.addView(tile("▦", "Preview") { printexOpenPreview() }, weight(1f,4))\n        source.addView(tile("⚙", "Settings") { printexPrintSettings() }, weight(1f,4))','''source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f,4))
        source.addView(tile("▦", "Library") { printexOpenLibrary() }, weight(1f,4))
        source.addView(tile("▦", "Preview") { printexOpenPreview() }, weight(1f,4))''',1)
s=s.replace('scaleType = ImageView.ScaleType.MATRIX; setBackgroundColor(Color.rgb(18,25,36)); setPadding(dp(8),dp(8),dp(8),dp(8)); setOnTouchListener { v, e -> printexZoomTouch(v, e) }','scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE); setPadding(dp(8),dp(8),dp(8),dp(8))',1)
s=s.replace('        printexImageRow(ims, "Rotation", printexRotation) { printexRotation = it; renderTab(3) }\n','')
s=s.replace('        actions.addView(actionButton("SHARE") { showShareFormatDialog() }, weight(1f,5))\n','')
s=s.replace('        content.addView(ims, margin(0,8))\n\n        val actions = row()','''        ims.addView(actionButton("DEFAULT") { printexBrightness = 0; printexContrast = 0; renderTab(3) }, LinearLayout.LayoutParams(-1,dp(44)).apply { setMargins(0,dp(8),0,0) })
        content.addView(ims, margin(0,8))

        val actions = row()''',1)
s=s.replace('printexSettingRow(ps, "Pages", printexPages) { printexChoose("Pages", arrayOf("All pages","Odd pages","Even pages","Selected pages","Page range")) { printexPages = it; renderTab(3) } }','printexSettingRow(ps, "Pages", if (printexPages=="Page range" && printexPageSpec.isNotBlank()) "Page range: $printexPageSpec" else printexPages) { printexChoosePages() }',1)
s=meth(s,'printexLoadPreviewBitmap',r'''    private fun printexLoadPreviewBitmap(): Bitmap? {
        val f=printexFile?:return null
        return try {
            if(f.extension.equals("pdf",true)) {
                val pfd=android.os.ParcelFileDescriptor.open(f,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val r=android.graphics.pdf.PdfRenderer(pfd); if(r.pageCount==0){r.close();pfd.close();return null}
                val page=r.openPage(0); val scale=1.35f
                val b=Bitmap.createBitmap((page.width*scale).toInt().coerceAtMost(1800),(page.height*scale).toInt().coerceAtMost(2400),Bitmap.Config.ARGB_8888)
                b.eraseColor(Color.WHITE); page.render(b,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close();r.close();pfd.close();b
            } else android.graphics.BitmapFactory.decodeFile(f.absolutePath)
        } catch(_:Throwable){null}
    }
''')
s=meth(s,'printexOpenPreview',r'''    private fun printexOpenPreview() {
        val b=printexLoadPreviewBitmap()?:run{Toast.makeText(this,"Select a supported PDF or image first",Toast.LENGTH_SHORT).show();return}
        val iv=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;setImageBitmap(b);setBackgroundColor(Color.WHITE)}
        AlertDialog.Builder(this).setTitle("Print Preview").setView(iv).setPositiveButton("CLOSE"){_,_->if(!b.isRecycled)b.recycle()}.show()
    }
''')
anchor='    private fun printexChooseCopies() {'
pagefun=r'''    private fun printexChoosePages() {
        val v=arrayOf("All pages","Odd pages","Even pages","Selected pages","Page range")
        AlertDialog.Builder(this).setTitle("Pages").setSingleChoiceItems(v,v.indexOf(printexPages).coerceAtLeast(0)){d,w->
            printexPages=v[w]; d.dismiss()
            if(printexPages=="Page range"){
                val e=EditText(this).apply{hint="1,3,5 or 1-5 or 1,3-5";setText(printexPageSpec);setSingleLine()}
                AlertDialog.Builder(this).setTitle("Page range").setView(e).setPositiveButton("APPLY"){_,_->printexPageSpec=e.text.toString().trim();renderTab(3)}.setNegativeButton("CANCEL",null).show()
            } else {printexPageSpec="";renderTab(3)}
        }.setNegativeButton("CANCEL",null).show()
    }

'''
s=s.replace(anchor,pagefun+anchor,1)
s=s.replace('        content.addView(controls, margin(0,8))\n        val list = library.list()','''        content.addView(controls, margin(0,8))
        if (printexPickingFromLibrary) content.addView(actionButton("USE SELECTED IN PRINTEX") { printexUseSelectedLibraryFile() }, margin(0,8))
        val list = library.list()''',1)
helpers=r'''    private fun printexOpenLibrary(){printexPickingFromLibrary=true;renderTab(2)}
    private fun printexUseSelectedLibraryFile(){
        val f=library.list().firstOrNull{selectedLibrary.contains(it.absolutePath)}?:run{setStatus("Select a library file first");return}
        printexFile=f;printexUri=Uri.fromFile(f);printexPickingFromLibrary=false;renderTab(3)
    }
    private fun printexPageIndices(count:Int):List<Int>{
        if(count<=0)return emptyList()
        if(printexPages=="Odd pages")return (0 until count).filter{(it+1)%2==1}
        if(printexPages=="Even pages")return (0 until count).filter{(it+1)%2==0}
        if(printexPages!="Page range"&&printexPages!="Selected pages")return (0 until count).toList()
        val out=linkedSetOf<Int>()
        printexPageSpec.split(',').forEach{p->val x=p.trim();if(x.contains('-')){val q=x.split('-',limit=2);val a=q[0].toIntOrNull();val z=q[1].toIntOrNull();if(a!=null&&z!=null&&a>0&&z>=a)for(n in a..z)if(n<=count)out.add(n-1)}else{x.toIntOrNull()?.let{if(it in 1..count)out.add(it-1)}}}
        return out.toList()
    }
    private fun printexPaper():Pair<Int,Int>=when(printexPaper){"A5"->420 to 595;"A3"->842 to 1191;"Letter"->612 to 792;"Legal"->612 to 1008;"4 × 6 in"->288 to 432;"5 × 7 in"->360 to 504;else->595 to 842}
    private fun printexAdjust(b:Bitmap):Bitmap{
        if(printexBrightness==0&&printexContrast==0)return b
        val c=1f+printexContrast/100f;val t=128f*(1f-c)+printexBrightness*2.55f
        val cm=android.graphics.ColorMatrix(floatArrayOf(c,0f,0f,0f,t,0f,c,0f,0f,t,0f,0f,c,0f,t,0f,0f,0f,1f,0f))
        val o=Bitmap.createBitmap(b.width,b.height,Bitmap.Config.ARGB_8888);android.graphics.Canvas(o).drawBitmap(b,0f,0f,android.graphics.Paint().apply{colorFilter=android.graphics.ColorMatrixColorFilter(cm)});b.recycle();return o
    }
    private fun printexPrintPdf():File?{
        val f=printexFile?:return null;val out=File(cacheDir,"printex_${System.currentTimeMillis()}.pdf");val pdf=android.graphics.pdf.PdfDocument();val src=mutableListOf<Bitmap>()
        try{
            if(f.extension.equals("pdf",true)){
                val pfd=android.os.ParcelFileDescriptor.open(f,android.os.ParcelFileDescriptor.MODE_READ_ONLY);val r=android.graphics.pdf.PdfRenderer(pfd)
                printexPageIndices(r.pageCount).forEach{n->val p=r.openPage(n);val b=Bitmap.createBitmap((p.width*1.4f).toInt().coerceAtMost(1800),(p.height*1.4f).toInt().coerceAtMost(2400),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_PRINT);p.close();src.add(printexAdjust(b))};r.close();pfd.close()
            }else android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.let{src.add(printexAdjust(it))}
            if(src.isEmpty())throw IllegalArgumentException("No printable pages")
            var(pw,ph)=printexPaper();if(printexOrientation=="Landscape"){val x=pw;pw=ph;ph=x};val slots=when(printexLayout){"2 pages per sheet"->2;"4 pages per sheet"->4;"6 pages per sheet"->6;"9 pages per sheet"->9;else->1};val cols=when(slots){2->1;4,6->2;9->3;else->1};val rows=(slots+cols-1)/cols;val cw=pw*2/cols;val ch=ph*2/rows;var no=1
            repeat(printexCopies.coerceIn(1,20)){src.chunked(slots).forEach{chunk->val pg=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pw*2,ph*2,no++).create());pg.canvas.drawColor(Color.WHITE);val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG);chunk.forEachIndexed{i,b->val box=android.graphics.RectF((i%cols*cw+12).toFloat(),(i/cols*ch+12).toFloat(),((i%cols+1)*cw-12).toFloat(),((i/cols+1)*ch-12).toFloat());val ratio=when(printexScaling){"Actual size"->1f;"Fill page"->maxOf(box.width()/b.width,box.height()/b.height);else->minOf(box.width()/b.width,box.height()/b.height)};val w=b.width*ratio;val h=b.height*ratio;pg.canvas.drawBitmap(b,null,android.graphics.RectF((box.left+box.right-w)/2,(box.top+box.bottom-h)/2,(box.left+box.right+w)/2,(box.top+box.bottom+h)/2),paint)};pdf.finishPage(pg)}}
            FileOutputStream(out).use{pdf.writeTo(it)};pdf.close();src.forEach{if(!it.isRecycled)it.recycle()};return out
        }catch(t:Throwable){src.forEach{if(!it.isRecycled)it.recycle()};try{pdf.close()}catch(_:Throwable){};out.delete();setStatus("Print preparation failed • ${t.message?:"Unknown error"}");return null}
    }

'''
pos=s.find('    private fun renderTools() {');s=s[:pos]+helpers+s[pos:]
s=meth(s,'printexPrint',r'''    private fun printexPrint(){
        if(printexFile==null){Toast.makeText(this,"Select a PDF or image first",Toast.LENGTH_SHORT).show();return}
        executor.execute{val prepared=printexPrintPdf()?:return@execute;runOnUiThread{try{
            val pm=getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val adapter=object:android.print.PrintDocumentAdapter{
                override fun onLayout(o:android.print.PrintAttributes?,n:android.print.PrintAttributes?,c:android.os.CancellationSignal?,cb:android.print.PrintDocumentAdapter.LayoutResultCallback?,e:android.os.Bundle?){if(c?.isCanceled==true){cb?.onLayoutCancelled();return};cb?.onLayoutFinished(android.print.PrintDocumentInfo.Builder(prepared.name).setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build(),true)}
                override fun onWrite(r:Array<android.print.PageRange>?,d:android.os.ParcelFileDescriptor?,c:android.os.CancellationSignal?,cb:android.print.PrintDocumentAdapter.WriteResultCallback?){try{FileInputStream(prepared).use{i->FileOutputStream(d?.fileDescriptor).use{o->i.copyTo(o)}};cb?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))}catch(t:Throwable){cb?.onWriteFailed(t.message)}}
            }
            val attrs=android.print.PrintAttributes.Builder().setMediaSize(when(printexPaper){"A5"->android.print.PrintAttributes.MediaSize("A5X","A5",148,210);"A3"->android.print.PrintAttributes.MediaSize("A3X","A3",297,420);"Letter"->android.print.PrintAttributes.MediaSize("LX","Letter",216,279);"Legal"->android.print.PrintAttributes.MediaSize("LGX","Legal",216,356);"4 × 6 in"->android.print.PrintAttributes.MediaSize("46X","4 x 6",102,152);"5 × 7 in"->android.print.PrintAttributes.MediaSize("57X","5 x 7",127,178);else->android.print.PrintAttributes.MediaSize("A4X","A4",210,297)}).setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).setDuplexMode(when(printexDuplex){"Duplex • Long edge"->android.print.PrintAttributes.DUPLEX_MODE_LONG_EDGE;"Duplex • Short edge"->android.print.PrintAttributes.DUPLEX_MODE_SHORT_EDGE;else->android.print.PrintAttributes.DUPLEX_MODE_NONE}).build()
            pm.print("Printex • ${printexDisplayName()}",adapter,attrs)
        }catch(t:Throwable){setStatus("Print failed • ${t.message?:"Unknown error"}")}}}
    }
''')
if 'import java.io.FileInputStream' not in s:s=s.replace('import java.io.File\n','import java.io.File\nimport java.io.FileInputStream\n',1)
p.write_text(s)
print('Finalized Printex functional controls')