from pathlib import Path

MAIN = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
if not MAIN.exists():
    raise SystemExit(f'Missing required file: {MAIN}')
s = MAIN.read_text(encoding='utf-8')

# Fresh integrated Printex tab. No standalone PrintExActivity dependency.
if 'private fun renderPrintex()' not in s:
    nav_old = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)'''
    nav_new = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("▣", "Printex", 3)\n        navButton("⚙", "Tools", 4)'''
    if nav_old not in s:
        raise SystemExit('Printex navigation anchor not found')
    s = s.replace(nav_old, nav_new, 1)

    when_old = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            else -> renderTools()\n        }'''
    when_new = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            3 -> renderPrintex()\n            else -> renderTools()\n        }'''
    if when_old not in s:
        raise SystemExit('Printex tab switch anchor not found')
    s = s.replace(when_old, when_new, 1)

    fields = '''    private var printexUri: Uri? = null\n    private var printexFile: File? = null\n    private var printexBrightness = 0\n    private var printexContrast = 0\n    private var printexRotation = 0\n    private var printexCopies = 1\n    private var printexPages = "All pages"\n    private var printexPaper = "A4"\n    private var printexOrientation = "Auto"\n    private var printexScaling = "Fit to page"\n    private var printexLayout = "1 page per sheet"\n    private var printexDuplex = "One side"\n    private var printexPreview: ImageView? = null\n    private var printexZoom = 1f\n\n'''
    marker = '    private val PDF_FOLDER_REQUEST = 7001\n'
    if marker not in s:
        raise SystemExit('Printex field anchor not found')
    s = s.replace(marker, marker + fields, 1)

    activity_marker = '    override fun onBackPressed() {'
    activity_code = '''    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {\n        super.onActivityResult(requestCode, resultCode, data)\n        if (requestCode == 8101 && resultCode == RESULT_OK) {\n            val uri = data?.data ?: return\n            printexUri = uri\n            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}\n            printexFile = copyPrintexUriToCache(uri)\n            renderTab(3)\n        }\n    }\n\n'''
    if activity_marker not in s:
        raise SystemExit('Activity callback anchor not found')
    s = s.replace(activity_marker, activity_code + activity_marker, 1)

    method = r'''    private fun renderPrintex() {
        content.addView(title("PRINTEX", "PROFESSIONAL PRINT STUDIO • DOCUMENTS + PHOTOS + PDF"))

        val hero = card()
        hero.addView(TextView(this).apply { text = "PRINT • PREVIEW • CONTROL"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(95,155,255)) })
        hero.addView(TextView(this).apply { text = "Everything in one print workspace"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(6),0,dp(2)) })
        hero.addView(TextView(this).apply { text = "PDF • Image • Document • Pages • Copies • Duplex"; textSize = 12f; setTextColor(Color.rgb(145,160,180)) })
        content.addView(hero, margin(0,10))

        val source = row()
        source.addView(tile("▤", "PDF / File") { printexChooseFile() }, weight(1f,4))
        source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f,4))
        source.addView(tile("▦", "Preview") { printexOpenPreview() }, weight(1f,4))
        source.addView(tile("⚙", "Settings") { printexPrintSettings() }, weight(1f,4))
        content.addView(source, margin(0,10))

        val previewCard = card()
        previewCard.addView(TextView(this).apply { text = "DOCUMENT PREVIEW"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(145,160,180)) })
        printexPreview = ImageView(this).apply { scaleType = ImageView.ScaleType.MATRIX; setBackgroundColor(Color.rgb(18,25,36)); setPadding(dp(8),dp(8),dp(8),dp(8)); setOnTouchListener { v, e -> printexZoomTouch(v, e) } }
        val previewBitmap = printexLoadPreviewBitmap()
        if (previewBitmap != null) printexPreview!!.setImageBitmap(previewBitmap) else printexPreview!!.setImageDrawable(null)
        previewCard.addView(printexPreview, LinearLayout.LayoutParams(-1, dp(300)))
        content.addView(previewCard, margin(0,10))

        if (printexUri == null) content.addView(emptyCard("No document selected", "Choose a PDF, image or document to start."), margin(0,8))
        else content.addView(TextView(this).apply { text = "Selected: ${printexDisplayName()}"; textSize = 13f; setTextColor(Color.rgb(160,175,195)); setPadding(dp(4),dp(4),dp(4),dp(4)) })

        content.addView(section("PRINT SETTINGS"), margin(0,8))
        val ps = card()
        printexSettingRow(ps, "Copies", printexCopies.toString()) { printexChooseCopies() }
        printexSettingRow(ps, "Pages", printexPages) { printexChoose("Pages", arrayOf("All pages","Odd pages","Even pages","Selected pages","Page range")) { printexPages = it; renderTab(3) } }
        printexSettingRow(ps, "Paper Size", printexPaper) { printexChoose("Paper Size", arrayOf("A4","A5","A3","Letter","Legal","4 × 6 in","5 × 7 in")) { printexPaper = it; renderTab(3) } }
        printexSettingRow(ps, "Orientation", printexOrientation) { printexChoose("Orientation", arrayOf("Auto","Portrait","Landscape")) { printexOrientation = it; renderTab(3) } }
        printexSettingRow(ps, "Scaling", printexScaling) { printexChoose("Scaling", arrayOf("Fit to page","Actual size","Fill page","Custom")) { printexScaling = it; renderTab(3) } }
        printexSettingRow(ps, "Layout", printexLayout) { printexChoose("Layout", arrayOf("1 page per sheet","2 pages per sheet","4 pages per sheet","6 pages per sheet","9 pages per sheet")) { printexLayout = it; renderTab(3) } }
        printexSettingRow(ps, "Double-Sided", printexDuplex) { printexChoose("Double-Sided Printing", arrayOf("One side","Duplex • Long edge","Duplex • Short edge")) { printexDuplex = it; renderTab(3) } }
        content.addView(ps, margin(0,8))

        content.addView(section("IMAGE SETTINGS"), margin(0,8))
        val ims = card()
        printexImageRow(ims, "Brightness", printexBrightness) { printexBrightness = it; renderTab(3) }
        printexImageRow(ims, "Contrast", printexContrast) { printexContrast = it; renderTab(3) }
        printexImageRow(ims, "Rotation", printexRotation) { printexRotation = it; renderTab(3) }
        content.addView(ims, margin(0,8))

        val actions = row()
        actions.addView(actionButton("PRINT") { printexPrint() }, weight(1f,5))
        actions.addView(actionButton("SHARE") { showShareFormatDialog() }, weight(1f,5))
        content.addView(actions, margin(0,12))
    }

    private fun printexSettingRow(parent: LinearLayout, name: String, value: String, click: () -> Unit) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(7),dp(12),dp(7)); setOnClickListener { click() } }
        r.addView(TextView(this).apply { text = name; textSize = 15f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0,dp(52),1f))
        r.addView(TextView(this).apply { text = "$value  ›"; textSize = 14f; setTextColor(Color.rgb(125,165,230)); gravity = Gravity.CENTER_VERTICAL or Gravity.END }, LinearLayout.LayoutParams(dp(190),dp(52)))
        parent.addView(r)
    }

    private fun printexImageRow(parent: LinearLayout, name: String, value: Int, changed: (Int) -> Unit) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(7),dp(12),dp(7)) }
        r.addView(TextView(this).apply { text = name; textSize = 15f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0,dp(52),1f))
        r.addView(Button(this).apply { text = "−"; textSize = 20f; setOnClickListener { changed((value-1).coerceAtLeast(-100)); } }, LinearLayout.LayoutParams(dp(48),dp(48)))
        r.addView(TextView(this).apply { text = value.toString(); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(dp(58),dp(48)))
        r.addView(Button(this).apply { text = "+"; textSize = 20f; setOnClickListener { changed((value+1).coerceAtMost(100)); } }, LinearLayout.LayoutParams(dp(48),dp(48)))
        parent.addView(r)
    }

    private fun printexChooseCopies() {
        val values = (1..20).map { it.toString() }.toTypedArray()
        printexChoose("Copies", values) { printexCopies = it.toInt(); renderTab(3) }
    }

    private fun printexChoose(title: String, values: Array<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(values, values.indexOfFirst { it == when(title) { "Copies" -> printexCopies.toString(); "Pages" -> printexPages; "Paper Size" -> printexPaper; "Orientation" -> printexOrientation; "Scaling" -> printexScaling; "Layout" -> printexLayout; "Double-Sided Printing" -> printexDuplex; else -> "" } }.coerceAtLeast(0)) { d, which -> onSelected(values[which]); d.dismiss() }.setNegativeButton("CANCEL", null).show()
    }

    private fun printexPrintSettings() {
        val items = arrayOf("Copies","Pages","Multi-Page Printing","Double-Sided Printing","Paper Size & Layout","Paper & Printing Options","Default")
        AlertDialog.Builder(this).setTitle("Print Settings").setItems(items) { _, which ->
            when (which) {
                0 -> printexChooseCopies()
                1 -> printexChoose("Pages", arrayOf("All pages","Odd pages","Even pages","Selected pages","Page range")) { printexPages = it; renderTab(3) }
                2 -> printexChoose("Multi-Page Printing", arrayOf("1 page per sheet","2 pages per sheet","4 pages per sheet","6 pages per sheet","9 pages per sheet")) { printexLayout = it; renderTab(3) }
                3 -> printexChoose("Double-Sided Printing", arrayOf("One side","Duplex • Long edge","Duplex • Short edge")) { printexDuplex = it; renderTab(3) }
                4 -> printexChoose("Paper Size & Layout", arrayOf("A4","A5","A3","Letter","Legal","4 × 6 in","5 × 7 in")) { printexPaper = it; renderTab(3) }
                5 -> printexChoose("Paper & Printing Options", arrayOf("Auto","Color","Black & White","Draft","High Quality")) { renderTab(3) }
                6 -> { printexCopies=1; printexPages="All pages"; printexPaper="A4"; printexOrientation="Auto"; printexScaling="Fit to page"; printexLayout="1 page per sheet"; printexDuplex="One side"; renderTab(3); Toast.makeText(this,"Print Settings restored to default",Toast.LENGTH_SHORT).show() }
            }
        }.show()
    }

    private fun printexChooseFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*"; putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/pdf","image/*","text/plain","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }
        startActivityForResult(i,8101)
    }

    private fun printexDisplayName(): String = printexUri?.let { uri -> try { contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c -> if(c.moveToFirst()) c.getString(0) else uri.lastPathSegment ?: "Document" } ?: (uri.lastPathSegment ?: "Document") } catch(_:Exception) { uri.lastPathSegment ?: "Document" } } ?: "Document"

    private fun copyPrintexUriToCache(uri: Uri): File? = try { val ext = when { contentResolver.getType(uri)?.contains("pdf",true)==true -> ".pdf"; contentResolver.getType(uri)?.startsWith("image/")==true -> ".img"; else -> ".bin" }; val f=File(cacheDir,"printex_${System.currentTimeMillis()}$ext"); contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(f).use { out -> input.copyTo(out) } }; f } catch(_:Exception) { null }

    private fun printexLoadPreviewBitmap(): Bitmap? {
        val file=printexFile ?: return null
        return try {
            if (file.extension.equals("pdf",true)) {
                val pfd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer=android.graphics.pdf.PdfRenderer(pfd)
                if(renderer.pageCount==0){renderer.close();pfd.close();return null}
                val page=renderer.openPage(0)
                val scale=2.5f
                val bmp=Bitmap.createBitmap((page.width*scale).toInt().coerceAtMost(2400),(page.height*scale).toInt().coerceAtMost(3400),Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close();renderer.close();pfd.close();bmp
            } else android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        } catch(_:Exception) { null }
    }

    private fun printexOpenPreview() { if(printexUri==null) { Toast.makeText(this,"Select a document first",Toast.LENGTH_SHORT).show(); return }; val b=printexLoadPreviewBitmap(); if(b==null){Toast.makeText(this,"Preview is not available for this file type",Toast.LENGTH_SHORT).show();return}; printexPreview?.setImageBitmap(b); printexZoom=1f; renderTab(3) }

    private fun printexZoomTouch(v: View, e: android.view.MotionEvent): Boolean {
        if(e.pointerCount==2 && e.actionMasked==android.view.MotionEvent.ACTION_MOVE){ val dx=e.getX(0)-e.getX(1); val dy=e.getY(0)-e.getY(1); val dist=kotlin.math.sqrt(dx*dx+dy*dy); if(!::printexZoom.isInitialized){}; if(dist>120) printexZoom=(printexZoom*1.015f).coerceAtMost(4f); else printexZoom=(printexZoom/1.015f).coerceAtLeast(1f); val m=android.graphics.Matrix(); m.setScale(printexZoom,printexZoom,v.width/2f,v.height/2f); (v as ImageView).imageMatrix=m; return true }; return true }

    private fun printexPrint() {
        val file=printexFile ?: run { Toast.makeText(this,"Select a PDF or image first",Toast.LENGTH_SHORT).show(); return }
        val printable = if(file.extension.equals("pdf",true)) file else printexImageToPdf(file)
        if(printable==null){Toast.makeText(this,"This document could not be prepared for printing",Toast.LENGTH_LONG).show();return}
        val pm=getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val adapter=object:android.print.PrintDocumentAdapter(){
            override fun onLayout(oldAttributes:android.print.PrintAttributes?,newAttributes:android.print.PrintAttributes?,cancellationSignal:android.os.CancellationSignal?,callback:android.print.PrintDocumentAdapter.LayoutResultCallback?,extras:android.os.Bundle?){ if(cancellationSignal?.isCanceled==true){callback?.onLayoutCancelled();return}; callback?.onLayoutFinished(android.print.PrintDocumentInfo.Builder(printable.name).setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build(),true) }
            override fun onWrite(pages: Array<android.print.PageRange>?,destination:android.os.ParcelFileDescriptor?,cancellationSignal:android.os.CancellationSignal?,callback:android.print.PrintDocumentAdapter.WriteResultCallback?){ try{ if(cancellationSignal?.isCanceled==true){callback?.onWriteCancelled();return}; FileInputStream(printable).use{input->FileOutputStream(destination?.fileDescriptor).use{out->input.copyTo(out)}}; callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES)) }catch(e:Exception){callback?.onWriteFailed(e.message)}}
        }
        pm.print("Printex • ${printexDisplayName()}",adapter,android.print.PrintAttributes.Builder().build())
    }

    private fun printexImageToPdf(file:File):File?=try{ val bmp=android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return null; val out=File(cacheDir,"printex_image_${System.currentTimeMillis()}.pdf"); val pdf=android.graphics.pdf.PdfDocument(); val page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width.coerceAtLeast(1),bmp.height.coerceAtLeast(1),1).create()); page.canvas.drawBitmap(bmp,0f,0f,null); pdf.finishPage(page); FileOutputStream(out).use{pdf.writeTo(it)}; pdf.close(); out }catch(_:Exception){null}

'''
    marker = '    private fun renderTools() {'
    if marker not in s:
        raise SystemExit('renderTools anchor not found')
    s = s.replace(marker, method + marker, 1)
    MAIN.write_text(s, encoding='utf-8')
    print('Complete integrated Printex implementation added')
else:
    print('Printex implementation already present; no duplicate changes')
