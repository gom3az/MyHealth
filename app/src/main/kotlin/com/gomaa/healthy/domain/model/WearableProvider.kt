package com.gomaa.healthy.domain.model

interface WearableProvider : WearableDeviceDiscoverer, WearableHeartRateMonitor {
    override val brand: String
    val isSupported: Boolean
}

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}