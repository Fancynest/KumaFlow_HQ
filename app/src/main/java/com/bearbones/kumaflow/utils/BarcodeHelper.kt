package com.bearbones.kumaflow.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object BarcodeHelper {
    fun generateBarcode(text: String): Bitmap? {
        if (text.isBlank()) return null
        return try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(text, BarcodeFormat.CODE_128, 800, 200)
            val width = matrix.width
            val height = matrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.TRANSPARENT)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
