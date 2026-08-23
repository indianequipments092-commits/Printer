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

# Keep one stable zoom state and make the main preview start at fit-to-page.
if 'private var printexZoom = 1f' not in s:
    marker='    private var printexPickingFromLibrary = false\n'
    if marker in s: s=s.replace(marker,marker+'    private var printexZoom = 1f\n',1)

s=s.replace('scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE); setPadding(dp(8),dp(8),dp(8),dp(8))','scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.WHITE); setPadding(dp(8),dp(8),dp(8),dp(8)); setOnTouchListener { v,e -> printexZoomTouch(v,e) }',1)
s=s.replace('val previewBitmap = printexLoadPreviewBitmap()\n        if (previewBitmap != null) printexPreview!!.setImageBitmap(previewBitmap)','val previewBitmap = printexPreviewBitmap()\n        if (previewBitmap != null) printexPreview!!.setImageBitmap(previewBitmap)',1)

# The preview must use the same brightness/contrast transformation as print rendering.
if 'private fun printexPreviewBitmap()' not in s:
    anchor='    private fun printexOpenPreview() {'
    helper='''    private fun printexPreviewBitmap(): Bitmap? {\n        val b=printexLoadPreviewBitmap()?:return null\n        return try {\n            if (printexBrightness==0 && printexContrast==0) b else {\n                val c=1f+printexContrast/100f\n                val t=128f*(1f-c)+printexBrightness*2.55f\n                val cm=android.graphics.ColorMatrix(floatArrayOf(c,0f,0f,0f,t,0f,c,0f,0f,t,0f,0f,c,0f,t,0f,0f,0f,1f,0f))\n                val out=Bitmap.createBitmap(b.width,b.height,Bitmap.Config.ARGB_8888)\n                android.graphics.Canvas(out).drawBitmap(b,0f,0f,android.graphics.Paint().apply { colorFilter=android.graphics.ColorMatrixColorFilter(cm) })\n                if(!b.isRecycled) b.recycle()\n                out\n            }\n        } catch(_:Throwable) { b }\n    }\n\n    private fun printexZoomTouch(v: View, e: android.view.MotionEvent): Boolean {\n        val iv=v as? ImageView ?: return false\n        if(e.actionMasked==android.view.MotionEvent.ACTION_DOWN){ return true }\n        if(e.pointerCount>=2 && (e.actionMasked==android.view.MotionEvent.ACTION_MOVE || e.actionMasked==android.view.MotionEvent.ACTION_POINTER_DOWN)){\n            val dx=e.getX(0)-e.getX(1); val dy=e.getY(0)-e.getY(1); val dist=kotlin.math.sqrt(dx*dx+dy*dy)\n            if(!::printexZoom.isInitialized){}\n            printexZoom=(if(dist>120f) printexZoom*1.02f else printexZoom/1.02f).coerceIn(1f,4f)\n            val m=android.graphics.Matrix(); m.setScale(printexZoom,printexZoom,iv.width/2f,iv.height/2f); iv.imageMatrix=m; iv.scaleType=ImageView.ScaleType.MATRIX\n            return true\n        }\n        if(e.actionMasked==android.view.MotionEvent.ACTION_UP){ return true }\n        return true\n    }\n\n'''
    # Avoid Kotlin's invalid lateinit-property reference: Float is always initialized.
    helper=helper.replace('            if(!::printexZoom.isInitialized){}\n','')
    if anchor in s: s=s.replace(anchor,helper+anchor,1)

# Preview dialog also starts at fit-to-page and can be zoomed with the same touch handler.
s=meth(s,'printexOpenPreview',r'''    private fun printexOpenPreview() {
        val b=printexPreviewBitmap()?:run{Toast.makeText(this,"Select a supported PDF or image first",Toast.LENGTH_SHORT).show();return}
        printexZoom=1f
        val iv=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER;setImageBitmap(b);setBackgroundColor(Color.WHITE);setOnTouchListener { v,e -> printexZoomTouch(v,e) }}
        AlertDialog.Builder(this).setTitle("Print Preview").setView(iv).setPositiveButton("CLOSE"){_,_->if(!b.isRecycled)b.recycle()}.show()
    }
''')

p.write_text(s)
print('Printex preview now uses fit-to-page defaults, zoom handling, and image adjustments')
