from pathlib import Path
p=Path('ScannerApp/app/src/main/java/com/indianequipments/usbscanner/MainActivity.kt')
s=p.read_text(encoding='utf-8')
s=s.replace('FileInputStream(prepared).use{i->FileOutputStream(d?.fileDescriptor).use{o->i.copyTo(o)}};cb?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))','val fd=d?.fileDescriptor?:throw IllegalStateException("No print destination");FileInputStream(prepared).use{i->FileOutputStream(fd).use{o->i.copyTo(o)}};cb?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))')
p.write_text(s,encoding='utf-8')
print('Final Printex build repair applied')