package com.gomaa.healthy.data.mapper

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
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
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.StepSource
import java.time.LocalDate
import java.util.UUID
import androidx.health.connect.client.records.HeartRateRecord as HealthConnectHeartRateRecord
import com.gomaa.healthy.domain.model.HeartRateRecord as DomainHeartRateRecord

// DailySteps Mappers
fun DailyStepsEntity.toDomain(): DailySteps {
    return DailySteps(
        date = LocalDate.ofEpochDay(date),
        totalSteps = totalSteps,
        totalDistanceMeters = totalDistanceMeters,
        activeMinutes = activeMinutes,
        lightActivityMinutes = lightActivityMinutes,
        moderateActivityMinutes = moderateActivityMinutes,
        vigorousActivityMinutes = vigorousActivityMinutes,
        source = if (source == "health_connect") StepSource.HEALTH_CONNECT else StepSource.MY_HEALTH
    )
}

fun DailySteps.toEntity(source: String = "myhealth"): DailyStepsEntity {
    return DailyStepsEntity(
        date = date.toEpochDay(),
        totalSteps = totalSteps,
        totalDistanceMeters = totalDistanceMeters,
        activeMinutes = activeMinutes,
        lightActivityMinutes = lightActivityMinutes,
        moderateActivityMinutes = moderateActivityMinutes,
        vigorousActivityMinutes = vigorousActivityMinutes,
        source = source
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

fun ExerciseSessionEntity.toDomain(heartRates: List<DomainHeartRateRecord> = emptyList()): ExerciseSession {
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

fun HeartRateEntity.toDomain(): DomainHeartRateRecord {
    return DomainHeartRateRecord(
        timestamp = timestamp,
        bpm = bpm
    )
}

// HC-058: Use timestamp as the ID since composite key is (timestamp, source)
fun HeartRateEntity.toDomainReading(): HeartRateReading {
    return HeartRateReading(
        id = timestamp, // Use timestamp as Long ID
        bpm = bpm,
        timestamp = timestamp,
        source = if (source == "health_connect") HeartRateSource.HEALTH_CONNECT else HeartRateSource.MY_HEALTH
    )
}

// HC-062: Added source parameter
fun DomainHeartRateRecord.toEntity(
    sessionId: String,
    source: String = "myhealth"
): HeartRateEntity {
    return HeartRateEntity(
        timestamp = timestamp,
        source = source,
        sessionId = sessionId,
        bpm = bpm
    )
}

const val SOURCE_HEALTH_CONNECT = "health_connect"

fun mapHeartRateRecordToEntity(
    record: HealthConnectHeartRateRecord, recordId: String, source: String
): List<HeartRateEntity> {
    return record.samples.map { sample ->
        HeartRateEntity(
            sessionId = null,
            timestamp = sample.time.toEpochMilli(),
            bpm = sample.beatsPerMinute.toInt(),
            source = source,
            healthConnectRecordId = recordId
        )
    }
}

fun mapExerciseSessionRecordToEntity(
    record: ExerciseSessionRecord,
    healthConnectRecordId: String
): ExerciseSessionEntity {
    return ExerciseSessionEntity(
        id = UUID.randomUUID().toString(),
        startTime = record.startTime.toEpochMilli(),
        endTime = record.endTime.toEpochMilli(),
        avgHeartRate = 0,
        maxHeartRate = 0,
        minHeartRate = 0,
        deviceBrand = "Health Connect",
        source = SOURCE_HEALTH_CONNECT,
        healthConnectRecordId = healthConnectRecordId
    )
}

fun mapStepsRecordToEntity(record: StepsRecord, date: Long): DailyStepsEntity {
    return DailyStepsEntity(
        date = date,
        totalSteps = record.count.toInt(),
        totalDistanceMeters = 0.0,
        activeMinutes = 0,
        lightActivityMinutes = 0,
        moderateActivityMinutes = 0,
        vigorousActivityMinutes = 0,
        source = SOURCE_HEALTH_CONNECT
    )
}
