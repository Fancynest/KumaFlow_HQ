package com.bearbones.kumaflow.utils

import android.content.Context
import android.util.Base64
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.KumaTransaction
import com.bearbones.kumaflow.TransactionSplit
import com.bearbones.kumaflow.UserProfile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RestoreUtils {
    suspend fun parseAndRestoreJson(jsonToRestore: String, context: Context) {
        val root = JSONObject(jsonToRestore)
        val pObj = root.getJSONObject("profile")
        val newProfile = UserProfile(
            userName = pObj.optString("userName", "User"),
            isAppLocked = pObj.optBoolean("isAppLocked", false),
            appPin = pObj.optString("appPin", ""),
            currency = pObj.optString("currency", "IDR"),
            dateFormat = pObj.optString("dateFormat", "dd MMM yyyy"),
            monthlyTarget = pObj.optLong("monthlyTarget", 0L),
            themeMode = pObj.optInt("themeMode", 0),
            isReminderOn = pObj.optBoolean("isReminderOn", false),
            reminderTimes = pObj.optString("reminderTimes", "05:00,12:30,15:30,18:00,20:00"),
            useCarryOver = pObj.optBoolean("useCarryOver", false),
            expenseCats = pObj.optString("expenseCats", "Food,Shopping,Health,Transport,Education,Entertainment,Others"),
            incomeCats = pObj.optString("incomeCats", "Financial,Others"),
            wallets = pObj.optString("wallets", "Cash,Bank BCA,GoPay"),
            categoryTargets = pObj.optString("categoryTargets", "{}"),
            isAmoledMode = pObj.optBoolean("isAmoledMode", false),
            categoryIcons = pObj.optString("categoryIcons", "{}"),
            isLiquidGlass = pObj.optBoolean("isLiquidGlass", false),
            isPremiumGlassBlur = pObj.optBoolean("isPremiumGlassBlur", false),
            currentStreak = pObj.optInt("currentStreak", 0),
            lastActiveDate = pObj.optString("lastActiveDate", ""),
            freezeCount = pObj.optInt("freezeCount", 0),
            lastMilestoneNotified = pObj.optInt("lastMilestoneNotified", 0)
        )
        
        var restoredQrisPath = pObj.optString("qrisFilePath", "")
        val qrisBase64 = pObj.optString("qrisBase64", "")
        if (qrisBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(qrisBase64, Base64.DEFAULT)
                val file = File(context.filesDir, "qris_restored_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { it.write(bytes) }
                restoredQrisPath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val finalProfile = newProfile.copy(
            qrisFilePath = restoredQrisPath,
            qrisHolderName = pObj.optString("qrisHolderName", ""),
            bankName = pObj.optString("bankName", ""),
            bankAccount = pObj.optString("bankAccount", "")
        )

        val txsArr = root.getJSONArray("transactions")
        val txsWithSplits = mutableListOf<Pair<KumaTransaction, List<TransactionSplit>>>()

        for (i in 0 until txsArr.length()) {
            val tObj = txsArr.getJSONObject(i)
            var safeTimestamp = tObj.optString("timestamp", "")
            if (safeTimestamp.isBlank()) {
                safeTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }

            val baseTx = KumaTransaction(
                id = 0,
                name = tObj.optString("name", "Unknown"),
                date = tObj.optString("date", ""),
                amount = tObj.optString("amount", "0"),
                isIncome = tObj.optBoolean("isIncome", false),
                category = tObj.optString("category", "Others"),
                wallet = tObj.optString("wallet", "Cash"),
                timestamp = safeTimestamp,
                message = tObj.optString("message", ""),
                isEdited = tObj.optBoolean("isEdited", false)
            )

            val splitsArr = tObj.optJSONArray("splits")
            val currentSplits = mutableListOf<TransactionSplit>()
            if (splitsArr != null) {
                for (j in 0 until splitsArr.length()) {
                    val sObj = splitsArr.getJSONObject(j)
                    currentSplits.add(
                        TransactionSplit(
                            transactionId = 0,
                            splitWallet = sObj.optString("w", "Cash"),
                            splitAmount = sObj.optLong("a", 0L)
                        )
                    )
                }
            }
            txsWithSplits.add(Pair(baseTx, currentSplits))
        }

        val dao = KumaDatabase.getDatabase(context).transactionDao()
        dao.restoreDatabase(finalProfile, txsWithSplits)
        
        // Restore custom card images
        val customCardsArr = root.optJSONArray("customCards")
        if (customCardsArr != null) {
            val customCardsDir = File(context.filesDir, "custom_cards").apply { mkdirs() }
            for (i in 0 until customCardsArr.length()) {
                try {
                    val cardObj = customCardsArr.getJSONObject(i)
                    val name = cardObj.getString("name")
                    val data = cardObj.getString("data")
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    val file = File(customCardsDir, name)
                    FileOutputStream(file).use { it.write(bytes) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
