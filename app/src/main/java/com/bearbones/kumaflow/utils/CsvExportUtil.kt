package com.bearbones.kumaflow.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.bearbones.kumaflow.TransactionWithSplits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.util.Locale

object CsvExportUtil {
    suspend fun exportTransactionsToCsv(
        context: Context,
        goalName: String,
        transactions: List<TransactionWithSplits>
    ) = withContext(Dispatchers.IO) {
        try {
            val fileName = "Riwayat_Tabungan_${goalName}_${System.currentTimeMillis()}.csv"
            
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external"), contentValues)
            
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        // Write header
                        writer.write("Tanggal,Tipe,Kategori,Nama Transaksi,Nominal\n")
                        
                        // Write data
                        transactions.forEach { txw ->
                            val tx = txw.transaction
                            val date = try {
                                if (tx.timestamp.isNotBlank()) {
                                    val dt = java.time.LocalDateTime.parse(tx.timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    dt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
                                } else {
                                    val parsed = java.time.LocalDate.parse(tx.date, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    parsed.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
                                }
                            } catch (_: Exception) {
                                tx.date
                            }
                            val type = if (tx.isIncome) "Masuk" else "Keluar"
                            val cat = tx.category.replace(",", "")
                            val name = tx.name.replace(",", " ")
                            val amount = tx.amount
                            writer.write("$date,$type,$cat,$name,$amount\n")
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Berhasil diunduh ke folder Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Terjadi kesalahan saat mengunduh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun exportTransactionsToPdf(
        context: Context,
        goalName: String,
        transactions: List<TransactionWithSplits>
    ) = withContext(Dispatchers.IO) {
        try {
            val fileName = "Riwayat_Tabungan_${goalName}_${System.currentTimeMillis()}.pdf"
            
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external"), contentValues)
            
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    var pageNum = 1
                    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 format
                    var page = pdfDocument.startPage(pageInfo)
                    
                    val paint = android.graphics.Paint()
                    val titlePaint = android.graphics.Paint().apply { isFakeBoldText = true; textSize = 18f; color = android.graphics.Color.BLACK }
                    val headerPaint = android.graphics.Paint().apply { isFakeBoldText = true; textSize = 12f; color = android.graphics.Color.DKGRAY }
                    
                    var logoBitmap: android.graphics.Bitmap? = null
                    try {
                        val drawable = androidx.core.content.ContextCompat.getDrawable(context, com.bearbones.kumaflow.R.mipmap.ic_launcher)
                        if (drawable != null) {
                            logoBitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                            val canvasBitmap = android.graphics.Canvas(logoBitmap)
                            drawable.setBounds(0, 0, canvasBitmap.width, canvasBitmap.height)
                            drawable.draw(canvasBitmap)
                        }
                    } catch (e: Exception) {}

                    fun drawSavingsHeaders(canvas: android.graphics.Canvas, pNum: Int) {
                        if (logoBitmap != null) {
                            val destRect = android.graphics.RectF(40f, 40f, 90f, 90f)
                            canvas.drawBitmap(logoBitmap!!, null, destRect, paint)
                            canvas.drawText("LAPORAN TABUNGAN ($pNum)", 100f, 50f, titlePaint)
                            canvas.drawText("Currency: IDR | Tabungan: $goalName", 100f, 75f, android.graphics.Paint().apply { textSize = 12f })
                        } else {
                            canvas.drawText("LAPORAN TABUNGAN ($pNum)", 40f, 50f, titlePaint)
                            canvas.drawText("Currency: IDR | Tabungan: $goalName", 40f, 75f, android.graphics.Paint().apply { textSize = 12f })
                        }
                        
                        canvas.drawLine(40f, 95f, 550f, 95f, paint)
                        canvas.drawText("Tanggal", 40f, 115f, headerPaint)
                        canvas.drawText("Tipe", 150f, 115f, headerPaint)
                        canvas.drawText("Transaksi", 250f, 115f, headerPaint)
                        canvas.drawText("Nominal", 450f, 115f, headerPaint)
                        canvas.drawLine(40f, 125f, 550f, 125f, paint)
                    }

                    drawSavingsHeaders(page.canvas, pageNum)
                    var y = 150f
                    
                    transactions.forEach { txw ->
                        if (y > 720f) {
                            pdfDocument.finishPage(page)
                            pageNum++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                            page = pdfDocument.startPage(pageInfo)
                            drawSavingsHeaders(page.canvas, pageNum)
                            y = 150f
                        }
                        
                        val tx = txw.transaction
                        val type = if (tx.isIncome) "Masuk" else "Keluar"
                        val name = if (tx.name.length > 25) tx.name.substring(0, 22) + "..." else tx.name
                        val numFormat = NumberFormat.getInstance(Locale("id", "ID"))
                        val amountLong = tx.amount.toLongOrNull() ?: 0L
                        val amountStr = "Rp " + numFormat.format(amountLong)
                        
                        val amountPrefix = if (tx.isIncome) "+" else "-"
                        val amountColor = if (tx.isIncome) android.graphics.Color.parseColor("#1B5E20") else android.graphics.Color.parseColor("#B71C1C")
                        
                        paint.color = android.graphics.Color.BLACK
                        val displayDate = try {
                            if (tx.timestamp.isNotBlank()) {
                                val dt = java.time.LocalDateTime.parse(tx.timestamp, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                dt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
                            } else {
                                val parsed = java.time.LocalDate.parse(tx.date, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                parsed.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
                            }
                        } catch (_: Exception) {
                            tx.date
                        }
                        page.canvas.drawText(displayDate, 40f, y, paint)
                        page.canvas.drawText(type, 150f, y, paint)
                        page.canvas.drawText(name, 250f, y, paint)
                        
                        paint.color = amountColor
                        page.canvas.drawText("$amountPrefix $amountStr", 450f, y, paint)
                        
                        y += 25f
                    }
                    
                    // Footer
                    if (y > 720f) {
                        pdfDocument.finishPage(page)
                        pageNum++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                        page = pdfDocument.startPage(pageInfo)
                        y = 50f
                    }
                    
                    paint.color = android.graphics.Color.BLACK
                    page.canvas.drawLine(40f, y, 550f, y, paint)
                    y += 20f
                    val inc = transactions.filter { it.transaction.isIncome }.sumOf { it.transaction.amount.toLongOrNull() ?: 0L }
                    val exp = transactions.filter { !it.transaction.isIncome }.sumOf { it.transaction.amount.toLongOrNull() ?: 0L }
                    val formatter = NumberFormat.getInstance(Locale("id", "ID"))
                    
                    page.canvas.drawText("Pemasukan: Rp ${formatter.format(inc)}", 40f, y, titlePaint.apply { textSize = 12f; color = android.graphics.Color.parseColor("#1B5E20") })
                    y += 20f
                    page.canvas.drawText("Pengeluaran: Rp ${formatter.format(exp)}", 40f, y, titlePaint.apply { textSize = 12f; color = android.graphics.Color.parseColor("#B71C1C") })
                    
                    y += 30f
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                    val printDateStr = "Dicetak pada: ${sdf.format(java.util.Date())}"
                    val footerTextPaint = android.graphics.Paint().apply { textSize = 10f; color = android.graphics.Color.WHITE; isFakeBoldText = true }
                    val textWidth = footerTextPaint.measureText(printDateStr)
                    val bgRect = android.graphics.RectF(40f, y - 12f, 40f + textWidth + 20f, y + 6f)
                    val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#1B5E20") }
                    
                    page.canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
                    page.canvas.drawText(printDateStr, 50f, y, footerTextPaint)
                    
                    pdfDocument.finishPage(page)
                    pdfDocument.writeTo(outputStream)
                    pdfDocument.close()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Berhasil diunduh ke folder Downloads: $fileName", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Terjadi kesalahan saat mengunduh PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
