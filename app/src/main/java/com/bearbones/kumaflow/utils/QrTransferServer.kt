package com.bearbones.kumaflow.utils

import android.util.Base64
import com.bearbones.kumaflow.TransactionWithSplits
import com.bearbones.kumaflow.UserProfile
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicReference

interface DuoServerListener {
    fun onHandshakeRequest(payloadJson: String): Pair<Int, String> // Returns HTTP Status and Response JSON
    fun onSyncRequest(payloadJson: String, signature: String): Pair<Int, String>
}

class QrTransferServer(port: Int = 8080) : NanoHTTPD(port) {
    private val currentToken = AtomicReference<String>("")
    private var backupJsonCache: String = ""
    
    var duoListener: DuoServerListener? = null

    fun updateTokenAndData(token: String, profile: UserProfile, txs: List<TransactionWithSplits>) {
        currentToken.set(token)
        backupJsonCache = generateBackupJson(profile, txs)
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri == "/download-kuma") {
            val params = session.parameters
            val requestToken = params["token"]?.firstOrNull()
            
            if (requestToken != null && requestToken == currentToken.get()) {
                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    backupJsonCache
                ).apply {
                    addHeader("Content-Disposition", "attachment; filename=\"KumaFlow_Transfer.kuma\"")
                }
            } else {
                return newFixedLengthResponse(
                    Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Invalid or expired token"
                )
            }
        }
        if (session.method == Method.POST && uri == "/duo/handshake") {
            return try {
                val map = HashMap<String, String>()
                session.parseBody(map)
                val body = map["postData"] ?: ""
                val response = duoListener?.onHandshakeRequest(body)
                if (response != null) {
                    val status = if (response.first == 200) Response.Status.OK else Response.Status.BAD_REQUEST
                    newFixedLengthResponse(status, "application/json", response.second)
                } else {
                    newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, NanoHTTPD.MIME_PLAINTEXT, "Duo listener not set")
                }
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.message ?: "Unknown error")
            }
        }

        if (session.method == Method.POST && uri == "/duo/sync") {
            return try {
                val map = HashMap<String, String>()
                session.parseBody(map)
                val body = map["postData"] ?: ""
                val signature = session.headers["x-duo-signature"] ?: ""
                
                val response = duoListener?.onSyncRequest(body, signature)
                if (response != null) {
                    val status = if (response.first == 200) Response.Status.OK else Response.Status.BAD_REQUEST
                    newFixedLengthResponse(status, "application/json", response.second)
                } else {
                    newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, NanoHTTPD.MIME_PLAINTEXT, "Duo listener not set")
                }
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.message ?: "Unknown error")
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    }

    private fun generateBackupJson(profile: UserProfile, txsWithSplits: List<TransactionWithSplits>): String {
        val root = JSONObject()
        root.put("backupVersion", 6)

        val pJson = JSONObject().apply {
            put("userName", profile.userName)
            put("isAppLocked", profile.isAppLocked)
            put("appPin", profile.appPin)
            put("currency", profile.currency)
            put("dateFormat", profile.dateFormat)
            put("monthlyTarget", profile.monthlyTarget)
            put("themeMode", profile.themeMode)
            put("isReminderOn", profile.isReminderOn)
            put("reminderTimes", profile.reminderTimes)
            put("useCarryOver", profile.useCarryOver)
            put("expenseCats", profile.expenseCats)
            put("incomeCats", profile.incomeCats)
            put("wallets", profile.wallets)
            put("categoryTargets", profile.categoryTargets)
            put("isAmoledMode", profile.isAmoledMode)
            put("categoryIcons", profile.categoryIcons)
            put("isLiquidGlass", profile.isLiquidGlass)
            put("isPremiumGlassBlur", profile.isPremiumGlassBlur)
            put("isNavMotionEnabled", profile.isNavMotionEnabled)
            put("isParallaxEnabled", profile.isParallaxEnabled)
            put("currentStreak", profile.currentStreak)
            put("lastActiveDate", profile.lastActiveDate)
            put("freezeCount", profile.freezeCount)
            put("lastMilestoneNotified", profile.lastMilestoneNotified)
            put("qrisFilePath", profile.qrisFilePath)
            put("qrisHolderName", profile.qrisHolderName)
            put("bankName", profile.bankName)
            put("bankAccount", profile.bankAccount)
            if (profile.qrisFilePath.isNotEmpty()) {
                try {
                    val file = File(profile.qrisFilePath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64Str = Base64.encodeToString(bytes, Base64.DEFAULT)
                        put("qrisBase64", base64Str)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        root.put("profile", pJson)

        val tArr = JSONArray()
        txsWithSplits.forEach { obj ->
            val tJson = JSONObject().apply {
                put("name", obj.transaction.name)
                put("date", obj.transaction.date)
                put("amount", obj.transaction.amount)
                put("isIncome", obj.transaction.isIncome)
                put("category", obj.transaction.category)
                put("wallet", obj.transaction.wallet)
                put("timestamp", obj.transaction.timestamp)
                put("message", obj.transaction.message)
                put("isEdited", obj.transaction.isEdited)
            }
            if (obj.splits.isNotEmpty()) {
                val splitArr = JSONArray()
                obj.splits.forEach { s ->
                    splitArr.put(JSONObject().apply {
                        put("w", s.splitWallet)
                        put("a", s.splitAmount)
                    })
                }
                tJson.put("splits", splitArr)
            }
            tArr.put(tJson)
        }
        root.put("transactions", tArr)
        return root.toString()
    }
}
