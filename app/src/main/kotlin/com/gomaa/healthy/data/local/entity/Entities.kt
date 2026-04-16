package com.gomaa.healthy.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "daily_steps", primaryKeys = ["date", "source"])
data class DailyStepsEntity(
    val date: Long, // epoch day
    val totalSteps: Int,
    val totalDistanceMeters: Double,
    val activeMinutes: Int,
    val lightActivityMinutes: Int,
    val moderateActivityMinutes: Int,
    val vigorousActivityMinutes: Int,
    val source: String = "myhealth", // "myhealth" or "health_connect"
    @ColumnInfo(name = "synced_to_hc") val syncedToHc: Int = 0, // 0 = not synced, 1 = synced to Health Connect
    val dataOrigin: String? = null // Package name from Health Connect (e.g., "com.huawei.health")
)

@Entity(tableName = "fitness_goals")
data class FitnessGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "steps", "distance", "activity_minutes", "heart_rate_zone"
    val targetValue: Int,
    val period: String, // "DAILY", "WEEKLY"
    val createdAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val minHeartRate: Int,
    val deviceBrand: String,
    val source: String = "myhealth", // "myhealth" or "health_connect"
    val healthConnectRecordId: String? = null, // Original HC record ID for deduplication
    @ColumnInfo(name = "synced_to_hc") val syncedToHc: Int = 0, // 0 = not synced, 1 = synced to Health Connect
    val exerciseType: Int = 0, // ExerciseSessionRecord exercise type constant
    val title: String = "", // Session title/name
    val dataOrigin: String? = null // Package name from Health Connect
)

@Entity(
    tableName = "heart_rate_buckets", indices = [Index(value = ["source", "dayTimestamp"])]
)
data class HeartRateBucketEntity(
    @PrimaryKey val bucketId: String,        // "2026-04-15-14" (YYYY-MM-DD-HH)
    val source: String,                       // "wearable_huawei_cloud"
    val dayTimestamp: Long,                   // Midnight of day (millis, for fast range queries)
    val minBpm: Int,
    val avgBpm: Int,
    val maxBpm: Int,
    val count: Int,
    val samplesJson: String,                   // [{"t":1713192000,"v":72},...]
    @ColumnInfo(name = "synced_to_hc") val syncedToHc: Int = 0, // 0 = not synced, 1 = synced to Health Connect
    val healthConnectRecordId: String,
    val sessionId: String? = null, // Nullable - can be null for Health Connect readings
)

data class AggregatedHeartRateBucket(
    val bucketId: String,
    val source: String,
    val dayTimestamp: Long,
    val minBpm: Int,
    val avgBpm: Int,
    val maxBpm: Int,
    val count: Int
)