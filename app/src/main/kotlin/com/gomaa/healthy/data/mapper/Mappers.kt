package com.gomaa.healthy.data.mapper

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateBucketEntity
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.DistanceUnit
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalPeriod
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.StepSource
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
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
        source = when (source) {
            "health_connect" -> StepSource.HEALTH_CONNECT
            SOURCE_WEARABLE_HUAWEI_CLOUD -> StepSource.WEARABLE  // Cloud-based wearable data (Health Kit)
            else -> StepSource.MY_HEALTH
        }
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
    val typeStr = when (type) {
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
        heartRates = heartRates,
        exerciseType = exerciseType,
        title = title
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
        deviceBrand = deviceBrand,
        exerciseType = exerciseType,
        title = title
    )
}

fun HeartRateBucketEntity.toDomain(): DomainHeartRateRecord {
    return DomainHeartRateRecord(
        timestamp = dayTimestamp, bpm = avgBpm
    )
}

// HC-058: Expand bucket samplesJson to individual domain readings
fun HeartRateBucketEntity.toDomainReadings(): List<HeartRateReading> {
    return try {
        val jsonArray = JSONArray(samplesJson)
        (0 until jsonArray.length()).map { index ->
            val sample = jsonArray.getJSONObject(index)
            val timestampSeconds = sample.getLong("t")
            val bpm = sample.getInt("v")
            HeartRateReading(
                id = "${bucketId}_$index",
                bpm = bpm,
                timestamp = timestampSeconds * 1000, // Convert seconds back to millis
                source = if (source == "health_connect") HeartRateSource.HEALTH_CONNECT
                else if (source.startsWith("wearable_huawei")) HeartRateSource.WEARABLE_HUAWEI_CLOUD
                else HeartRateSource.MY_HEALTH
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// HC-062: Convert single domain record to bucket entity with single-sample aggregation
fun DomainHeartRateRecord.toEntity(
    sessionId: String, source: String = "myhealth"
): HeartRateBucketEntity {
    val bucketId = generateBucketId(timestamp)
    val dayTimestamp = generateDayTimestamp(timestamp)
    val samplesJson = createSamplesJson(timestamp, bpm)

    return HeartRateBucketEntity(
        bucketId = bucketId,
        source = source,
        dayTimestamp = dayTimestamp,
        minBpm = bpm,
        avgBpm = bpm,
        maxBpm = bpm,
        count = 1,
        samplesJson = samplesJson,
        syncedToHc = 0,
        healthConnectRecordId = "",
        sessionId = sessionId
    )
}

private fun generateBucketId(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
    return instant.atZone(java.time.ZoneId.systemDefault()).format(formatter)
}

private fun generateDayTimestamp(timestamp: Long): Long {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun createSamplesJson(timestamp: Long, bpm: Int): String {
    val sample = org.json.JSONObject().put("t", timestamp / 1000).put("v", bpm)
    return org.json.JSONArray(listOf(sample)).toString()
}

// Health Connect source constant (for reference)
const val SOURCE_HEALTH_CONNECT = "health_connect"

// App's own source
const val SOURCE_MY_HEALTH = "myhealth"

// Wearable sources
const val SOURCE_WEARABLE_HUAWEI = "wearable_huawei"
const val SOURCE_WEARABLE_HUAWEI_CLOUD = "wearable_huawei_cloud"  // Cloud-based (Health Kit)
const val SOURCE_WEARABLE_OTHER = "wearable_other"

// Phone sensor source
const val SOURCE_PHONE_SENSOR = "phone_sensor"

// Manual entry source
const val SOURCE_MANUAL = "manual"

fun mapHeartRateRecordToEntity(
    record: HealthConnectHeartRateRecord, recordId: String
): List<HeartRateBucketEntity> {
    val dataOrigin = record.metadata.dataOrigin.packageName
    return record.samples.groupBy { sample ->
        val instant = sample.time
        val zoned = instant.atZone(ZoneId.systemDefault())
        String.format(
            "%04d-%02d-%02d-%02d", zoned.year, zoned.monthValue, zoned.dayOfMonth, zoned.hour
        )
    }.map { (bucketId, samplesForHour) ->
        val firstSampleTime = samplesForHour.first().time
        val dayTimestamp = firstSampleTime.atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val minBpm = samplesForHour.minOf { it.beatsPerMinute.toInt() }
        val avgBpm = samplesForHour.map { it.beatsPerMinute.toInt() }.average().toInt()
        val maxBpm = samplesForHour.maxOf { it.beatsPerMinute.toInt() }
        val count = samplesForHour.size
        val samplesJsonArray = JSONArray()
        samplesForHour.forEach { sample ->
            val obj = JSONObject()
            obj.put("t", sample.time.toEpochMilli() / 1000) // seconds
            obj.put("v", sample.beatsPerMinute)
            samplesJsonArray.put(obj)
        }
        val samplesJson = samplesJsonArray.toString()

        HeartRateBucketEntity(
            bucketId = bucketId,
            source = dataOrigin,
            dayTimestamp = dayTimestamp,
            minBpm = minBpm,
            avgBpm = avgBpm,
            maxBpm = maxBpm,
            count = count,
            samplesJson = samplesJson,
            syncedToHc = 0,
            healthConnectRecordId = recordId,
            sessionId = null
        )
    }
}

fun mapExerciseSessionRecordToEntity(
    record: ExerciseSessionRecord, healthConnectRecordId: String
): ExerciseSessionEntity {
    val dataOrigin = record.metadata.dataOrigin.packageName
    return ExerciseSessionEntity(
        id = UUID.randomUUID().toString(),
        startTime = record.startTime.toEpochMilli(),
        endTime = record.endTime.toEpochMilli(),
        avgHeartRate = 0,
        maxHeartRate = 0,
        minHeartRate = 0,
        deviceBrand = "Health Connect",
        source = SOURCE_HEALTH_CONNECT,
        healthConnectRecordId = healthConnectRecordId,
        exerciseType = record.exerciseType,
        title = record.title.orEmpty(),
        dataOrigin = dataOrigin
    )
}

fun mapStepsRecordToEntity(record: StepsRecord, date: Long): DailyStepsEntity {
    val dataOrigin = record.metadata.dataOrigin.packageName
    return DailyStepsEntity(
        date = date,
        totalSteps = record.count.toInt(),
        totalDistanceMeters = 0.0,
        activeMinutes = 0,
        lightActivityMinutes = 0,
        moderateActivityMinutes = 0,
        vigorousActivityMinutes = 0,
        source = SOURCE_HEALTH_CONNECT,
        dataOrigin = dataOrigin
    )
}
