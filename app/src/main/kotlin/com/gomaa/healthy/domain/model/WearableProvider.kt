package com.gomaa.healthy.domain.model

import kotlinx.coroutines.flow.Flow

interface WearableProvider {
    val brand: String
    val isSupported: Boolean
    fun heartRateFlow(): Flow<Int>
    fun connectionStatus(): Flow<ConnectionState>
    suspend fun startMonitoring(deviceId: String)
    suspend fun stopMonitoring()
    suspend fun isDeviceConnected(deviceId: String): Boolean
    suspend fun getConnectedDevices(): List<DeviceInfo>
}

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}