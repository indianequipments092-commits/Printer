from pathlib import Path
p=Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
s=p.read_text()
def meth(s,n,new):
 a=s.find('    private fun '+n+'('); b=s.find('{',a); d=0
 for i in range(b,len(s)):
  if s[i]=='{': d+=1
  elif s[i]=='}':
   d-=1
   if d==0:return s[:a]+new+s[i+1:]
 raise SystemExit('missing '+n)
new=r'''    private fun printexLoadPreviewBitmap(): Bitmap? {
        val f=printexFile?:return null
        return try {
            val raw=if(f.extension.equals("pdf",true)){
                val pfd=android.os.ParcelFileDescriptor.open(f,android.os.ParcelFileDescriptor.MODE_READ_ONLY);val r=android.graphics.pdf.PdfRenderer(pfd)
                if(r.pageCount==0){r.close();pfd.close();return null};val p=r.openPage(0);val b=Bitmap.createBitmap((p.width*1.35f).toInt().coerceAtMost(1800),(p.height*1.35f).toInt().coerceAtMost(2400),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();r.close();pfd.close();b
            }else android.graphics.BitmapFactory.decodeFile(f.absolutePath)?:return null
            printexPreviewTransform(printexAdjust(raw))
        }catch(_:Throwable){null}
    }
'''
s=meth(s,'printexLoadPreviewBitmap',new)
helper=r'''    private fun printexPreviewTransform(src:Bitmap):Bitmap{
        var b=src
        var w=595;var h=842
        when(printexPaper){"A5"->{w=420;h=595};"A3"->{w=842;h=1191};"Letter"->{w=612;h=792};"Legal"->{w=612;h=1008};"4 × 6 in"->{w=288;h=432};"5 × 7 in"->{w=360;h=504}}
        if(printexOrientation=="Landscape"||(printexOrientation=="Auto"&&b.width>b.height)){val x=w;w=h;h=x}
        val outW=900;val outH=(h.toFloat()/w*outW).toInt().coerceAtLeast(1);val out=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888);out.eraseColor(Color.WHITE)
        val c=android.graphics.Canvas(out);val pad=18f;val box=android.graphics.RectF(pad,pad,outW-pad,outH-pad);val ratio=when(printexScaling){"Actual size"->1f;"Fill page"->maxOf(box.width()/b.width,box.height()/b.height);else->minOf(box.width()/b.width,box.height()/b.height)};val dw=b.width*ratio;val dh=b.height*ratio
        c.drawBitmap(b,null,android.graphics.RectF((outW-dw)/2,(outH-dh)/2,(outW+dw)/2,(outH+dh)/2),android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG));if(!b.isRecycled)b.recycle();return out
    }

'''
pos=s.find('    private fun renderTools() {');s=s[:pos]+helper+s[pos:]
p.write_text(s)
print('Printex preview settings patch applied')