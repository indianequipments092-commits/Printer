# Advanced Billing App

A standalone Android GST billing module isolated under `BillingApp/` so the existing Printer/Scanner application is not modified.

## Documents
- Invoice
- Delivery Challan
- Proforma Invoice
- Quotation

## Masters
- Customer / Party profiles
- Item / Product profiles
- HSN, unit, GST and default rate
- Multiple saved descriptions per item using `|` choices
- Configurable invoice starting sequence

## Billing
- Financial-year numbering: `IE/2026-27/16`
- Customer selection with saved GST/address/state details
- Item selection with saved HSN/unit/GST/rate
- Description selection
- Quantity, rate and discount
- CGST + SGST, IGST or No Tax
- Automatic taxable value, GST and grand total
- Indian Rupee amount in words
- Up to 12 template line rows
- Bill history and summary

## Excel template
Open Settings in the app and import `IE INV professional.xlsx` (the user's exact workbook). The workbook is stored locally as the master template. Generated documents edit the workbook's existing `Sheet1` cells and preserve the rest of the workbook package, rather than drawing a new invoice design.

## APK
The repository includes a GitHub Actions workflow at `.github/workflows/build-billing-app.yml` that builds the debug APK and uploads it as an artifact on the billing branch and its pull requests.
