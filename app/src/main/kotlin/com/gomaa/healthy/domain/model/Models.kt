package com.gomaa.healthy.domain.model

import java.time.LocalDate

data class DailySteps(
    val date: LocalDate,
    val totalSteps: Int,
    val totalDistanceMeters: Double,
    val activeMinutes: Int,
    val lightActivityMinutes: Int,
    val moderateActivityMinutes: Int,
    val vigorousActivityMinutes: Int,
    val source: StepSource = StepSource.MY_HEALTH
)

sealed class GoalType {
    data class Steps(val target: Int) : GoalType()
    data class Distance(val targetMeters: Double, val unit: DistanceUnit) : GoalType()
    data class ActivityMinutes(val targetMinutes: Int) : GoalType()
    data class HeartRateZone(
        val zone: com.gomaa.healthy.domain.model.HeartRateZone,
        val targetMinutes: Int
    ) : GoalType()
}

enum class GoalPeriod { DAILY, WEEKLY }
enum class DistanceUnit { KILOMETERS, MILES }

data class FitnessGoal(
    val id: String,
    val name: String,
    val type: GoalType,
    val period: GoalPeriod,
    val createdAt: Long,
    val isActive: Boolean
)

data class HeartRateRecord(
    val timestamp: Long,
    val bpm: Int
)

enum class HeartRateSource {
    MY_HEALTH,
    HEALTH_CONNECT
}

data class HeartRateReading(
    val id: Long,
    val bpm: Int,
    val timestamp: Long,
    val source: HeartRateSource
)

data class HeartRateSummary(
    val averageBpm: Int,
    val maxBpm: Int,
    val minBpm: Int,
    val readingCount: Int,
    val source: HeartRateSource? = null
)

data class ExerciseSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val minHeartRate: Int,
    val deviceBrand: String,
    val heartRates: List<HeartRateRecord> = emptyList()
)

data class DeviceInfo(
    val id: String,
    val name: String,
    val brand: String,
    val isConnected: Boolean
)

enum class HeartRateZone {
    REST, LOW, MODERATE, HIGH, VERY_HIGH
}

enum class StepSource {
    MY_HEALTH,
    HEALTH_CONNECT
}

data class StepsWithSource(
    val source: StepSource,
    val steps: Int
)

data class CombinedSteps(
    val totalSteps: Int,
    val myHealthSteps: Int,
    val healthConnectSteps: Int
)