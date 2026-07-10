package com.bearbones.kumaflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.EncodeHintType
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.io.InputStream

data class DecodedQris(val payload: String, val bounds: android.graphics.Rect)

object DynamicQrisUtils {

    /**
     * Decodes a QR code image from a Uri into its raw string payload and bounding box.
     */
    fun decodeQRImage(context: Context, uri: Uri): DecodedQris? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            val points = result.resultPoints
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            
            if (points != null && points.isNotEmpty()) {
                for (p in points) {
                    if (p.x < minX) minX = p.x
                    if (p.y < minY) minY = p.y
                    if (p.x > maxX) maxX = p.x
                    if (p.y > maxY) maxY = p.y
                }
                
                val width = maxX - minX
                val height = maxY - minY
                val paddingX = width * 0.08f
                val paddingY = height * 0.08f
                
                var rectLeft = (minX - paddingX).toInt()
                var rectTop = (minY - paddingY).toInt()
                var rectRight = (maxX + paddingX).toInt()
                var rectBottom = (maxY + paddingY).toInt()
                
                if (rectLeft < 0) rectLeft = 0
                if (rectTop < 0) rectTop = 0
                if (rectRight > bitmap.width) rectRight = bitmap.width
                if (rectBottom > bitmap.height) rectBottom = bitmap.height
                
                val rect = android.graphics.Rect(rectLeft, rectTop, rectRight, rectBottom)
                return DecodedQris(result.text, rect)
            }
            return DecodedQris(result.text, android.graphics.Rect(0, 0, bitmap.width, bitmap.height))
        } catch (e: NotFoundException) {
            // QR Code not found in image (e.g. blurry, cropped, glare)
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }

    /**
     * Calculates the CRC16 CCITT-FALSE of the given payload.
     * Polynomial: 0x1021
     * Initial Value: 0xFFFF
     */
    fun calculateCRC16(payload: String): String {
        var crc = 0xFFFF
        for (i in payload.indices) {
            val c = payload[i].code
            crc = crc xor (c shl 8)
            for (j in 0 until 8) {
                if ((crc and 0x8000) != 0) {
                    crc = (crc shl 1) xor 0x1021
                } else {
                    crc = crc shl 1
                }
            }
        }
        return String.format("%04X", crc and 0xFFFF)
    }

    /**
     * Converts a Static QRIS payload to a Dynamic QRIS payload by injecting the amount.
     */
    fun generateDynamicQrisString(staticPayload: String, amount: Long): String? {
        if (!staticPayload.startsWith("000201")) return null

        var payload = staticPayload

        // 1. Change Point of Initiation Method (Tag 01) from "11" (Static) to "12" (Dynamic)
        if (payload.contains("010211")) {
            payload = payload.replaceFirst("010211", "010212")
        }

        // 2. Find the CRC tag (Tag 63)
        // EMVCo QR codes must end with the CRC tag: "6304" followed by 4 characters of the CRC
        val crcIndex = payload.lastIndexOf("6304")
        if (crcIndex == -1) return null

        // 3. Extract everything before the CRC
        val payloadWithoutCrc = payload.substring(0, crcIndex)

        // 4. Inject the Transaction Amount (Tag 54)
        var newPayloadPrefix = payloadWithoutCrc
        val amountStr = amount.toString()
        val lengthStr = String.format("%02d", amountStr.length)
        val amountTag = "54$lengthStr$amountStr"

        newPayloadPrefix += amountTag

        // 5. Inject Bill Number (Tag 62 Sub-tag 01)
        val billNumberId = "01"
        val billNumberValue = "KMA" + (System.currentTimeMillis() % 100000000L).toString()
        val billNumberLength = String.format("%02d", billNumberValue.length)
        val billNumberTag = billNumberId + billNumberLength + billNumberValue

        var index = 0
        var tag62StartIndex = -1
        var tag62EndIndex = -1
        var tag62Value = ""
        
        while (index < newPayloadPrefix.length - 4) {
            val tag = newPayloadPrefix.substring(index, index + 2)
            val tagLengthStr = newPayloadPrefix.substring(index + 2, index + 4)
            val length = tagLengthStr.toIntOrNull() ?: break
            
            if (tag == "62") {
                tag62StartIndex = index
                tag62EndIndex = index + 4 + length
                if (tag62EndIndex <= newPayloadPrefix.length) {
                    tag62Value = newPayloadPrefix.substring(index + 4, tag62EndIndex)
                }
                break
            }
            index += 4 + length
        }

        if (tag62StartIndex != -1 && tag62Value.isNotEmpty()) {
            val newTag62Value = tag62Value + billNumberTag
            val newTag62Length = String.format("%02d", newTag62Value.length)
            val newTag62 = "62" + newTag62Length + newTag62Value
            newPayloadPrefix = newPayloadPrefix.substring(0, tag62StartIndex) + newTag62 + newPayloadPrefix.substring(tag62EndIndex)
        } else {
            val newTag62Length = String.format("%02d", billNumberTag.length)
            val newTag62 = "62" + newTag62Length + billNumberTag
            newPayloadPrefix += newTag62
        }

        // 6. Append the CRC tag header
        newPayloadPrefix += "6304"

        // 7. Calculate the new CRC
        val newCrc = calculateCRC16(newPayloadPrefix)

        // 8. Return the final string
        return newPayloadPrefix + newCrc
    }

    /**
     * Generates a Bitmap QR code from the given payload string.
     */
    fun encodeDynamicQris(payload: String, size: Int = 512): Bitmap? {
        try {
            val writer = QRCodeWriter()
            val hints = mapOf(EncodeHintType.MARGIN to 0)
            val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
