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
import java.io.File
import java.io.FileOutputStream

object ShareSplitBillUtils {

    fun generateQRWithText(context: Context, qrisFilePath: String, text: String): Uri? {
        if (qrisFilePath.isEmpty()) return null

        val file = File(qrisFilePath)
        if (!file.exists()) return null

        return try {
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (originalBitmap == null) return null

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

            // Draw the original QR code at the top
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

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
                append("Tolong scan QRIS di lampiran untuk transfer *Rp $finalAmountStr* ya.\n\nAtau bisa juga ke rekening ini:\n🏦 $bankName\n💳 $bankAccount\n\nThanks! ✨")
            } else if (hasQris) {
                append("Tolong scan QRIS di lampiran untuk transfer *Rp $finalAmountStr* ya.\n\nThanks! ✨")
            } else if (hasBank) {
                append("Tolong transfer *Rp $finalAmountStr* ke rekening ini ya:\n🏦 $bankName\n💳 $bankAccount\n\nThanks! ✨")
            } else {
                append("Halo! Ini rincian split bill via KumaFlow ya.\nBagian kamu totalnya jadi *Rp $finalAmountStr*.\n\nThanks! ✨")
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
