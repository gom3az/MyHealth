package com.gomaa.healthy.data.provider

import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.WearableProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class MockWearableProvider @Inject constructor() : WearableProvider {

    override val brand: String = "Mock"
    override val isSupported: Boolean = true

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _heartRate = MutableStateFlow(0)
    private var isMonitoring = false

    override fun heartRateFlow(): Flow<Int> = _heartRate.asStateFlow()

    override fun connectionStatus(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun startMonitoring(deviceId: String) {
        _connectionState.value = ConnectionState.Connecting
        delay(500)
        _connectionState.value = ConnectionState.Connected
        isMonitoring = true

        while (isMonitoring) {
            val baseHR = Random.nextInt(60, 100)
            _heartRate.value = baseHR
            delay(1000)
        }
    }

    override suspend fun stopMonitoring() {
        isMonitoring = false
        _connectionState.value = ConnectionState.Disconnected
        _heartRate.value = 0
    }

    override suspend fun isDeviceConnected(deviceId: String): Boolean {
        return isMonitoring
    }

    override suspend fun getConnectedDevices(): List<DeviceInfo> {
        return if (isMonitoring) {
            listOf(DeviceInfo("mock-1", "Mock Watch", "Mock", true))
        } else {
            emptyList()
        }
    }
}