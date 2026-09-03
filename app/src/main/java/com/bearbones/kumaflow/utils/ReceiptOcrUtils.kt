package com.bearbones.kumaflow.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ReceiptItem(
    val name: String,
    val price: Long
)

data class ReceiptParseResult(
    val merchantName: String? = null,
    val date: String? = null, // Format YYYY-MM-DD
    val total: Long? = null,
    val items: List<ReceiptItem> = emptyList(),
    val rawText: String = "",
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

object ReceiptOcrUtils {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Ekstrak seluruh teks mentah dari file gambar struk menggunakan Google ML Kit Text Recognition.
     */
    suspend fun extractRawText(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume("")
                    }
            }
        } catch (e: Exception) {
            ""
        } finally {
            recognizer.close()
        }
    }

    /**
     * Mem-parsing teks struk mentah menjadi JSON terstruktur menggunakan Anthropic Claude API (model claude-haiku-4-5-20251001).
     */
    suspend fun parseReceiptWithAI(rawText: String, apiKey: String): ReceiptParseResult = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext ReceiptParseResult(
                rawText = rawText,
                isSuccess = false,
                errorMessage = "Teks struk kosong"
            )
        }

        if (apiKey.isBlank()) {
            val fallback = fallbackRegexParse(rawText)
            return@withContext fallback.copy(
                errorMessage = "API key Anthropic belum dikonfigurasi"
            )
        }

        try {
            val prompt = """
                Ekstrak data dari teks struk belanja berikut. Kembalikan HANYA format JSON valid tanpa penjelasan atau blok markdown:
                {
                  "merchant_name": string|null,
                  "date": string|null (format YYYY-MM-DD),
                  "total": number|null,
                  "items": [
                    {"name": string, "price": number}
                  ]
                }
                Catatan:
                - Angka nominal dalam Rupiah murni tanpa titik atau koma ribuan (contoh: 25000, 150000).
                - Jika ada nama toko/merchant di bagian atas struk, ambil sebagai merchant_name.
                - Jika total akhir/grand total ditemukan, ambil sebagai total.
                - Field yang tidak ditemukan isi dengan null.

                Teks struk:
                $rawText
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", 1024)
                put("messages", messagesArray)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey.trim())
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val fallback = fallbackRegexParse(rawText)
                return@withContext fallback.copy(
                    errorMessage = "HTTP ${response.code}: $responseBody"
                )
            }

            val resObj = JSONObject(responseBody)
            val contentArray = resObj.optJSONArray("content")
            val rawAiText = if (contentArray != null && contentArray.length() > 0) {
                contentArray.getJSONObject(0).optString("text", "")
            } else {
                ""
            }

            // Bersihkan markdown jika model mengembalikan ```json ... ```
            val cleanedJson = cleanJsonString(rawAiText)
            val parsedJson = JSONObject(cleanedJson)

            val merchant = parsedJson.optString("merchant_name").takeIf { it.isNotBlank() && it != "null" }
            val dateStr = parsedJson.optString("date").takeIf { it.isNotBlank() && it != "null" }
            val totalNum = if (parsedJson.has("total") && !parsedJson.isNull("total")) {
                parsedJson.optLong("total", 0L).takeIf { it > 0 }
            } else null

            val itemsList = mutableListOf<ReceiptItem>()
            val itemsJsonArray = parsedJson.optJSONArray("items")
            if (itemsJsonArray != null) {
                for (i in 0 until itemsJsonArray.length()) {
                    val itm = itemsJsonArray.getJSONObject(i)
                    val itmName = itm.optString("name", "").trim()
                    val itmPrice = itm.optLong("price", 0L)
                    if (itmName.isNotBlank() && itmPrice > 0) {
                        itemsList.add(ReceiptItem(itmName, itmPrice))
                    }
                }
            }

            return@withContext ReceiptParseResult(
                merchantName = merchant,
                date = dateStr,
                total = totalNum ?: itemsList.sumOf { it.price }.takeIf { it > 0 },
                items = itemsList,
                rawText = rawText,
                isSuccess = true
            )

        } catch (e: Exception) {
            val fallback = fallbackRegexParse(rawText)
            return@withContext fallback.copy(
                errorMessage = e.localizedMessage ?: "Gagal parsing dengan AI"
            )
        }
    }

    /**
     * Membersihkan JSON string dari pembungkus markdown (```json ... ```).
     */
    private fun cleanJsonString(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```json")) {
            s = s.substring(7)
        } else if (s.startsWith("```")) {
            s = s.substring(3)
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length - 3)
        }
        return s.trim()
    }

    /**
     * Fallback parsing lokal berbasis pola regex jika pemanggilan API gagal atau offline.
     */
    fun fallbackRegexParse(rawText: String): ReceiptParseResult {
        var foundTotal: Long? = null
        var foundDate: String? = null
        var merchant: String? = null

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isNotEmpty()) {
            merchant = lines.firstOrNull { it.length > 2 && !it.contains(Regex("\\d{4}")) }
        }

        // Cari pola total / grand total / bayar
        val totalPattern = Pattern.compile("(?i)(?:total|grand total|tagihan|jumlah|bayar)[^\\d]*([\\d.,]+)")
        for (line in lines) {
            val matcher = totalPattern.matcher(line)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(".", "")?.replace(",", "") ?: ""
                val parsed = numStr.toLongOrNull()
                if (parsed != null && parsed > 0) {
                    foundTotal = parsed
                    break
                }
            }
        }

        // Cari pola tanggal (DD/MM/YYYY atau YYYY-MM-DD atau DD-MM-YYYY)
        val datePattern1 = Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})")
        val datePattern2 = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})")

        for (line in lines) {
            val m1 = datePattern1.matcher(line)
            if (m1.find()) {
                val y = m1.group(1)
                val m = m1.group(2)?.padStart(2, '0')
                val d = m1.group(3)?.padStart(2, '0')
                foundDate = "$y-$m-$d"
                break
            }
            val m2 = datePattern2.matcher(line)
            if (m2.find()) {
                val d = m2.group(1)?.padStart(2, '0')
                val m = m2.group(2)?.padStart(2, '0')
                val y = m2.group(3)
                foundDate = "$y-$m-$d"
                break
            }
        }

        return ReceiptParseResult(
            merchantName = merchant,
            date = foundDate,
            total = foundTotal,
            rawText = rawText,
            isSuccess = (foundTotal != null || merchant != null)
        )
    }
}
