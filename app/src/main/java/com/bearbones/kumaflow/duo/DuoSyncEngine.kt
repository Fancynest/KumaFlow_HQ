package com.bearbones.kumaflow.duo

import com.bearbones.kumaflow.KumaTransaction
import com.bearbones.kumaflow.TransactionSplit
import com.bearbones.kumaflow.KumaDatabase
import com.bearbones.kumaflow.duo.model.DuoConflictLog
import com.bearbones.kumaflow.duo.model.DuoPairing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.bearbones.kumaflow.TransactionWithSplits
import java.util.UUID

class DuoSyncEngine(
    private val database: KumaDatabase,
    private val secureStorage: DuoSecureStorage,
    private val localDeviceId: String
) {

    suspend fun generateSyncPayload(pairing: DuoPairing): String? {
        return withContext(Dispatchers.IO) {
            val dao = database.transactionDao()
            val sharedWalletIds = pairing.sharedWalletStableId.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (sharedWalletIds.isEmpty()) return@withContext null
            
            val walletsArray = JSONArray()
            val allLocalTxs = mutableListOf<TransactionWithSplits>()
            
            sharedWalletIds.forEach { walletId ->
                val walletMeta = dao.getWalletMetadataById(walletId) ?: return@forEach
                
                val walletObj = JSONObject().apply {
                    put("walletStableId", walletMeta.walletStableId)
                    put("walletCurrentName", walletMeta.currentName)
                    put("walletNameLastModified", walletMeta.nameLastModified)
                }
                walletsArray.put(walletObj)
                
                // Get local delta for this wallet
                val localTxs = dao.getTransactionsForSync(walletMeta.currentName, pairing.lastSyncedTimestamp)
                allLocalTxs.addAll(localTxs)
            }
            
            if (walletsArray.length() == 0) return@withContext null
            
            // Create Payload
            val payloadJson = JSONObject().apply {
                put("deviceId", localDeviceId)
                put("lastSyncedTimestamp", pairing.lastSyncedTimestamp)
                put("wallets", walletsArray)
                
                val txArray = JSONArray()
                allLocalTxs.forEach { txWithSplits ->
                    val tx = txWithSplits.transaction
                    val txObj = JSONObject().apply {
                        put("transactionUuid", tx.transactionUuid)
                        put("originDeviceId", tx.originDeviceId)
                        put("lastModified", tx.lastModified)
                        put("isDeleted", tx.isDeleted)
                        put("syncVersion", tx.syncVersion)
                        
                        put("name", tx.name)
                        put("date", tx.date)
                        put("amount", tx.amount)
                        put("isIncome", tx.isIncome)
                        put("category", tx.category)
                        put("wallet", tx.wallet) // Should be same as walletMeta.currentName
                        put("timestamp", tx.timestamp)
                        put("message", tx.message)
                        put("isEdited", tx.isEdited)
                    }
                    
                    if (txWithSplits.splits.isNotEmpty()) {
                        val splitsArray = JSONArray()
                        txWithSplits.splits.forEach { split ->
                            val splitObj = JSONObject().apply {
                                put("w", split.splitWallet)
                                put("a", split.splitAmount)
                            }
                            splitsArray.put(splitObj)
                        }
                        txObj.put("splits", splitsArray)
                    }
                    txArray.put(txObj)
                }
                put("transactions", txArray)
            }.toString()
            
            DuoCrypto.encrypt(payloadJson, pairing.pairingSecret)
        }
    }

    suspend fun startSync(pairing: DuoPairing, partnerIp: String, port: Int = 8080): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val dao = database.transactionDao()
                val currentTimestamp = System.currentTimeMillis()

                val encryptedPayload = generateSyncPayload(pairing) ?: return@withContext Result.failure(Exception("Failed to generate payload"))
                val signature = DuoCrypto.generateHmacSignature(encryptedPayload, pairing.pairingSecret)
                
                // Send Request
                val url = URL("http://$partnerIp:$port/duo/sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "text/plain")
                conn.setRequestProperty("X-Duo-Signature", signature)
                conn.connectTimeout = 5000
                conn.readTimeout = 10000
                
                OutputStreamWriter(conn.outputStream).use { it.write(encryptedPayload) }
                
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    // Partner responds with {"payload": "<encrypted>", "signature": "<hmac>"}
                    val responseJson = JSONObject(responseStr)
                    val responsePayload = responseJson.getString("payload")
                    val responseSignature = responseJson.getString("signature")
                    processIncomingPayload(responsePayload, responseSignature, pairing)
                    
                    // Update sync timestamp
                    val updatedPairing = pairing.copy(lastSyncedTimestamp = currentTimestamp)
                    dao.upsertPairing(updatedPairing)
                    
                    return@withContext Result.success("Sync completed successfully")
                } else {
                    return@withContext Result.failure(Exception("Partner sync failed (Code: ${conn.responseCode})"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    suspend fun processIncomingPayload(encryptedPayload: String, signature: String, pairing: DuoPairing): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!DuoCrypto.verifyHmacSignature(encryptedPayload, signature, pairing.pairingSecret)) {
                    return@withContext Result.failure(Exception("Invalid HMAC signature"))
                }
                
                val decryptedJson = DuoCrypto.decrypt(encryptedPayload, pairing.pairingSecret)
                val json = JSONObject(decryptedJson)
                
                val walletNameMap = mutableMapOf<String, String>() // Maps partner's wallet name to local final wallet name
                val dao = database.transactionDao()
                
                if (json.has("wallets")) {
                    val walletsArray = json.getJSONArray("wallets")
                    for (i in 0 until walletsArray.length()) {
                        val walletObj = walletsArray.getJSONObject(i)
                        val stableId = walletObj.getString("walletStableId")
                        val partnerWalletCurrentName = walletObj.getString("walletCurrentName")
                        val partnerWalletNameLastModified = walletObj.getLong("walletNameLastModified")
                        
                        val localWalletMeta = dao.getWalletMetadataById(stableId)
                        if (localWalletMeta == null) continue
                        
                        var finalWalletNameForIncomingTxs = localWalletMeta.currentName
                        
                        if (partnerWalletCurrentName != localWalletMeta.currentName) {
                            if (partnerWalletNameLastModified > localWalletMeta.nameLastModified) {
                                val existingMetaWithSameName = dao.getWalletMetadataByName(partnerWalletCurrentName)
                                if (existingMetaWithSameName != null && existingMetaWithSameName.walletStableId != stableId) {
                                    val disambiguatedName = "$partnerWalletCurrentName (Shared)"
                                    val conflictLog = DuoConflictLog(
                                        transactionUuid = "",
                                        walletStableId = stableId,
                                        reason = "Wallet renamed by partner but name collides. Auto-disambiguated to $disambiguatedName",
                                        originalDataJson = localWalletMeta.currentName
                                    )
                                    dao.insertConflictLog(conflictLog)
                                    
                                    dao.updateWalletName(localWalletMeta.currentName, disambiguatedName)
                                    dao.renameWalletStringInProfile(localWalletMeta.currentName, disambiguatedName)
                                    dao.upsertWalletMetadata(localWalletMeta.copy(
                                        currentName = disambiguatedName,
                                        nameLastModified = partnerWalletNameLastModified
                                    ))
                                    finalWalletNameForIncomingTxs = disambiguatedName
                                } else {
                                    dao.updateWalletName(localWalletMeta.currentName, partnerWalletCurrentName)
                                    dao.renameWalletStringInProfile(localWalletMeta.currentName, partnerWalletCurrentName)
                                    dao.upsertWalletMetadata(localWalletMeta.copy(
                                        currentName = partnerWalletCurrentName,
                                        nameLastModified = partnerWalletNameLastModified
                                    ))
                                    finalWalletNameForIncomingTxs = partnerWalletCurrentName
                                }
                            }
                        }
                        walletNameMap[partnerWalletCurrentName] = finalWalletNameForIncomingTxs
                    }
                }
                
                // --- Transaction Sync ---
                val txArray = json.getJSONArray("transactions")
                for (i in 0 until txArray.length()) {
                    val txObj = txArray.getJSONObject(i)
                    val txUuid = txObj.getString("transactionUuid")
                    val remoteLastModified = txObj.getLong("lastModified")
                    val remoteSyncVersion = txObj.getInt("syncVersion")
                    val remoteWalletName = txObj.getString("wallet")
                    val finalWalletName = walletNameMap[remoteWalletName] ?: remoteWalletName
                    
                    val existingTx = dao.getTransactionByUuid(txUuid)
                    
                    if (existingTx == null) {
                        // Insert new transaction
                        val newTx = KumaTransaction(
                            id = 0,
                            name = txObj.getString("name"),
                            date = txObj.getString("date"),
                            amount = txObj.getString("amount"),
                            isIncome = txObj.getBoolean("isIncome"),
                            category = txObj.getString("category"),
                            wallet = finalWalletName,
                            timestamp = txObj.getString("timestamp"),
                            message = txObj.optString("message", ""),
                            isEdited = txObj.getBoolean("isEdited"),
                            transactionUuid = txUuid,
                            originDeviceId = txObj.getString("originDeviceId"),
                            lastModified = remoteLastModified,
                            isDeleted = txObj.getBoolean("isDeleted"),
                            syncVersion = remoteSyncVersion
                        )
                        
                        val splits = mutableListOf<TransactionSplit>()
                        if (txObj.has("splits")) {
                            val splitsArray = txObj.getJSONArray("splits")
                            for (j in 0 until splitsArray.length()) {
                                val sObj = splitsArray.getJSONObject(j)
                                splits.add(TransactionSplit(splitWallet = sObj.getString("w"), splitAmount = sObj.getLong("a"), transactionId = 0))
                            }
                        }
                        
                        dao.insertFullTransaction(newTx, splits)
                        
                    } else {
                        // Conflict resolution: Last-write-wins based on lastModified
                        if (remoteLastModified > existingTx.lastModified) {
                            // Remote wins
                            val conflictLog = DuoConflictLog(
                                transactionUuid = txUuid,
                                walletStableId = pairing.sharedWalletStableId, // May be inaccurate if multiple, but acceptable for conflict logs
                                reason = "Transaction overwritten by remote sync",
                                originalDataJson = JSONObject().apply {
                                    put("name", existingTx.name)
                                    put("amount", existingTx.amount)
                                    put("wallet", existingTx.wallet)
                                    put("isDeleted", existingTx.isDeleted)
                                }.toString()
                            )
                            dao.insertConflictLog(conflictLog)
                            
                            val updatedTx = existingTx.copy(
                                name = txObj.getString("name"),
                                date = txObj.getString("date"),
                                amount = txObj.getString("amount"),
                                isIncome = txObj.getBoolean("isIncome"),
                                category = txObj.getString("category"),
                                wallet = finalWalletName,
                                timestamp = txObj.getString("timestamp"),
                                message = txObj.optString("message", ""),
                                isEdited = txObj.getBoolean("isEdited"),
                                lastModified = remoteLastModified,
                                isDeleted = txObj.getBoolean("isDeleted"),
                                syncVersion = remoteSyncVersion
                            )
                            
                            val splits = mutableListOf<TransactionSplit>()
                            if (txObj.has("splits")) {
                                val splitsArray = txObj.getJSONArray("splits")
                                for (j in 0 until splitsArray.length()) {
                                    val sObj = splitsArray.getJSONObject(j)
                                    splits.add(TransactionSplit(splitWallet = sObj.getString("w"), splitAmount = sObj.getLong("a"), transactionId = existingTx.id))
                                }
                            }
                            
                            dao.updateFullTransaction(updatedTx, splits)
                        } else {
                            // Local wins, ignore remote version
                        }
                    }
                }
                
                Result.success("Payload processed")
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
