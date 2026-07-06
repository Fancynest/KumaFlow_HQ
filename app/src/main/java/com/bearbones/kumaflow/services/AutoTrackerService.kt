package com.bearbones.kumaflow.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.KumaTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AutoTrackerService : NotificationListenerService() {

    private val knownPackages = mapOf(
        "com.gojek.app" to "GoPay",
        "id.dana" to "DANA",
        "com.bca" to "BCA",
        "com.shopee.id" to "ShopeePay",
        "com.bcadigital.blu" to "blu",
        "com.bankjago.jago" to "Jago",
        "id.co.bri.brimo" to "BRImo",
        "com.bankmandiri.livin" to "Livin",
        "com.finaccel.android" to "Kredivo",
        "com.bni.mobile.ku" to "BNI Mobile",
        "id.or.ovo.ovo" to "OVO",
        "com.telkom.mwallet" to "LinkAja",
        "com.seabank.idn.mobile" to "SeaBank",
        "com.neocommerce.mobile" to "NeoBank"
    )

    private val expenseWords = listOf("bayar", "pembayaran", "keluar", "potongan", "transfer", "sent", "kirim", "mengirim")
    private val incomeWords = listOf("terima", "masuk", "top up", "cashback", "received")
    private val ignoreWords = listOf("blusaving", "blu saving", "pocket", "kantong", "jago", "pindah")

    private val amountRegex = Regex("Rp\\s?\\d{1,3}(?:\\.\\d{3})*", RegexOption.IGNORE_CASE)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        
        val prefs = applicationContext.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("enable_auto_tracker", false)
        if (!isEnabled) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val fullText = "$title $text".lowercase(Locale.getDefault())

        // Ignore internal transfers or unwanted notifications
        if (ignoreWords.any { fullText.contains(it) }) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = KumaDatabase.getDatabase(applicationContext)
            val profile = db.transactionDao().getProfileSync() ?: return@launch
            val userWallets = profile.wallets.split(",").map { it.trim() }

            val pm = applicationContext.packageManager
            val defaultAppName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName
            }
            
            val mappedWalletName = knownPackages[packageName] ?: defaultAppName
            
            // Check if user has a wallet that contains the mapped name or default app name
            val matchedWallet = userWallets.find { 
                it.contains(mappedWalletName, ignoreCase = true) || it.contains(defaultAppName, ignoreCase = true) 
            }
            
            if (matchedWallet == null) return@launch 

            analyzeAndInsert(matchedWallet, fullText, db, prefs)
        }
    }

    private suspend fun analyzeAndInsert(walletName: String, text: String, db: KumaDatabase, prefs: android.content.SharedPreferences) {
        val matchResult = amountRegex.find(text) ?: return
        val rawAmountStr = matchResult.value
        val cleanAmount = rawAmountStr.replace(Regex("[^\\d]"), "")
        if (cleanAmount.isEmpty() || cleanAmount == "0") return

        val isIncome = when {
            incomeWords.any { text.contains(it) } -> true
            expenseWords.any { text.contains(it) } -> false
            else -> return
        }

        val dateFormat = prefs.getString("dateFormat", "dd MMM yyyy") ?: "dd MMM yyyy"
        
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern(dateFormat, Locale.forLanguageTag("id-ID")))
        val timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val newTransaction = KumaTransaction(
            name = "Auto Record - $walletName",
            date = dateStr,
            amount = cleanAmount,
            isIncome = isIncome,
            category = "Others",
            wallet = walletName,
            timestamp = timestamp,
            message = "Auto-tracked from notification"
        )

        db.transactionDao().insertTransaction(newTransaction)
        com.bearbones.kumaflow.utils.StreakManager.checkAndUpdateStreak(db.transactionDao())
        Log.d("AutoTracker", "Inserted transaction: $cleanAmount from $walletName")
    }
}
