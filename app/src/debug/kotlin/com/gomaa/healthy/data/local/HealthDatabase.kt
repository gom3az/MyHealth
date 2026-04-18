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

        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(
            context: Context, encryptedPrefsManager: EncryptedPreferencesManager
        ): HealthDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext, HealthDatabase::class.java, DATABASE_NAME
                ).addMigrations(
                    MIGRATION_9_10
                ).build()
                INSTANCE = instance
                instance
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