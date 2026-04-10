package com.gomaa.healthy.domain.model

import kotlinx.coroutines.flow.Flow

interface WearableHeartRateMonitor {
    fun heartRateFlow(): Flow<Int>
    fun connectionStatus(): Flow<ConnectionState>
    suspend fun startMonitoring(deviceId: String)
    suspend fun stopMonitoring()
}