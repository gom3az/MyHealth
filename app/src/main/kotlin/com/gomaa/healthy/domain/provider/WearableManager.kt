package com.gomaa.healthy.domain.provider

import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.WearableProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class WearableManager @Inject constructor(
    private val providers: Map<String, WearableProvider>
) {
    private val _currentProvider = MutableStateFlow<WearableProvider?>(null)
    val currentProvider: Flow<WearableProvider?> = _currentProvider.asStateFlow()

    private val _heartRate = MutableStateFlow(0)
    val heartRate: Flow<Int> = _heartRate.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: Flow<DeviceInfo?> = _connectedDevice.asStateFlow()

    val availableProviders: List<String> = providers.keys.toList()

    fun getProvider(brand: String): WearableProvider? = providers[brand]

    fun setCurrentProvider(provider: WearableProvider) {
        _currentProvider.value = provider
    }

    fun updateHeartRate(bpm: Int) {
        _heartRate.value = bpm
    }

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun setConnectedDevice(device: DeviceInfo?) {
        _connectedDevice.value = device
    }
}