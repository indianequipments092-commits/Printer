from pathlib import Path

p = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
if not p.exists():
    raise SystemExit(f'Missing required file: {p}')
s = p.read_text(encoding='utf-8')

# Remove Printex-only forbidden legacy controls left by earlier generator passes.
s = s.replace('    private var printexRotation = 0\n', '')
s = '\n'.join(line for line in s.split('\n') if 'printexRotation' not in line and 'actionButton("SHARE")' not in line)

# Add state used by the real document renderer.
marker = '    private var printexPages = "All pages"\n'
if marker in s and 'private var printexPageSpec' not in s:
    s = s.replace(marker, marker + '    private var printexPageSpec = ""\n', 1)

# Put Library beside PDF/Photo/Preview.
old = 'source.addView(tile("▤", "PDF / File") { printexChooseFile() }, weight(1f,4))\n        source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f,4))\n        source.addView(tile("▦", "Preview") { printexOpenPreview() }, weight(1f,4))'
new = 'source.addView(tile("▤", "PDF / File") { printexChooseFile() }, weight(1f,4))\n        source.addView(tile("▧", "Photo") { printexChooseFile() }, weight(1f,4))\n        source.addView(tile("▦", "Library") { printexOpenLibrary() }, weight(1f,4))\n        source.addView(tile("▣", "Preview") { printexOpenPreview() }, weight(1f,4))'
if old in s:
    s = s.replace(old, new, 1)

# Remove Rotation and add a real Default reset action to Image Settings.
s = s.replace('        printexImageRow(ims, "Rotation", printexRotation) { printexRotation = it; renderTab(3) }\n', '', 1)
needle = '        printexImageRow(ims, "Contrast", printexContrast) { printexContrast = it; renderTab(3) }\n'
if needle in s and 'actionButton("DEFAULT")' not in s:
    s = s.replace(needle, needle + '        ims.addView(actionButton("DEFAULT") { printexResetDefaults() }, LinearLayout.LayoutParams(-1, dp(46)).apply { setMargins(0,dp(8),0,0) })\n', 1)

# Add a real manual page-range editor.
s = s.replace('printexSettingRow(ps, "Pages", printexPages) { printexChoose("Pages", arrayOf("All pages","Odd pages","Even pages","Selected pages","Page range")) { printexPages = it; renderTab(3) } }',
'''printexSettingRow(ps, "Pages", if (printexPages == "Page range" && printexPageSpec.isNotBlank()) "Page range: $printexPageSpec" else printexPages) {
            printexChoosePages()
        }''', 1)
old_pages = '    private fun printexChooseCopies() {'
if old_pages in s and 'private fun printexChoosePages()' not in s:
    helper = r'''    private fun printexChoosePages() {
        val values = arrayOf("All pages", "Odd pages", "Even pages", "Selected pages", "Page range")
        AlertDialog.Builder(this).setTitle("Pages").setItems(values) { _, which ->
            val selected = values[which]
            if (selected != "Page range") {
                printexPages = selected
                printexPageSpec = ""
                renderTab(3)
            } else {
                val input = EditText(this).apply { hint = "Example: 1,3,5 or 1-5 or 1,3-5,8"; setSingleLine(true); setText(printexPageSpec) }
                AlertDialog.Builder(this).setTitle("Page Range").setMessage("Enter pages such as 1,3,5 or 1-5").setView(input)
                    .setPositiveButton("APPLY") { _, _ ->
                        val value = input.text.toString().trim()
                        if (value.isBlank() || value.split(',').all { token -> token.trim().matches(Regex("\\d+(-\\d+)?")) }) {
                            printexPages = "Page range"; printexPageSpec = value; renderTab(3)
                        } else Toast.makeText(this, "Invalid page range", Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("CANCEL", null).show()
            }
        }.setNegativeButton("CANCEL", null).show()
    }

'''
    s = s.replace(old_pages, helper + old_pages, 1)

anchor = '    private fun dp('
if anchor not in s:
    raise SystemExit('Could not find dp() insertion anchor')
