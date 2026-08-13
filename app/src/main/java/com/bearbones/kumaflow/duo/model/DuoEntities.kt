package com.bearbones.kumaflow.duo.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallet_metadata")
data class WalletMetadata(
    @PrimaryKey val walletStableId: String = UUID.randomUUID().toString(),
    val currentName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val nameLastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "duo_pairings")
data class DuoPairing(
    @PrimaryKey val pairingId: String,
    val partnerDeviceId: String,
    val partnerDisplayName: String,
    val pairingSecret: String,
    val sharedWalletStableId: String,
    val pairedAt: Long = System.currentTimeMillis(),
    val lastSyncedTimestamp: Long = 0L,
    val isActive: Boolean = true
)

@Entity(tableName = "duo_conflict_log")
data class DuoConflictLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionUuid: String,
    val walletStableId: String,
    val conflictedAt: Long = System.currentTimeMillis(),
    val reason: String, // e.g., "Transaction edited by partner", "Wallet renamed by partner"
    val originalDataJson: String // The JSON representation of the local data that was overwritten
)
