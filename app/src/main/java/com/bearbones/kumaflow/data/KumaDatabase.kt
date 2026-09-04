package com.bearbones.kumaflow

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions_fts")
@Fts4(contentEntity = KumaTransaction::class)
data class TransactionFTS(
    @ColumnInfo(name = "rowid")
    @PrimaryKey
    val rowId: Int,
    val name: String,
    val category: String,
    val message: String
)

@Immutable
@Entity(tableName = "transactions")
data class KumaTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val date: String,
    val amount: String,
    val isIncome: Boolean,
    val category: String,
    val wallet: String,
    val timestamp: String,
    val message: String = "",
    val isEdited: Boolean = false,
    
    // Kuma Duo Sync Fields
    val transactionUuid: String = java.util.UUID.randomUUID().toString(),
    val originDeviceId: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncVersion: Int = 1
)

@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(
            entity = KumaTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
@Immutable
data class TransactionSplit(
    @PrimaryKey(autoGenerate = true)
    val splitId: Int = 0,
    val transactionId: Int,
    val splitWallet: String,
    val splitAmount: Long
)

@Immutable
data class TransactionWithSplits(
    @Embedded
    val transaction: KumaTransaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val splits: List<TransactionSplit>
)

@Immutable
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val userName: String,
    val isAppLocked: Boolean = false,
    val appPin: String = "",
    val currency: String = "IDR",
    val dateFormat: String = "dd MMM yyyy",
    val monthlyTarget: Long = 0L,
    val themeMode: Int = 0,
    val isReminderOn: Boolean = false,
    val reminderTimes: String = "05:00,12:30,15:30,18:00,20:00",
    val useCarryOver: Boolean = false,
    val expenseCats: String = "Food,Shopping,Health,Transport,Education,Entertainment,Others",
    val incomeCats: String = "Financial,Others",
    val wallets: String = "",
    val categoryTargets: String = "{}",
    val isAmoledMode: Boolean = false,
    val categoryIcons: String = "{}",
    val isLiquidGlass: Boolean = false,
    val isPremiumGlassBlur: Boolean = false,
    val currentStreak: Int = 0,
    val lastActiveDate: String = "",
    val freezeCount: Int = 0,
    val lastMilestoneNotified: Int = 0,
    val qrisFilePath: String = "",
    val qrisHolderName: String = "",
    val bankName: String = "",
    val bankAccount: String = "",
    val hasSeenTutorial: Boolean = true,
    val savingsWallets: String = "",
    val savingsGoals: String = "{}",
    val isNavMotionEnabled: Boolean = true,
    val isParallaxEnabled: Boolean = true
)

