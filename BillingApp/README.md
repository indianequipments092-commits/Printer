# Advanced Billing App

Professional GST billing system planned around the user's existing Excel invoice template.

## Documents
- Invoice
- Delivery Challan
- Proforma Invoice
- Quotation

## Core masters
- Company profile
- Customer / party master
- Item master with HSN, unit, GST and reusable descriptions
- Financial-year document numbering

## Invoice numbering
Default format: `IE/YYYY-YY/sequence` (example: `IE/2026-27/16`).

## Billing engine
- Quantity × rate calculation
- Discount support
- CGST + SGST or IGST
- Taxable value, tax totals and grand total
- Amount in words
- Multiple line items

## Output
The user's Excel template remains the master visual format. Generated documents will map saved billing data into that template, with PDF/print output and bill history planned.

## Safety
This module is intentionally isolated under `BillingApp/` so the existing Printer/Scanner application remains untouched while billing is developed.
