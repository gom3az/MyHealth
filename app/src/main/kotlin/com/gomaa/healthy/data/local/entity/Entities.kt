package com.gomaa.healthy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
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
    val source: String = "myhealth" // "myhealth" or "health_connect"
)

@Entity(tableName = "fitness_goals")
data class FitnessGoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // "steps", "distance", "activity_minutes", "heart_rate_zone"
    val targetValue: Int,
    val period: String, // "DAILY", "WEEKLY"
    val createdAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val minHeartRate: Int,
    val deviceBrand: String,
    val source: String = "myhealth", // "myhealth" or "health_connect"
    val healthConnectRecordId: String? = null // Original HC record ID for deduplication
)

// HC-058: Add composite primary key on (timestamp, source) for deduplication
// Removed @PrimaryKey(autoGenerate - composite keys handle uniqueness
@Entity(tableName = "heart_rates", primaryKeys = ["timestamp", "source"])
data class HeartRateEntity(
    // Using composite key instead of auto-generated ID
    val timestamp: Long,
    val source: String,
    val sessionId: String? = null, // Nullable - can be null for Health Connect readings
    val bpm: Int,
    val healthConnectRecordId: String? = null // For deduplication with HC records
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Long = value

    @TypeConverter
    fun toTimestamp(value: Long): Long = value
}