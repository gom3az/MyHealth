package com.gomaa.healthy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HeartRateBucketDao
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateBucketEntity
import com.gomaa.healthy.data.security.EncryptedPreferencesManager
import java.io.File
import java.io.RandomAccessFile

@Database(
    entities = [
        ExerciseSessionEntity::class,
        HeartRateBucketEntity::class,
        DailyStepsEntity::class,
        FitnessGoalEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun heartRateBucketDao(): HeartRateBucketDao
    abstract fun briefDao(): BriefDao
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun goalDao(): GoalDao

    companion object {
        private const val DATABASE_NAME = "health_database"
        private const val DB_PASSPHRASE_KEY = "database_passphrase"

        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(
            context: Context, encryptedPrefsManager: EncryptedPreferencesManager
        ): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                if (isMigrationNeeded(context, encryptedPrefsManager)) {
                    migrateUnencryptedToEncrypted(context, encryptedPrefsManager)
                }

                val passphrase = getOrCreatePassphrase(encryptedPrefsManager)

                val instance = Room.databaseBuilder(
                    context.applicationContext, HealthDatabase::class.java, DATABASE_NAME
                ).openHelperFactory(
                    net.sqlcipher.database.SupportFactory(passphrase)
                ).addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                ).build()
                INSTANCE = instance
                instance
            }
        }

        private fun isMigrationNeeded(
            context: Context, encryptedPrefsManager: EncryptedPreferencesManager
        ): Boolean {
            var migrationDone = false
            kotlinx.coroutines.runBlocking {
                migrationDone = encryptedPrefsManager.getEncryptedBoolean(
                    EncryptedPreferencesManager.KEY_DB_SQLCIPHER_MIGRATION_DONE, false
                )
            }

            if (migrationDone) return false

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return dbFile.exists() && isUnencryptedDatabase(dbFile)
        }

        private fun isUnencryptedDatabase(dbFile: File): Boolean {
            return try {
                RandomAccessFile(dbFile, "r").use { raf ->
                    val header = ByteArray(16)
                    raf.read(header)
                    val signature = String(header, 0, 15)
                    signature.startsWith("SQLite format 3")
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun migrateUnencryptedToEncrypted(
            context: Context, encryptedPrefsManager: EncryptedPreferencesManager
        ) {
            val passphrase = getOrCreatePassphrase(encryptedPrefsManager)

            val instance = Room.databaseBuilder(
                context.applicationContext, HealthDatabase::class.java, DATABASE_NAME
            ).addMigrations(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10
            ).build()

            instance.openHelper.writableDatabase.use { db ->
                db.execSQL("PRAGMA rekey = '${String(passphrase, Charsets.UTF_8)}'")
            }

            kotlinx.coroutines.runBlocking {
                encryptedPrefsManager.saveEncryptedBoolean(
                    EncryptedPreferencesManager.KEY_DB_SQLCIPHER_MIGRATION_DONE, true
                )
            }
        }

        private fun getOrCreatePassphrase(
            encryptedPrefsManager: EncryptedPreferencesManager
        ): ByteArray {
            var stored: String? = null
            kotlinx.coroutines.runBlocking {
                stored = encryptedPrefsManager.getEncryptedString(DB_PASSPHRASE_KEY)
            }
            return if (stored != null) {
                android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
            } else {
                val passphrase = generateSecurePassphrase()
                val encoded =
                    android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)
                kotlinx.coroutines.runBlocking {
                    encryptedPrefsManager.saveEncryptedString(DB_PASSPHRASE_KEY, encoded)
                }
                passphrase
            }
        }

        private fun generateSecurePassphrase(): ByteArray {
            val random = java.security.SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return bytes
        }

        // Migration from v3 to v4: Add source column to daily_steps with default "myhealth"
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add source column to daily_steps with default value "myhealth"
                database.execSQL(
                    "ALTER TABLE daily_steps ADD COLUMN source TEXT NOT NULL DEFAULT 'myhealth'"
                )
            }
        }

        // Migration from v4 to v5: Add source and healthConnectRecordId to heart_rates
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add source column with default value "myhealth"
                database.execSQL(
                    "ALTER TABLE heart_rates ADD COLUMN source TEXT NOT NULL DEFAULT 'myhealth'"
                )
                // Add healthConnectRecordId column for deduplication
                database.execSQL(
                    "ALTER TABLE heart_rates ADD COLUMN healthConnectRecordId TEXT"
                )
            }
        }

        // Migration v5 to v6: Rebuild heart_rates with composite primary key (timestamp, source)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with composite key
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS heart_rates_new (
                        timestamp INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        sessionId TEXT,
                        bpm INTEGER NOT NULL,
                        healthConnectRecordId TEXT,
                        PRIMARY KEY (timestamp, source)
                    )
                """.trimIndent()
                )
                // Migrate data
                database.execSQL(
                    """
                    INSERT INTO heart_rates_new (timestamp, source, sessionId, bpm, healthConnectRecordId)
                    SELECT timestamp, 'myhealth', 
                           CASE WHEN sessionId = '' THEN NULL ELSE sessionId END,
                           bpm, NULL
                    FROM heart_rates
                """.trimIndent()
                )
                // Drop old table and rename new one
                database.execSQL("DROP TABLE heart_rates")
                database.execSQL("ALTER TABLE heart_rates_new RENAME TO heart_rates")
            }
        }

        // Migration v6 to v7: Rebuild daily_steps with composite primary key (date, source)
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with composite key
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_steps_new (
                        date INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        totalSteps INTEGER NOT NULL,
                        totalDistanceMeters REAL NOT NULL,
                        activeMinutes INTEGER NOT NULL,
                        lightActivityMinutes INTEGER NOT NULL,
                        moderateActivityMinutes INTEGER NOT NULL,
                        vigorousActivityMinutes INTEGER NOT NULL,
                        PRIMARY KEY (date, source)
                    )
                """.trimIndent()
                )
                // Migrate data with default source
                database.execSQL(
                    """
                    INSERT INTO daily_steps_new (date, source, totalSteps, totalDistanceMeters, activeMinutes, lightActivityMinutes, moderateActivityMinutes, vigorousActivityMinutes)
                    SELECT date, 'myhealth', totalSteps, totalDistanceMeters, activeMinutes, lightActivityMinutes, moderateActivityMinutes, vigorousActivityMinutes
                    FROM daily_steps
                """.trimIndent()
                )
                // Drop old table and rename new one
                database.execSQL("DROP TABLE daily_steps")
                database.execSQL("ALTER TABLE daily_steps_new RENAME TO daily_steps")
            }
        }

        // Migration v7 to v8: Add dataOrigin column to all tables
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_steps ADD COLUMN dataOrigin TEXT")
                database.execSQL("ALTER TABLE heart_rates ADD COLUMN dataOrigin TEXT")
                database.execSQL("ALTER TABLE exercise_sessions ADD COLUMN dataOrigin TEXT")
            }
        }

        // Migration v8 to v9: Create heart_rate_buckets table
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS heart_rate_buckets (
                        bucketId TEXT NOT NULL PRIMARY KEY,
                        source TEXT NOT NULL,
                        dayTimestamp INTEGER NOT NULL,
                        minBpm INTEGER NOT NULL,
                        avgBpm INTEGER NOT NULL,
                        maxBpm INTEGER NOT NULL,
                        count INTEGER NOT NULL,
                        samplesJson TEXT NOT NULL
                    )
                """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_heart_rate_buckets_source_dayTimestamp ON heart_rate_buckets(source, dayTimestamp)"
                )
            }
        }

        // Migration v9 to v10: Add synced_to_hc, healthConnectRecordId, and sessionId to heart_rate_buckets
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE heart_rate_buckets ADD COLUMN synced_to_hc INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE heart_rate_buckets ADD COLUMN healthConnectRecordId TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE heart_rate_buckets ADD COLUMN sessionId TEXT"
                )
            }
        }
    }
}