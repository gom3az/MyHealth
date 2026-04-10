package com.gomaa.healthy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val minHeartRate: Int,
    val deviceBrand: String
)

@Entity(tableName = "heart_rates")
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val bpm: Int
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Long = value

    @TypeConverter
    fun toTimestamp(value: Long): Long = value
}