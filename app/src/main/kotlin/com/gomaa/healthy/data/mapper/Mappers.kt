package com.gomaa.healthy.data.mapper

import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord

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