@Entity(tableName = "virtual_wallets")
data class VirtualWallet(
    @PrimaryKey val name: String,
    val orderIndex: Int,
    val backgroundType: String,
    val backgroundValue: String,
    val cardNumber: String = "",
    val notes: String = "",
    val cardLabel: String = "ACCESS CARD"
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactionsWithSplits(): Flow<List<TransactionWithSplits>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllTransactionsWithSplitsSync(): List<TransactionWithSplits>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE wallet = :walletName AND isIncome = 0 AND CAST(amount AS INTEGER) <= :maxBudget AND isDeleted = 0
        GROUP BY name 
        ORDER BY timestamp DESC LIMIT 8
    """)
    fun getRecentExpensesForRoulette(walletName: String, maxBudget: Long): Flow<List<KumaTransaction>>

    @Transaction
    @Query("""
        SELECT t.* FROM transactions t 
        JOIN transactions_fts fts ON (t.id = fts.rowid) 
        WHERE transactions_fts MATCH :query AND t.isDeleted = 0
    """)
    fun searchTransactions(query: String): Flow<List<TransactionWithSplits>>

    @Insert
    suspend fun insertTransaction(transaction: KumaTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: KumaTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: KumaTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<KumaTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplit>)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :txId")
    suspend fun deleteSplitsByTxId(txId: Int)

    @Transaction
    suspend fun insertFullTransaction(transaction: KumaTransaction, splits: List<TransactionSplit>) {
        val parentId = insertTransaction(transaction).toInt()
        val splitsWithParentId = splits.map { it.copy(transactionId = parentId) }
        insertSplits(splitsWithParentId)
    }

    @Transaction
    suspend fun updateFullTransaction(transaction: KumaTransaction, splits: List<TransactionSplit>) {
        updateTransaction(transaction)
        deleteSplitsByTxId(transaction.id)
        val splitsWithParentId = splits.map { it.copy(transactionId = transaction.id) }
        insertSplits(splitsWithParentId)
    }

    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun getProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // --- Kuma Duo Methods ---
    @Query("SELECT * FROM wallet_metadata")
    suspend fun getAllWalletMetadata(): List<com.bearbones.kumaflow.duo.model.WalletMetadata>

    @Transaction
    suspend fun getOrGenerateAllWalletMetadata(): List<com.bearbones.kumaflow.duo.model.WalletMetadata> {
        val profile = getProfileSync() ?: return emptyList()
        val wallets = profile.wallets.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val allMeta = getAllWalletMetadata().toMutableList()
        val metaNames = allMeta.map { it.currentName }
        
        var addedNew = false
        val now = System.currentTimeMillis()
        for (w in wallets) {
            if (!metaNames.contains(w)) {
                val newMeta = com.bearbones.kumaflow.duo.model.WalletMetadata(
                    walletStableId = java.util.UUID.randomUUID().toString(),
                    currentName = w,
                    createdAt = now,
                    nameLastModified = now
                )
                upsertWalletMetadata(newMeta)
                allMeta.add(newMeta)
                addedNew = true
            }
        }
        return if (addedNew) getAllWalletMetadata() else allMeta
    }

    @Query("SELECT * FROM wallet_metadata WHERE currentName = :name LIMIT 1")
    suspend fun getWalletMetadataByName(name: String): com.bearbones.kumaflow.duo.model.WalletMetadata?

    @Query("SELECT * FROM wallet_metadata WHERE walletStableId = :id LIMIT 1")
    suspend fun getWalletMetadataById(id: String): com.bearbones.kumaflow.duo.model.WalletMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWalletMetadata(metadata: com.bearbones.kumaflow.duo.model.WalletMetadata)

    @Query("SELECT * FROM duo_pairings WHERE isActive = 1")
    suspend fun getActivePairings(): List<com.bearbones.kumaflow.duo.model.DuoPairing>
    
    @Query("SELECT * FROM duo_pairings WHERE isActive = 1")
    fun observeActivePairings(): kotlinx.coroutines.flow.Flow<List<com.bearbones.kumaflow.duo.model.DuoPairing>>

    @Query("SELECT * FROM duo_pairings WHERE sharedWalletStableId = :walletStableId AND isActive = 1 LIMIT 1")
    suspend fun getPairingByWalletId(walletStableId: String): com.bearbones.kumaflow.duo.model.DuoPairing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPairing(pairing: com.bearbones.kumaflow.duo.model.DuoPairing)

    @Insert
    suspend fun insertConflictLog(log: com.bearbones.kumaflow.duo.model.DuoConflictLog)

    @Transaction
    @Query("SELECT * FROM transactions WHERE wallet = :walletName AND lastModified > :sinceTimestamp")
    suspend fun getTransactionsForSync(walletName: String, sinceTimestamp: Long): List<TransactionWithSplits>

    @Query("SELECT * FROM transactions WHERE transactionUuid = :uuid LIMIT 1")
    suspend fun getTransactionByUuid(uuid: String): KumaTransaction?
    // --- Virtual Wallet Operations ---
    @Query("SELECT * FROM virtual_wallets ORDER BY orderIndex ASC")
    fun observeAllVirtualWallets(): Flow<List<VirtualWallet>>

    @Query("SELECT * FROM virtual_wallets ORDER BY orderIndex ASC")
    suspend fun getAllVirtualWallets(): List<VirtualWallet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVirtualWallet(wallet: VirtualWallet)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVirtualWallets(wallets: List<VirtualWallet>)

    @Query("DELETE FROM virtual_wallets WHERE name = :name")
    suspend fun deleteVirtualWallet(name: String)

    @Query("UPDATE virtual_wallets SET name = :newName WHERE name = :oldName")
    suspend fun updateVirtualWalletName(oldName: String, newName: String)

    @Transaction
    suspend fun renameVirtualWalletFully(oldName: String, wallet: VirtualWallet) {
        updateVirtualWalletName(oldName, wallet.name)
        renameWalletAndMetadata(oldName, wallet.name)
        upsertVirtualWallet(wallet)
        deleteVirtualWallet(oldName)
    }

    // -------------------------

    @Query("UPDATE transactions SET wallet = :newName WHERE wallet = :oldName")
    suspend fun updateTransactionsWalletName(oldName: String, newName: String)

    @Query("UPDATE transaction_splits SET splitWallet = :newName WHERE splitWallet = :oldName")
    suspend fun updateSplitsWalletName(oldName: String, newName: String)

    @Query("DELETE FROM transaction_splits")
    suspend fun clearSplits()
    
    @Transaction
    suspend fun updateWalletName(oldName: String, newName: String) {
        updateTransactionsWalletName(oldName, newName)
        updateSplitsWalletName(oldName, newName)
    }

    @Transaction
    suspend fun renameWalletStringInProfile(oldName: String, newName: String) {
        val profile = getProfileSync()
        if (profile != null) {
            val walletsList = profile.wallets.split(",").map { it.trim() }.toMutableList()
            val index = walletsList.indexOf(oldName)
            if (index != -1) {
                walletsList[index] = newName
                saveProfile(profile.copy(wallets = walletsList.joinToString(",")))
            }
        }
    }

    @Transaction
    suspend fun addWalletToProfile(walletName: String) {
        val profile = getProfileSync()
        if (profile != null) {
            val walletsList = profile.wallets.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (!walletsList.contains(walletName)) {
                walletsList.add(walletName)
                saveProfile(profile.copy(wallets = walletsList.joinToString(",")))
            }
        }
    }

    @Transaction
    suspend fun removeWalletFromProfile(walletName: String) {
        val profile = getProfileSync()
        if (profile != null) {
            val walletsList = profile.wallets.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            if (walletsList.contains(walletName)) {
                walletsList.remove(walletName)
                saveProfile(profile.copy(wallets = walletsList.joinToString(",")))
            }
        }
    }

    @Transaction
    suspend fun renameWalletAndMetadata(oldName: String, newName: String) {
        updateWalletName(oldName, newName)
        renameWalletStringInProfile(oldName, newName)
        // Find existing metadata and update it so stable ID is preserved
        val metas = getAllWalletMetadata()
        val meta = metas.find { it.currentName == oldName }
        if (meta != null) {
            upsertWalletMetadata(meta.copy(currentName = newName, nameLastModified = System.currentTimeMillis()))
        }
    }

    @Transaction
    suspend fun restoreDatabase(profile: UserProfile, transactionsWithSplits: List<Pair<KumaTransaction, List<TransactionSplit>>>) {
        saveProfile(profile)
        clearSplits()
        clearTransactions()
        transactionsWithSplits.forEach { (tx, splits) ->
            insertFullTransaction(tx.copy(id = 0), splits)
        }
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN isAmoledMode INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transaction_splits` (
                `splitId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `transactionId` INTEGER NOT NULL, 
                `splitWallet` TEXT NOT NULL, 
                `splitAmount` INTEGER NOT NULL, 
                FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_transactionId` ON `transaction_splits` (`transactionId`)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN categoryIcons TEXT NOT NULL DEFAULT '{}'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN isLiquidGlass INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN isPremiumGlassBlur INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN lastActiveDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN freezeCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN lastMilestoneNotified INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN qrisFilePath TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN bankAccount TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` USING FTS4(`name` TEXT, `category` TEXT, `message` TEXT, content=`transactions`)")
        db.execSQL("INSERT INTO `transactions_fts` (`transactions_fts`, `rowid`, `name`, `category`, `message`) SELECT 'rebuild', `id`, `name`, `category`, `message` FROM `transactions`")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `virtual_wallets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `allocatedBalance` REAL NOT NULL, 
                `colorHex` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE transactions ADD COLUMN virtualWalletId INTEGER DEFAULT NULL")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `virtual_wallets`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `new_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `date` TEXT NOT NULL, 
                `amount` TEXT NOT NULL, 
                `isIncome` INTEGER NOT NULL, 
                `category` TEXT NOT NULL, 
                `wallet` TEXT NOT NULL, 
                `timestamp` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                `isEdited` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO new_transactions SELECT id, name, date, amount, isIncome, category, wallet, timestamp, message, isEdited FROM transactions")
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE new_transactions RENAME TO transactions")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN hasSeenTutorial INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN qrisHolderName TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create WalletMetadata table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallet_metadata` (
                `walletStableId` TEXT NOT NULL,
                `currentName` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `nameLastModified` INTEGER NOT NULL,
                PRIMARY KEY(`walletStableId`)
            )
            """.trimIndent()
        )
        // Extract wallets from user_profile and insert to WalletMetadata
        val cursor = db.query("SELECT wallets FROM user_profile LIMIT 1")
        if (cursor.moveToFirst()) {
            val walletsCsv = cursor.getString(0) ?: ""
            val wallets = walletsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val now = System.currentTimeMillis()
            for (wallet in wallets) {
                val stableId = java.util.UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO `wallet_metadata` (`walletStableId`, `currentName`, `createdAt`, `nameLastModified`) VALUES (?, ?, ?, ?)",
                    arrayOf(stableId, wallet, now, now)
                )
            }
        }
        cursor.close()

        // 2. Create DuoPairing table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `duo_pairings` (
                `pairingId` TEXT NOT NULL,
                `partnerDeviceId` TEXT NOT NULL,
                `partnerDisplayName` TEXT NOT NULL,
                `pairingSecret` TEXT NOT NULL,
                `sharedWalletStableId` TEXT NOT NULL,
                `pairedAt` INTEGER NOT NULL,
                `lastSyncedTimestamp` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                PRIMARY KEY(`pairingId`)
            )
            """.trimIndent()
        )

        // 3. Create DuoConflictLog table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `duo_conflict_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transactionUuid` TEXT NOT NULL,
                `walletStableId` TEXT NOT NULL,
                `conflictedAt` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `originalDataJson` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 4. Update transactions table
        db.execSQL("ALTER TABLE transactions ADD COLUMN transactionUuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN originDeviceId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1")
        
        // Generate UUID for existing transactions
        val txCursor = db.query("SELECT id FROM transactions")
        val nowForTx = System.currentTimeMillis()
        while (txCursor.moveToNext()) {
            val id = txCursor.getInt(0)
            val newUuid = java.util.UUID.randomUUID().toString()
            db.execSQL("UPDATE transactions SET transactionUuid = ?, lastModified = ? WHERE id = ?", arrayOf(newUuid, nowForTx, id))
        }
        txCursor.close()
        
        // Rebuild FTS
        db.execSQL("DROP TABLE IF EXISTS `transactions_fts`")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `transactions_fts` USING FTS4(`name` TEXT, `category` TEXT, `message` TEXT, content=`transactions`)")
        db.execSQL("INSERT INTO `transactions_fts` (`transactions_fts`, `rowid`, `name`, `category`, `message`) SELECT 'rebuild', `id`, `name`, `category`, `message` FROM `transactions`")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN savingsWallets TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN savingsGoals TEXT NOT NULL DEFAULT '{}'")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `virtual_wallets` (
                `name` TEXT NOT NULL,
                `orderIndex` INTEGER NOT NULL,
                `backgroundType` TEXT NOT NULL,
                `backgroundValue` TEXT NOT NULL,
                PRIMARY KEY(`name`)
            )
            """.trimIndent()
        )
        // Extract wallets from user_profile and insert into virtual_wallets
        val cursor = db.query("SELECT wallets FROM user_profile LIMIT 1")
        if (cursor.moveToFirst()) {
            val walletsCsv = cursor.getString(0) ?: ""
            val wallets = walletsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for ((index, wallet) in wallets.withIndex()) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `virtual_wallets` (`name`, `orderIndex`, `backgroundType`, `backgroundValue`) VALUES (?, ?, ?, ?)",
                    arrayOf(wallet, index, "SOLID", "#2A2A2A")
                )
            }
        }
        cursor.close()
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE virtual_wallets ADD COLUMN cardNumber TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE virtual_wallets ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE virtual_wallets ADD COLUMN cardLabel TEXT NOT NULL DEFAULT 'ACCESS CARD'")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN isNavMotionEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN isParallaxEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(
    entities = [
        KumaTransaction::class,
        UserProfile::class,
        TransactionSplit::class,
        TransactionFTS::class,
        com.bearbones.kumaflow.duo.model.WalletMetadata::class,
        com.bearbones.kumaflow.duo.model.DuoPairing::class,
        com.bearbones.kumaflow.duo.model.DuoConflictLog::class,
        VirtualWallet::class
    ],
    version = 32,
    exportSchema = false
)
abstract class KumaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: KumaDatabase? = null

        fun getDatabase(context: Context): KumaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KumaDatabase::class.java,
                    "kuma_database"
                )
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
