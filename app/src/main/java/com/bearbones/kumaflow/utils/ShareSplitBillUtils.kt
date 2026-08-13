package com.bearbones.kumaflow.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ShareSplitBillUtils {

    fun generateQRWithText(context: Context, qrisFilePath: String, merchantName: String, amountStr: String = "", amount: Long? = null): Uri? {
        if (qrisFilePath.isEmpty()) return null

        val file = File(qrisFilePath)
        if (!file.exists()) return null

        return try {
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (originalBitmap == null) return null

            var dynamicQrisBitmap: Bitmap? = null
            var qrBounds: Rect? = null

            if (amount != null) {
                try {
                    val fileUri = Uri.fromFile(file)
                    val decodedQris = DynamicQrisUtils.decodeQRImage(context, fileUri)
                    if (decodedQris != null) {
                        qrBounds = decodedQris.bounds
                        val dynamicPayload = DynamicQrisUtils.generateDynamicQrisString(decodedQris.payload, amount)
                        if (dynamicPayload != null) {
                            val boxWidth = qrBounds.width()
                            val dynamicBitmap = DynamicQrisUtils.encodeDynamicQris(dynamicPayload, boxWidth)
                            if (dynamicBitmap != null) {
                                dynamicQrisBitmap = dynamicBitmap
                            } else {
                                throw Exception("Failed to encode dynamic QRIS")
                            }
                        } else {
                            throw Exception("Failed to generate dynamic payload")
                        }
                    } else {
                        throw Exception("Failed to decode static QRIS")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Gagal bikin QR dinamis, pakai QR statis bawaan.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            var finalBitmap = originalBitmap
            if (dynamicQrisBitmap != null && qrBounds != null) {
                // Overlay dynamic QR on original
                val tempBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
                val tempCanvas = Canvas(tempBitmap)
                tempCanvas.drawBitmap(originalBitmap, 0f, 0f, null)
                tempCanvas.drawBitmap(dynamicQrisBitmap, qrBounds.left.toFloat(), qrBounds.top.toFloat(), null)
                finalBitmap = tempBitmap
            }
            
            val canvasBitmap = addQrisFrame(context, finalBitmap, merchantName, amountStr)

            // Save to cache directory
            val cacheFile = File(context.cacheDir, "split_bill_qr.jpg")
            val outputStream = FileOutputStream(cacheFile)
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            // Return content URI using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBillingDetails(context: Context, imageUri: Uri?, finalAmountStr: String, holderName: String, bankName: String, bankAccount: String) {
        val hasQris = imageUri != null
        val hasBank = bankName.isNotBlank() && bankAccount.isNotBlank()
        
        // Check if amount is actually provided or just empty/generic
        val hasAmount = finalAmountStr.isNotBlank() && finalAmountStr != "Total: Rp " && finalAmountStr != "Total: Rp 0"

        val autoTextString = buildString {
            if (hasQris && hasBank) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    if (hasAmount) append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan pindai QRIS terlampir atau transfer ke rekening berikut:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                    else append("Silakan pindai QRIS terlampir atau transfer ke rekening berikut:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                } else {
                    if (hasAmount) append("Here are the billing details for *Rp $finalAmountStr*. Please scan the attached QRIS or transfer to the following account:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                    else append("Please scan the attached QRIS or transfer to the following account:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                }
            } else if (hasQris) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    if (hasAmount) append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan pindai QRIS terlampir.\n\nTerima kasih.")
                    else append("Silakan pindai QRIS terlampir.\n\nTerima kasih.")
                } else {
                    if (hasAmount) append("Here are the billing details for *Rp $finalAmountStr*. Please scan the attached QRIS.\n\nThank you.")
                    else append("Please scan the attached QRIS.\n\nThank you.")
                }
            } else if (hasBank) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    if (hasAmount) append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan transfer ke rekening berikut:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                    else append("Silakan transfer ke rekening berikut:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                } else {
                    if (hasAmount) append("Here are the billing details for *Rp $finalAmountStr*. Please transfer to the following account:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                    else append("Please transfer to the following account:\n👤 $holderName\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                }
            } else {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    if (hasAmount) append("Berikut rincian penagihan melalui KumaFlow. Total tagihan Anda adalah *Rp $finalAmountStr*.\n\nTerima kasih.")
                    else append("Berikut rincian tagihan melalui KumaFlow.\n\nTerima kasih.")
                } else {
                    if (hasAmount) append("Here are the billing details via KumaFlow. Your total is *Rp $finalAmountStr*.\n\nThank you.")
                    else append("Here are the billing details via KumaFlow.\n\nThank you.")
                }
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, autoTextString)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooserIntent = Intent.createChooser(intent, if (com.bearbones.kumaflow.AppStr.isId) "Bagikan via" else "Share via")
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addQrisFrame(context: Context, originalBitmap: Bitmap, merchantName: String = "", amountStr: String = ""): Bitmap {
        val qrSize = originalBitmap.width // QR is square
        
        // Scale everything relative to QR size for proper proportions
        val sidePadding = (qrSize * 0.12f)
        val qrBoxPadding = (qrSize * 0.06f)
        val totalWidth = qrSize + (sidePadding * 2) + (qrBoxPadding * 2)
        
        // Header section heights
        val topPadding = qrSize * 0.10f
        val logoRowHeight = qrSize * 0.10f
        val amountHeight = if (amountStr.isNotBlank()) qrSize * 0.14f else 0f
        val amountGap = if (amountStr.isNotBlank()) qrSize * 0.06f else 0f
        val preQrGap = qrSize * 0.06f
        
        val headerTotalHeight = topPadding + logoRowHeight + amountGap + amountHeight + preQrGap
        val qrBoxHeight = qrSize + (qrBoxPadding * 2)
        val bottomPadding = qrSize * 0.08f
        
        val totalHeight = headerTotalHeight + qrBoxHeight + bottomPadding
        
        val canvasBitmap = Bitmap.createBitmap(totalWidth.toInt(), totalHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        // Light grey background
        canvas.drawColor(Color.parseColor("#F5F6F8"))

        // ── QRIS Logo (symbol only, cropped) ──
        val logoY = topPadding
        var qrisSymbolWidth = 0f
        
        try {
            val logoResId = context.resources.getIdentifier("qris_logo", "drawable", context.packageName)
            if (logoResId != 0) {
                val fullLogoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
                if (fullLogoBitmap != null) {
                    // Crop to ~42% width to get only the 「Q|RIS」 symbol part fully, without the side text
                    val cropWidth = (fullLogoBitmap.width * 0.42f).toInt()
                    val croppedLogo = Bitmap.createBitmap(fullLogoBitmap, 0, 0, cropWidth, fullLogoBitmap.height)
                    fullLogoBitmap.recycle()
                    
                    // Scale cropped QRIS symbol to fit logoRowHeight
                    val logoScale = logoRowHeight / croppedLogo.height
                    qrisSymbolWidth = croppedLogo.width * logoScale
                    
                    // Center the QRIS symbol
                    val startX = (totalWidth - qrisSymbolWidth) / 2f
                    val qrisDest = android.graphics.RectF(startX, logoY, startX + qrisSymbolWidth, logoY + logoRowHeight)
                    canvas.drawBitmap(croppedLogo, null, qrisDest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    croppedLogo.recycle()
                }
            }
        } catch (e: Exception) {
            // Fallback: draw text
            val qrisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = qrSize * 0.12f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
            }
            canvas.drawText("QRIS", totalWidth / 2f, logoY + logoRowHeight, qrisPaint)
        }

        // ── Amount (only for dynamic) ──
        if (amountStr.isNotBlank()) {
            val rpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = qrSize * 0.06f
                textAlign = Paint.Align.RIGHT
                typeface = android.graphics.Typeface.DEFAULT
            }
            val amountMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = qrSize * 0.12f
                textAlign = Paint.Align.LEFT
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            
            val amountOnly = amountStr.removePrefix("Rp ").removePrefix("Rp")
            val rpWidth = rpPaint.measureText("Rp ")
            val amountWidth = amountMainPaint.measureText(amountOnly)
            val totalTextWidth = rpWidth + amountWidth
            val startX = (totalWidth - totalTextWidth) / 2f
            
            val amountY = topPadding + logoRowHeight + amountGap + amountHeight
            canvas.drawText("Rp ", startX + rpWidth, amountY, rpPaint)
            canvas.drawText(amountOnly, startX + rpWidth, amountY, amountMainPaint)
        }

        // ── White QR Box with rounded corners ──
        val qrBoxLeft = sidePadding
        val qrBoxTop = headerTotalHeight
        val qrBoxRight = qrBoxLeft + qrSize + (qrBoxPadding * 2)
        val qrBoxBottom = qrBoxTop + qrBoxHeight
        val qrBoxRect = android.graphics.RectF(qrBoxLeft, qrBoxTop, qrBoxRight, qrBoxBottom)
        
        val cornerRadius = qrSize * 0.06f
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(qrBoxRect, cornerRadius, cornerRadius, boxPaint)

        // ── Red Triangle Accents (Gopay style) ──
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ED1C24") }
        val accentSize = qrSize * 0.2f
        
        // Top-Left red triangle
        val tlPath = android.graphics.Path().apply {
            moveTo(qrBoxRect.left, qrBoxRect.top + accentSize)
            lineTo(qrBoxRect.left, qrBoxRect.top + cornerRadius)
            quadTo(qrBoxRect.left, qrBoxRect.top, qrBoxRect.left + cornerRadius, qrBoxRect.top)
            lineTo(qrBoxRect.left + accentSize, qrBoxRect.top)
            close()
        }
        canvas.drawPath(tlPath, redPaint)
        
        // Bottom-Right red triangle
        val brPath = android.graphics.Path().apply {
            moveTo(qrBoxRect.right, qrBoxRect.bottom - accentSize)
            lineTo(qrBoxRect.right, qrBoxRect.bottom - cornerRadius)
            quadTo(qrBoxRect.right, qrBoxRect.bottom, qrBoxRect.right - cornerRadius, qrBoxRect.bottom)
            lineTo(qrBoxRect.right - accentSize, qrBoxRect.bottom)
            close()
        }
        canvas.drawPath(brPath, redPaint)

        // ── Draw QR Code centered in white box ──
        canvas.drawBitmap(originalBitmap, qrBoxLeft + qrBoxPadding, qrBoxTop + qrBoxPadding, null)
        
        return canvasBitmap
    }
}
