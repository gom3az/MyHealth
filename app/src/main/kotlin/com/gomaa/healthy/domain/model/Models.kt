package com.gomaa.healthy.domain.model

data class HeartRateRecord(
    val timestamp: Long,
    val bpm: Int
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