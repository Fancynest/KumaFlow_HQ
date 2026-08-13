package com.bearbones.kumaflow.duo

import android.content.Context
import android.provider.Settings
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.duo.model.DuoPairing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.UUID

class DuoPairingManager(private val context: Context, private val database: KumaDatabase) {
    private val secureStorage = DuoSecureStorage(context)
    
    fun getLocalDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }
    
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun generatePairingPayload(displayName: String): String {
        val secret = DuoCrypto.generatePairingSecret()
        val ip = getLocalIpAddress() ?: ""
        val pairingId = UUID.randomUUID().toString()
        
        val payload = JSONObject().apply {
            put("pairingId", pairingId)
            put("deviceId", getLocalDeviceId())
            put("displayName", displayName)
            put("secret", secret)
            put("ip", ip)
            put("timestamp", System.currentTimeMillis())
        }
        
        // Save the secret temporarily
        secureStorage.savePairingSecret("temp_pairing", secret)
        // Save the pairingId temporarily so the sender can use it too
        context.getSharedPreferences("duo_prefs", Context.MODE_PRIVATE)
            .edit().putString("temp_pairing_id", pairingId).apply()
        
        return payload.toString()
    }
    
    // Called when scanning a QR code
    suspend fun processScannedQrAndHandshake(qrPayload: String, localDisplayName: String): Result<DuoPairing> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(qrPayload)
                val partnerDeviceId = json.getString("deviceId")
                val partnerDisplayName = json.getString("displayName")
                val secret = json.getString("secret")
                val ip = json.getString("ip")
                val timestamp = json.getLong("timestamp")
                
                // Check expiry (5 minutes)
                if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                    return@withContext Result.failure(Exception("Kode QR sudah kadaluarsa (lebih dari 5 menit)"))
                }
                
                // Do Handshake on port 8081 to avoid colliding with DuoAutoSyncManager
                val url = URL("http://$ip:8081/duo/handshake")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                val reqBody = JSONObject().apply {
                    put("deviceId", getLocalDeviceId())
                    put("displayName", localDisplayName)
                }
                
                OutputStreamWriter(conn.outputStream).use { it.write(reqBody.toString()) }
                
                if (conn.responseCode == 200) {
                    val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val resJson = JSONObject(resStr)
                    if (resJson.getBoolean("success")) {
                        // Use the pairingId from the QR payload so both devices share the same ID
                        val pairingId = json.optString("pairingId", UUID.randomUUID().toString())
                        val pairing = DuoPairing(
                            pairingId = pairingId,
                            partnerDeviceId = partnerDeviceId,
                            partnerDisplayName = partnerDisplayName,
                            pairingSecret = secret,
                            sharedWalletStableId = "", // Will be selected later
                            pairedAt = System.currentTimeMillis(),
                            lastSyncedTimestamp = 0L,
                            isActive = true
                        )
                        secureStorage.savePairingSecret(pairingId, secret)
                        return@withContext Result.success(pairing)
                    } else {
                        return@withContext Result.failure(Exception("Handshake ditolak oleh partner"))
                    }
                } else {
                    return@withContext Result.failure(Exception("Gagal menghubungi partner (Code: ${conn.responseCode})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Called by the listener when receiving a handshake request
    fun handleHandshakeRequest(reqBody: String): Pair<Int, String> {
        return try {
            val json = JSONObject(reqBody)
            val deviceId = json.getString("deviceId")
            val displayName = json.getString("displayName")
            // In a real app we might prompt the user, but for now we auto-accept if they are on the pairing screen
            val res = JSONObject().apply {
                put("success", true)
                put("message", "Handshake accepted")
            }
            Pair(200, res.toString())
        } catch (e: Exception) {
            Pair(400, JSONObject().put("error", e.message).toString())
        }
    }
}
