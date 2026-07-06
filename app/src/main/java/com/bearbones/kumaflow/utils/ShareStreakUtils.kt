package com.bearbones.kumaflow.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.bearbones.kumaflow.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import com.bearbones.kumaflow.AppStr

object ShareStreakUtils {

    fun shareStreak(context: Context, profile: UserProfile, saveOnly: Boolean = false) {
        val width = 1080
        val height = 1920
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw Gradient Background
        val paint = Paint()
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#FF8E53")),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Draw Emoji
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        textPaint.textSize = 250f
        canvas.drawText("🔥", width / 2f, 750f, textPaint)
        
        // Draw KumaFlow at top
        textPaint.textSize = 80f
        canvas.drawText("KumaFlow", width / 2f, 250f, textPaint)
        
        // Draw Streak Number
        textPaint.textSize = 300f
        canvas.drawText("${profile.currentStreak}", width / 2f, 1100f, textPaint)
        
        // Draw Texts
        textPaint.textSize = 60f
        textPaint.color = Color.parseColor("#FFD54F") // Fire color
        canvas.drawText(if (AppStr.isId) "HARI BERTURUT-TURUT" else "DAY STREAK", width / 2f, 1250f, textPaint)
        
        // Reset color and alpha for bottom text
        textPaint.color = Color.WHITE
        textPaint.textSize = 50f
        textPaint.alpha = 200
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(profile.userName, 100f, height.toFloat() - 100f, textPaint)
        
        // Draw Date (Bottom Right)
        textPaint.textAlign = Paint.Align.RIGHT
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", if (AppStr.isId) Locale("id", "ID") else Locale.getDefault()))
        canvas.drawText(todayStr, width.toFloat() - 100f, height.toFloat() - 100f, textPaint)
        
        if (saveOnly) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "KumaFlow_Streak_${System.currentTimeMillis()}.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/KumaFlow")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val stream = resolver.openOutputStream(uri)
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        stream.close()
                        Toast.makeText(context, if (AppStr.isId) "Gambar disimpan ke Galeri!" else "Image saved to Gallery!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, if (AppStr.isId) "Gagal menyimpan gambar." else "Failed to save image.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Save to cache for sharing
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs() // don't forget to make the directory
                val file = File(cachePath, "streak_share.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
                
                // Share intent
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, if (AppStr.isId) "Bagikan Streak" else "Share Streak"))
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
