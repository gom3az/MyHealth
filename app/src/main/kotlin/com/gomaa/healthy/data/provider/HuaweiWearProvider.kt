package com.gomaa.healthy.data.provider

import android.content.Context
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.WearableProvider
import com.gomaa.healthy.logging.AppLogger
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.client.ServiceConnectionListener
import com.huawei.wearengine.client.WearEngineClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HuaweiWearProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDiscoverer: HuaweiDeviceDiscoverer,
    private val heartRateMonitor: HuaweiHeartRateMonitor,
    private val appLogger: AppLogger
) : WearableProvider {

    override val brand: String = "Huawei"
    override val isSupported: Boolean = true

    private val serviceConnectionListener = object : ServiceConnectionListener {
        override fun onServiceConnect() {
            appLogger.i("HuaweiWearProvider", "WearEngine Service Connected!")
        }

        override fun onServiceDisconnect() {
            appLogger.i("HuaweiWearProvider", "WearEngine Service Disconnected!")
        }
    }

    private val wearEngineClient: WearEngineClient =
        HiWear.getWearEngineClient(context, serviceConnectionListener)

    override fun heartRateFlow(): Flow<Int> = heartRateMonitor.heartRateFlow()

    override fun connectionStatus(): Flow<ConnectionState> = heartRateMonitor.connectionStatus()

    override suspend fun startMonitoring(deviceId: String) {
        wearEngineClient.registerServiceConnectionListener()
        heartRateMonitor.startMonitoring(deviceId)
    }

    override suspend fun stopMonitoring() {
        wearEngineClient.unregisterServiceConnectionListener()
        heartRateMonitor.stopMonitoring()
    }

    override suspend fun isDeviceConnected(deviceId: String): Boolean {
        return deviceDiscoverer.isDeviceConnected(deviceId)
    }

    override suspend fun getConnectedDevices(): List<DeviceInfo> {
        return deviceDiscoverer.getConnectedDevices()
    }

    override suspend fun hasAvailableDevices(): Boolean {
        return deviceDiscoverer.hasAvailableDevices()
    }
}