from pathlib import Path

main_path = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
manifest_path = Path('ScannerApp/app/src/main/AndroidManifest.xml')
reader_path = Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/PdfReaderActivity.kt')

main = main_path.read_text(encoding='utf-8')
manifest = manifest_path.read_text(encoding='utf-8')

old_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)'''
new_nav = '''        navButton("⌂", "Home", 0)\n        navButton("◎", "Studio", 1)\n        navButton("▦", "Library", 2)\n        navButton("⚙", "Tools", 3)\n        navButton("▤", "PDF Reader", 4)'''
if 'navButton("▤", "PDF Reader", 4)' not in main:
    if old_nav not in main: raise SystemExit('navigation block not found')
    main = main.replace(old_nav, new_nav, 1)

old_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            else -> renderTools()\n        }'''
new_when = '''        when (tab) {\n            0 -> renderHome()\n            1 -> renderStudio()\n            2 -> renderLibrary()\n            3 -> renderTools()\n            4 -> renderPdfReaderHome()\n        }'''
if '4 -> renderPdfReaderHome()' not in main:
    if old_when not in main: raise SystemExit('tab switch block not found')
    main = main.replace(old_when, new_when, 1)

if 'private fun renderPdfReaderHome()' not in main:
    marker = '    private fun renderTools() {'
    reader_home = '''    private fun renderPdfReaderHome() {\n        content.addView(title("PDF READER", "ADVANCED LOCAL PDF VIEWER • WHATSAPP READY"))\n        val hero = card()\n        hero.addView(TextView(this).apply {\n            text = "READ PDF DOCUMENTS"\n            textSize = 14f\n            typeface = Typeface.DEFAULT_BOLD\n            setTextColor(Color.rgb(130,160,200))\n        })\n        hero.addView(TextView(this).apply {\n            text = "Open any PDF from your phone, WhatsApp, Files or another app."\n            textSize = 20f\n            typeface = Typeface.DEFAULT_BOLD\n            setTextColor(Color.WHITE)\n            setPadding(0,dp(8),0,dp(4))\n        })\n        hero.addView(TextView(this).apply {\n            text = "Multi-page reading • Page jump • Zoom • Rotate • Share"\n            textSize = 12f\n            setTextColor(Color.rgb(150,165,185))\n        })\n        hero.addView(actionButton("OPEN PDF") {\n            startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {\n                addCategory(Intent.CATEGORY_OPENABLE)\n                type = "application/pdf"\n            })\n        }, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0,dp(18),0,0) })\n        content.addView(hero, margin(0,12))\n        val info = card()\n        info.addView(section("ADVANCED READER"))\n        listOf(\n            "✓ Open PDFs received from WhatsApp and other apps",\n            "✓ Previous / next page and direct page jump",\n            "✓ Zoom in / zoom out and fit-to-screen",\n            "✓ Rotate pages while reading",\n            "✓ Share the original PDF without re-scanning",\n            "✓ Works locally without automatic upload"\n        ).forEach { line -> info.addView(TextView(this).apply {\n            text = line; textSize = 13f; setTextColor(Color.rgb(185,198,215)); setPadding(0,dp(7),0,dp(7))\n        }) }\n        content.addView(info, margin(0,10))\n        content.addView(actionButton("OPEN FROM WHATSAPP / FILES") {\n            startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {\n                addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"\n            })\n        }, margin(0,10))\n    }\n\n'''
    if marker not in main: raise SystemExit('renderTools marker not found')
    main = main.replace(marker, reader_home + marker, 1)

manifest_activity = '''        <activity android:name=".PdfReaderActivity" android:exported="true">\n            <intent-filter>\n                <action android:name="android.intent.action.VIEW" />\n                <category android:name="android.intent.category.DEFAULT" />\n                <category android:name="android.intent.category.BROWSABLE" />\n                <data android:mimeType="application/pdf" />\n            </intent-filter>\n        </activity>\n'''
if '.PdfReaderActivity' not in manifest:
    anchor = '        <activity android:name=".LibraryActivity" android:exported="false" />\n'
    if anchor not in manifest: raise SystemExit('manifest anchor not found')
    manifest = manifest.replace(anchor, manifest_activity + anchor, 1)

# IMPORTANT: PdfReaderActivity.kt is now maintained as the canonical source file.
# Do not overwrite it here; previous versions of this script regenerated an older
# implementation and reintroduced the fillViewport/gravity compilation errors.

main_path.write_text(main, encoding='utf-8')
manifest_path.write_text(manifest, encoding='utf-8')
print('PDF Reader patch ready; canonical PdfReaderActivity.kt preserved')
