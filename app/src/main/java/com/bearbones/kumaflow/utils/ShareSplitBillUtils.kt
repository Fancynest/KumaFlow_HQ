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

    fun generateQRWithText(context: Context, qrisFilePath: String, text: String, amount: Long? = null): Uri? {
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

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 60f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Calculate padding needed for the text
            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val paddingY = textBounds.height() + 80
            val newHeight = originalBitmap.height + paddingY
            
            // Create a new bitmap that is slightly taller
            val canvasBitmap = Bitmap.createBitmap(originalBitmap.width, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)

            // Draw white background
            canvas.drawColor(Color.WHITE)

            // Draw the original QR code frame at the top
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            // If we successfully generated the dynamic QR, overlay it over the old QR code bounds
            if (dynamicQrisBitmap != null && qrBounds != null) {
                canvas.drawBitmap(dynamicQrisBitmap, qrBounds.left.toFloat(), qrBounds.top.toFloat(), null)
            }

            // Draw the text at the bottom
            val textX = canvasBitmap.width / 2f
            val textY = originalBitmap.height.toFloat() + (paddingY / 2f) + (textBounds.height() / 2f)
            canvas.drawText(text, textX, textY, paint)

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

    fun shareToWhatsApp(context: Context, imageUri: Uri?, finalAmountStr: String, bankName: String, bankAccount: String) {
        val hasQris = imageUri != null
        val hasBank = bankName.isNotBlank() && bankAccount.isNotBlank()

        val autoTextString = buildString {
            if (hasQris && hasBank) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan pindai QRIS terlampir atau transfer ke rekening berikut:\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                } else {
                    append("Here are the billing details for *Rp $finalAmountStr*. Please scan the attached QRIS or transfer to the following account:\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                }
            } else if (hasQris) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan pindai QRIS terlampir.\n\nTerima kasih.")
                } else {
                    append("Here are the billing details for *Rp $finalAmountStr*. Please scan the attached QRIS.\n\nThank you.")
                }
            } else if (hasBank) {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    append("Berikut rincian penagihan sejumlah *Rp $finalAmountStr*. Silakan transfer ke rekening berikut:\n🏦 $bankName\n💳 $bankAccount\n\nTerima kasih.")
                } else {
                    append("Here are the billing details for *Rp $finalAmountStr*. Please transfer to the following account:\n🏦 $bankName\n💳 $bankAccount\n\nThank you.")
                }
            } else {
                if (com.bearbones.kumaflow.AppStr.isId) {
                    append("Berikut rincian penagihan melalui KumaFlow. Total tagihan Anda adalah *Rp $finalAmountStr*.\n\nTerima kasih.")
                } else {
                    append("Here are the billing details via KumaFlow. Your total is *Rp $finalAmountStr*.\n\nThank you.")
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
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if WhatsApp is not installed
            val fallbackIntent = Intent.createChooser(intent, "Share Split Bill")
            context.startActivity(fallbackIntent)
        }
    }
}