if 'private fun printexPageIndices(' not in s:
    code = r'''    private fun printexPageIndices(spec: String, pageCount: Int): List<Int> {
        if (pageCount <= 0) return emptyList()
        val out = linkedSetOf<Int>()
        spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            if (token.contains('-')) {
                val parts = token.split('-', limit = 2); val a = parts[0].toIntOrNull(); val b = parts[1].toIntOrNull()
                if (a != null && b != null && a >= 1 && b >= a) for (n in a..b) if (n <= pageCount) out.add(n - 1)
            } else token.toIntOrNull()?.let { n -> if (n in 1..pageCount) out.add(n - 1) }
        }
        return out.toList().sorted()
    }

    private fun printexResetDefaults() {
        printexBrightness = 0; printexContrast = 0; renderTab(3)
        Toast.makeText(this, "Image Settings restored: Brightness 0 • Contrast 0", Toast.LENGTH_SHORT).show()
    }

    private fun printexOpenLibrary() {
        val files = library.list()
        if (files.isEmpty()) { Toast.makeText(this, "Library is empty", Toast.LENGTH_SHORT).show(); return }
        val labels = files.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Library").setItems(labels) { _, which -> printexUseSelectedLibraryFile(files[which]) }.setNegativeButton("CANCEL", null).show()
    }

    private fun printexUseSelectedLibraryFile(file: File) {
        if (!file.exists()) { Toast.makeText(this, "Library file is no longer available", Toast.LENGTH_SHORT).show(); return }
        printexFile = file; printexUri = Uri.fromFile(file); printexPageSpec = ""; printexPages = "All pages"; printexZoom = 1f; renderTab(3)
    }

    private fun printexPaperSizePoints(): Pair<Int, Int> {
        val p = when (printexPaper) { "A5" -> 420 to 595; "A3" -> 842 to 1191; "Letter" -> 612 to 792; "Legal" -> 612 to 1008; "4 × 6 in" -> 288 to 432; "5 × 7 in" -> 360 to 504; else -> 595 to 842 }
        return if (printexOrientation == "Landscape") p.second to p.first else if (printexOrientation == "Portrait") p.first to p.second else p
    }

    private fun printexAdjust(src: Bitmap): Bitmap {
        if (printexBrightness == 0 && printexContrast == 0) return src
        return try {
            val c = 1f + printexContrast / 100f; val t = 128f * (1f - c) + printexBrightness * 2.55f
            val matrix = android.graphics.ColorMatrix(floatArrayOf(c,0f,0f,0f,t, 0f,c,0f,0f,t, 0f,0f,c,0f,t, 0f,0f,0f,1f,0f))
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(out).drawBitmap(src, 0f, 0f, android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(matrix) })
            out
        } catch (_: Throwable) { src }
    }

    private fun printexSourceBitmaps(): List<Bitmap> {
        val file = printexFile ?: return emptyList(); if (!file.exists()) return emptyList()
        if (!file.extension.equals("pdf", true)) return listOfNotNull(android.graphics.BitmapFactory.decodeFile(file.absolutePath))
        val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY); val renderer = android.graphics.pdf.PdfRenderer(pfd)
        val selected = when { printexPages == "Odd pages" -> (0 until renderer.pageCount).filter { it % 2 == 0 }; printexPages == "Even pages" -> (0 until renderer.pageCount).filter { it % 2 == 1 }; printexPages == "Page range" -> printexPageIndices(printexPageSpec, renderer.pageCount); else -> (0 until renderer.pageCount).toList() }
        val result = mutableListOf<Bitmap>()
        selected.forEach { index ->
            val page = renderer.openPage(index); val scale = minOf(1800f / page.width, 2400f / page.height).coerceAtLeast(1f)
            val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888); bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_PRINT); page.close(); result.add(printexAdjust(bmp))
        }
        renderer.close(); pfd.close(); return result
    }

    private fun printexPrintPdf(): File {
        val source = printexSourceBitmaps(); if (source.isEmpty()) throw IllegalStateException("No printable document selected")
        val repeated = mutableListOf<Bitmap>(); repeat(printexCopies.coerceIn(1, 99)) { source.forEach { repeated.add(it) } }
        val (pageW, pageH) = printexPaperSizePoints()
        val columns = when { printexLayout.startsWith("2") || printexLayout.startsWith("4") -> 2; printexLayout.startsWith("6") || printexLayout.startsWith("9") -> 3; else -> 1 }
        val rows = when { printexLayout.startsWith("4") -> 2; printexLayout.startsWith("6") -> 2; printexLayout.startsWith("9") -> 3; else -> 1 }
        val pdf = android.graphics.pdf.PdfDocument(); var pageNumber = 1; var i = 0
        while (i < repeated.size) {
            val page = pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber++).create()); val canvas = page.canvas; canvas.drawColor(Color.WHITE)
            val cellW = pageW.toFloat() / columns; val cellH = pageH.toFloat() / rows; var slot = 0
            while (slot < columns * rows && i < repeated.size) {
                val bmp = repeated[i++]; val left = slot % columns * cellW; val top = slot / columns * cellH; val availW = cellW - 24f; val availH = cellH - 24f
                val scale = when (printexScaling) { "Actual size" -> 1f; "Fill page" -> maxOf(availW / bmp.width, availH / bmp.height); else -> minOf(availW / bmp.width, availH / bmp.height) }
                val dw = bmp.width * scale; val dh = bmp.height * scale; val l = left + (cellW - dw) / 2f; val t = top + (cellH - dh) / 2f
                canvas.drawBitmap(bmp, null, android.graphics.RectF(l,t,l+dw,t+dh), android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)); slot++
            }
            pdf.finishPage(page)
        }
        val out = File(cacheDir, "Printex_${System.currentTimeMillis()}.pdf"); FileOutputStream(out).use { pdf.writeTo(it) }; pdf.close(); source.forEach { if (!it.isRecycled) it.recycle() }; return out
    }

    private fun printexPrint() {
        try {
            if (printexFile == null) throw IllegalStateException("Select a PDF, image, or Library file first")
            val printManager = getSystemService(PRINT_SERVICE) as android.print.PrintManager
            val media = when (printexPaper) { "A5" -> android.print.PrintAttributes.MediaSize.ISO_A5; "A3" -> android.print.PrintAttributes.MediaSize.ISO_A3; "Letter" -> android.print.PrintAttributes.MediaSize.NA_LETTER; "Legal" -> android.print.PrintAttributes.MediaSize.NA_LEGAL; else -> android.print.PrintAttributes.MediaSize.ISO_A4 }
            val initial = android.print.PrintAttributes.Builder().setMediaSize(if (printexOrientation == "Landscape") media.asLandscape() else media.asPortrait()).setColorMode(android.print.PrintAttributes.COLOR_MODE_COLOR).setDuplexMode(when { printexDuplex.contains("Long") -> android.print.PrintAttributes.DUPLEX_MODE_LONG_EDGE; printexDuplex.contains("Short") -> android.print.PrintAttributes.DUPLEX_MODE_SHORT_EDGE; else -> android.print.PrintAttributes.DUPLEX_MODE_NONE }).build()
            val adapter = object : android.print.PrintDocumentAdapter() {
                private var prepared: File? = null
                override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes, cancellationSignal: android.os.CancellationSignal, callback: android.print.PrintDocumentAdapter.LayoutResultCallback, extras: Bundle?) {
                    try {
                        if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }; prepared?.delete(); prepared = printexPrintPdf()
                        val pfd = android.os.ParcelFileDescriptor.open(prepared!!, android.os.ParcelFileDescriptor.MODE_READ_ONLY); val renderer = android.graphics.pdf.PdfRenderer(pfd); val count = renderer.pageCount; renderer.close(); pfd.close()
                        callback.onLayoutFinished(android.print.PrintDocumentInfo.Builder("Printex.pdf").setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(count).build(), true)
                    } catch (t: Throwable) { callback.onLayoutFailed(t.message ?: "Could not prepare print document") }
                }
                override fun onWrite(pages: Array<android.print.PageRange>, destination: android.os.ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal, callback: android.print.PrintDocumentAdapter.WriteResultCallback) {
                    try {
                        if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }; val file = prepared ?: throw IllegalStateException("Print document was not prepared")
                        java.io.FileInputStream(file).use { input -> FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) } }; callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (t: Throwable) { callback.onWriteFailed(t.message ?: "Could not write print document") }
                }
                override fun onFinish() { prepared?.delete() }
            }
            printManager.print("Printex", adapter, initial)
        } catch (t: Throwable) { Toast.makeText(this, "Print failed: ${t.message ?: "Unknown error"}", Toast.LENGTH_LONG).show() }
    }

'''
    s = s.replace(anchor, code + anchor, 1)

p.write_text(s, encoding='utf-8')
print('Complete Printex V1 implementation applied: Library selection, manual page ranges, real PDF rendering, image adjustments, copies, paper size, orientation, scaling, layout, duplex, and print adapter')
