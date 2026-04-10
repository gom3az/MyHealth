package com.gomaa.healthy.domain.model

import kotlinx.coroutines.flow.Flow

interface WearableDeviceDiscoverer {
    val brand: String
    suspend fun getConnectedDevices(): List<DeviceInfo>
    suspend fun isDeviceConnected(deviceId: String): Boolean
}