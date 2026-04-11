package com.gomaa.healthy.domain.model

interface WearableDeviceDiscoverer {
    val brand: String
    suspend fun getConnectedDevices(): List<DeviceInfo>
    suspend fun isDeviceConnected(deviceId: String): Boolean
    suspend fun hasAvailableDevices(): Boolean
}