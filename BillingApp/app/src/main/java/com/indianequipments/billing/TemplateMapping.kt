package com.indianequipments.billing

/**
 * Template-driven Excel mapping model.
 *
 * The workbook remains the source of truth for the final printed/exported bill.
 * Each document type can have its own workbook and cell/range mapping.
 * Cell addresses are deliberately data-driven so the UI never hard-codes a
 * guessed invoice layout.
 */
enum class DocumentTemplateType { INVOICE, DELIVERY_CHALLAN, PROFORMA_INVOICE, QUOTATION }

enum class TemplateField {
    COMPANY_NAME,
    COMPANY_ADDRESS,
    COMPANY_GSTIN,
    CUSTOMER_NAME,
    CUSTOMER_ADDRESS,
    CUSTOMER_GSTIN,
    CUSTOMER_STATE,
    CUSTOMER_STATE_CODE,
    DOCUMENT_NUMBER,
    DOCUMENT_DATE,
    DESCRIPTION,
    HSN,
    QUANTITY,
    UNIT,
    RATE,
    DISCOUNT,
    TAXABLE_VALUE,
    GST_RATE,
    CGST_RATE,
    CGST_AMOUNT,
    SGST_RATE,
    SGST_AMOUNT,
    IGST_RATE,
    IGST_AMOUNT,
    ROUND_OFF,
    GRAND_TOTAL,
    AMOUNT_IN_WORDS,
    BANK_DETAILS,
    TERMS
}

data class CellBinding(
    val field: TemplateField,
    val cellOrRange: String,
    val sheetName: String? = null
)

data class TemplateProfile(
    val type: DocumentTemplateType,
    val workbookName: String,
    val bindings: List<CellBinding> = emptyList(),
    val itemStartRow: Int? = null,
    val itemEndRow: Int? = null
)

/**
 * Produces the exact financial-year invoice sequence requested by the app.
 * Example: IE/2026-27/16
 */
object InvoiceNumberFormat {
    fun format(prefix: String, financialYear: String, sequence: Long): String =
        "$prefix/$financialYear/$sequence"
}
