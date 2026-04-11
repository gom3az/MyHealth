package com.gomaa.healthy.data.mapper

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.DistanceUnit
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalPeriod
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.model.HeartRateRecord
import java.time.LocalDate

// DailySteps Mappers
fun DailyStepsEntity.toDomain(): DailySteps {
    return DailySteps(
        date = LocalDate.ofEpochDay(date),
        totalSteps = totalSteps,
        totalDistanceMeters = totalDistanceMeters,
        activeMinutes = activeMinutes,
        lightActivityMinutes = lightActivityMinutes,
        moderateActivityMinutes = moderateActivityMinutes,
        vigorousActivityMinutes = vigorousActivityMinutes
    )
}

fun DailySteps.toEntity(): DailyStepsEntity {
    return DailyStepsEntity(
        date = date.toEpochDay(),
        totalSteps = totalSteps,
        totalDistanceMeters = totalDistanceMeters,
        activeMinutes = activeMinutes,
        lightActivityMinutes = lightActivityMinutes,
        moderateActivityMinutes = moderateActivityMinutes,
        vigorousActivityMinutes = vigorousActivityMinutes
    )
}

// FitnessGoal Mappers
fun FitnessGoalEntity.toDomain(): FitnessGoal {
    val goalType = when (type) {
        "steps" -> GoalType.Steps(targetValue)
        "distance" -> GoalType.Distance(targetValue.toDouble(), DistanceUnit.KILOMETERS)
        "activity_minutes" -> GoalType.ActivityMinutes(targetValue)
        else -> GoalType.Steps(targetValue)
    }
    val goalPeriod = when (period) {
        "WEEKLY" -> GoalPeriod.WEEKLY
        else -> GoalPeriod.DAILY
    }
    return FitnessGoal(
        id = id,
        name = name,
        type = goalType,
        period = goalPeriod,
        createdAt = createdAt,
        isActive = isActive
    )
}

fun FitnessGoal.toEntity(): FitnessGoalEntity {
    val typeStr = when (val t = type) {
        is GoalType.Steps -> "steps"
        is GoalType.Distance -> "distance"
        is GoalType.ActivityMinutes -> "activity_minutes"
        is GoalType.HeartRateZone -> "heart_rate_zone"
    }
    val targetVal = when (val t = type) {
        is GoalType.Steps -> t.target
        is GoalType.Distance -> t.targetMeters.toInt()
        is GoalType.ActivityMinutes -> t.targetMinutes
        is GoalType.HeartRateZone -> t.targetMinutes
    }
    return FitnessGoalEntity(
        id = id,
        name = name,
        type = typeStr,
        targetValue = targetVal,
        period = period.name,
        createdAt = createdAt,
        isActive = isActive
    )
}

fun ExerciseSessionEntity.toDomain(heartRates: List<HeartRateRecord> = emptyList()): ExerciseSession {
    return ExerciseSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        minHeartRate = minHeartRate,
        deviceBrand = deviceBrand,
        heartRates = heartRates
    )
}

fun ExerciseSession.toEntity(): ExerciseSessionEntity {
    return ExerciseSessionEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        minHeartRate = minHeartRate,
        deviceBrand = deviceBrand
    )
}

fun HeartRateEntity.toDomain(): HeartRateRecord {
    return HeartRateRecord(
        timestamp = timestamp,
        bpm = bpm
    )
}

fun HeartRateRecord.toEntity(sessionId: String): HeartRateEntity {
    return HeartRateEntity(
        sessionId = sessionId,
        timestamp = timestamp,
        bpm = bpm
    )
}