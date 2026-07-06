package com.bearbones.kumaflow

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object WalletLogoManager {

    private val memoryCache = LruCache<String, ImageBitmap>(50)

    // Maps keyword → list of domains to try (first match wins)
    private val domainMap = mapOf(
        "gopay" to listOf("gojek.com", "gopay.co.id"),
        "gojek" to listOf("gojek.com"),
        "ovo" to listOf("ovo.id"),
        "dana" to listOf("dana.id"),
        "shopeepay" to listOf("shopeepay.co.id", "shopee.co.id"),
        "shopee" to listOf("shopee.co.id"),
        "bca" to listOf("bca.co.id"),
        "mandiri" to listOf("bankmandiri.co.id"),
        "bni" to listOf("bni.co.id"),
        "bri" to listOf("bri.co.id"),
        "bsi" to listOf("bankbsi.co.id"),
        "cimb" to listOf("cimbniaga.co.id"),
        "permata" to listOf("permatabank.com"),
        "danamon" to listOf("danamon.co.id"),
        "btn" to listOf("btn.co.id"),
        "mega" to listOf("bankmega.com"),
        "panin" to listOf("panin.co.id"),
        "maybank" to listOf("maybank.co.id"),
        "ocbc" to listOf("ocbcnisp.com"),
        "uob" to listOf("uob.co.id"),
        "hsbc" to listOf("hsbc.co.id"),
        "citibank" to listOf("citibank.co.id"),
        "dbs" to listOf("dbs.id"),
        "jago" to listOf("jago.com"),
        "paypal" to listOf("paypal.com"),
        "linkaja" to listOf("linkaja.id"),
        "blu" to listOf("blubybcadigital.id"),
        "seabank" to listOf("seabank.co.id"),
        "jenius" to listOf("jenius.com"),
        "qris" to listOf("bi.go.id"),
        "grab" to listOf("grab.com"),
        "grabpay" to listOf("grab.com"),
        "venmo" to listOf("venmo.com"),
        "cash" to listOf("cash.app"),
        "revolut" to listOf("revolut.com"),
        "monzo" to listOf("monzo.com"),
        "n26" to listOf("n26.com"),
        "wise" to listOf("wise.com"),
        "payoneer" to listOf("payoneer.com"),
        "skrill" to listOf("skrill.com"),
        "neteller" to listOf("neteller.com"),
        "alipay" to listOf("alipay.com"),
        "wechat" to listOf("wechat.com"),
        "apple" to listOf("apple.com"),
        "google" to listOf("pay.google.com"),
        "samsung" to listOf("samsung.com"),
        "paytm" to listOf("paytm.com"),
        "phonepe" to listOf("phonepe.com"),
        "gpay" to listOf("pay.google.com")
    )

    suspend fun getWalletLogo(context: Context, walletName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (walletName.isBlank()) return@withContext null
        val safeName = walletName.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        if (safeName.isEmpty()) return@withContext null

        memoryCache.get(safeName)?.let { return@withContext it }

        val dir = File(context.filesDir, "wallet_logos_v8")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$safeName.png")
        if (file.exists() && file.length() > 0L) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    memoryCache.put(safeName, imageBitmap)
                    return@withContext imageBitmap
                } else {
                    file.delete()
                }
            } catch (e: Exception) {
                file.delete()
            }
        }

        // Find matching domains
        val lowerName = walletName.trim().lowercase()
        val words = lowerName.split(Regex("[^a-z0-9]+"))

        var domains: List<String>? = null

        // 1. Exact word match
        for (word in words) {
            if (domainMap.containsKey(word)) {
                domains = domainMap[word]
                break
            }
        }

        // 2. Contains match
        if (domains == null) {
            for ((key, value) in domainMap) {
                if (lowerName.contains(key)) {
                    domains = value
                    break
                }
            }
        }

        // 3. Auto-guess
        if (domains == null) {
            val clean = lowerName.replace(Regex("[^a-z0-9]"), "")
            if (clean.length > 2) {
                domains = listOf("$clean.com")
            }
        }

        if (domains == null) return@withContext null

        // Try each domain with Google Favicon V2 FIRST (most reliable, fastest, always PNG)
        // Then Icon.horse as backup
        // Clearbit is REMOVED — it's dead (DNS fails or Cloudflare HTML traps)
        for (domain in domains) {
            val apiUrls = listOf(
                "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$domain&size=128",
                "https://icon.horse/icon/$domain"
            )

            for (apiUrl in apiUrls) {
                try {
                    val conn = URL(apiUrl).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.connectTimeout = 2000
                    conn.readTimeout = 3000
                    conn.instanceFollowRedirects = true

                    if (conn.responseCode == 200) {
                        val contentType = conn.contentType ?: ""
                        // Skip HTML responses (Cloudflare challenges)
                        if (contentType.contains("text/html", ignoreCase = true)) {
                            continue
                        }

                        val cl = conn.contentLength
                        // Skip Icon.horse generic tiny fallback
                        if (apiUrl.contains("icon.horse") && cl in 1..500) {
                            continue
                        }

                        conn.inputStream.use { input ->
                            FileOutputStream(file).use { fos ->
                                input.copyTo(fos)
                            }
                        }

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null && bitmap.width >= 16 && bitmap.height >= 16) {
                            val imageBitmap = bitmap.asImageBitmap()
                            memoryCache.put(safeName, imageBitmap)
                            return@withContext imageBitmap
                        } else {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    // Timeout or connection error — skip to next
                }
            }
        }

        return@withContext null
    }

    fun deleteWalletLogo(context: Context, walletName: String) {
        val safeName = walletName.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        if (safeName.isEmpty()) return
        memoryCache.remove(safeName)
    }
}

@Composable
fun rememberWalletLogo(context: Context, walletName: String): ImageBitmap? {
    var logo by remember(walletName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(walletName) {
        logo = WalletLogoManager.getWalletLogo(context, walletName)
    }
    return logo
}
