package com.gomaa.healthy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey
    val date: Long, // epoch day
    val totalSteps: Int,
    val totalDistanceMeters: Double,
    val activeMinutes: Int,
    val lightActivityMinutes: Int,
    val moderateActivityMinutes: Int,
    val vigorousActivityMinutes: Int
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

@Entity(tableName = "heart_rates")
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val bpm: Int
)

@Entity(tableName = "health_connect_steps")
data class HealthConnectStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val count: Int,
    val startTime: Long,
    val endTime: Long,
    val healthConnectRecordId: String, // Original HC record ID
    val importedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "health_connect_exercise_sessions")
data class HealthConnectExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val exerciseType: String,
    val durationMinutes: Int,
    val caloriesBurned: Int?,
    val healthConnectRecordId: String,
    val importedAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Long = value

    @TypeConverter
    fun toTimestamp(value: Long): Long = value
}