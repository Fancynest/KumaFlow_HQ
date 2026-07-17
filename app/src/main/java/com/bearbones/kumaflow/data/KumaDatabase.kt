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
    val isEdited: Boolean = false
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
    val wallets: String = "Cash,Bank BCA,GoPay",
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
    val bankName: String = "",
    val bankAccount: String = ""
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithSplits(): Flow<List<TransactionWithSplits>>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE wallet = :walletName AND isIncome = 0 AND CAST(amount AS INTEGER) <= :maxBudget 
        GROUP BY name 
        ORDER BY timestamp DESC LIMIT 8
    """)
    fun getRecentExpensesForRoulette(walletName: String, maxBudget: Long): Flow<List<KumaTransaction>>

    @Transaction
    @Query("""
        SELECT t.* FROM transactions t 
        JOIN transactions_fts fts ON (t.id = fts.rowid) 
        WHERE transactions_fts MATCH :query
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

@Database(
    entities = [
        KumaTransaction::class,
        UserProfile::class,
        TransactionSplit::class,
        TransactionFTS::class
    ],
    version = 23,
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
                    .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
