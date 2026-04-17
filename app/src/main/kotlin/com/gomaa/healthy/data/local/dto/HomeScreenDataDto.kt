package com.gomaa.healthy.data.local.dto

data class HomeScreenDataDto(
    val date: Long,
    val totalSteps: Int,
    val activeMinutes: Int,
    val totalDistanceMeters: Double,
    val avgBpm: Int?,
    val minBpm: Int?,
    val maxBpm: Int?,
    val heartRateCount: Int,
    val activeGoalsCount: Int,
    val goalTarget: Int?,
    val goalName: String,
    val goalType: String
)