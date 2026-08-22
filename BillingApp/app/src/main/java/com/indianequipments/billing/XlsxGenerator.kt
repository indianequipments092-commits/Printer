package com.indianequipments.billing

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object XlsxGenerator {
    private fun esc(v: String) = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    private fun amount(v: Double) = String.format(java.util.Locale.US, "%.2f", v)

    private fun cell(xml: String, ref: String, value: String?, numeric: Boolean = false): String {
        val pattern = Regex("<c r=\"" + Regex.escape(ref) + "\"[^>]*(?:/>|>.*?</c>)")
        val old = pattern.find(xml)?.value
        val style = old?.let { Regex("\\bs=\"([^\"]+)\"").find(it)?.groupValues?.get(1) }
        val attr = if (style != null) " s=\"$style\"" else ""
        val body = when {
            value == null || value.isBlank() -> "<c r=\"$ref\"$attr/>"
            numeric -> "<c r=\"$ref\"$attr><v>${esc(value)}</v></c>"
            else -> "<c r=\"$ref\"$attr t=\"inlineStr\"><is><t>${esc(value)}</t></is></c>"
        }
        if (old != null) return xml.replace(old, body)
        val rowNumber = ref.filter { it.isDigit() }
        val row = Regex("<row r=\"" + Regex.escape(rowNumber) + "\"[^>]*>.*?</row>").find(xml)
        return if (row != null) xml.replace(row.value, row.value.removeSuffix("</row>") + body + "</row>") else xml
    }

    fun generate(context: Context, template: File, output: File, bill: Bill) {
        require(template.exists()) { "Excel template not found" }
        val tmp = File.createTempFile("invoice-", ".xlsx", context.cacheDir)
        ZipInputStream(FileInputStream(template)).use { zin ->
            ZipOutputStream(FileOutputStream(tmp)).use { zout ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    val bytes = zin.readBytes()
                    zout.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == "xl/worksheets/sheet1.xml") {
                        var xml = bytes.toString(Charsets.UTF_8)

                        // Exact invoice header cells from the supplied workbook.
                        xml = cell(xml, "I4", bill.number)
                        xml = cell(xml, "K5", bill.date)
                        xml = cell(xml, "I6", bill.deliveryNote)
                        xml = cell(xml, "K18", bill.destination)

                        // Consignee / customer block.
                        xml = cell(xml, "A13", bill.customer.name)
                        xml = cell(xml, "A14", bill.customer.address)
                        xml = cell(xml, "C16", bill.customer.gstin)
                        xml = cell(xml, "C17", bill.customer.state)
                        xml = cell(xml, "C18", bill.customer.email)

                        var taxableTotal = 0.0
                        var gstTotal = 0.0
                        bill.lines.take(12).forEachIndexed { index, line ->
                            val row = 21 + index
                            taxableTotal += line.taxable
                            val gst = if (bill.taxMode == "NONE") 0.0 else line.taxable * line.item.gst / 100.0
                            gstTotal += gst
                            xml = cell(xml, "A$row", (index + 1).toString(), true)
                            xml = cell(xml, "B$row", line.description.ifBlank { line.item.name })
                            xml = cell(xml, "F$row", line.item.hsn)
                            xml = cell(xml, "G$row", amount(line.qty), true)
                            xml = cell(xml, "H$row", line.item.unit)
                            xml = cell(xml, "I$row", amount(line.rate), true)
                            xml = cell(xml, "J$row", amount(line.item.gst), true)
                            xml = cell(xml, "K$row", line.item.unit)
                            xml = cell(xml, "L$row", amount(line.taxable), true)
                        }

                        for (index in bill.lines.size.coerceAtMost(12) until 12) {
                            val row = 21 + index
                            listOf("A", "B", "F", "G", "H", "I", "J", "K", "L").forEach { col -> xml = cell(xml, "$col$row", null) }
                        }

                        val grand = taxableTotal + gstTotal
                        xml = cell(xml, "L33", amount(taxableTotal), true)
                        xml = cell(xml, "L34", amount(gstTotal), true)
                        xml = cell(xml, "L35", amount(grand), true)
                        xml = cell(xml, "A35", "Amount in Words: ${AmountWords.inr(grand)}")
                        xml = cell(xml, "F38", amount(taxableTotal), true)
                        xml = cell(xml, "F39", amount(taxableTotal), true)

                        if (bill.taxMode == "IGST") {
                            xml = cell(xml, "I38", "0.00", true)
                            xml = cell(xml, "I39", "0.00", true)
                            xml = cell(xml, "K38", "0.00", true)
                            xml = cell(xml, "K39", "0.00", true)
                            xml = cell(xml, "L38", amount(gstTotal), true)
                            xml = cell(xml, "L39", amount(gstTotal), true)
                        } else {
                            val half = gstTotal / 2.0
                            xml = cell(xml, "I38", amount(half), true)
                            xml = cell(xml, "I39", amount(half), true)
                            xml = cell(xml, "K38", amount(half), true)
                            xml = cell(xml, "K39", amount(half), true)
                            xml = cell(xml, "L38", amount(gstTotal), true)
                            xml = cell(xml, "L39", amount(gstTotal), true)
                        }
                        zout.write(xml.toByteArray(Charsets.UTF_8))
                    } else {
                        zout.write(bytes)
                    }
                    zout.closeEntry()
                }
            }
        }
        output.parentFile?.mkdirs()
        FileInputStream(tmp).use { input -> FileOutputStream(output).use { out -> input.copyTo(out) } }
        tmp.delete()
    }
}
