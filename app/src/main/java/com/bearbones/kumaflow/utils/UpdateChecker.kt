package com.bearbones.kumaflow.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private const val UPDATE_URL = "https://raw.githubusercontent.com/Fancynest/kumaflow-releases/main/version.json"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Bypass GitHub Raw Cache dengan timestamp
            val url = URL("$UPDATE_URL?t=${System.currentTimeMillis()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                
                return@withContext UpdateInfo(
                    versionCode = jsonObject.optInt("versionCode", 0),
                    versionName = jsonObject.optString("versionName", ""),
                    apkUrl = jsonObject.optString("apkUrl", ""),
                    releaseNotes = jsonObject.optString("releaseNotes", "")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
