package com.gomaa.healthy.data.provider

import android.content.Context
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.WearableHeartRateMonitor
import com.gomaa.healthy.logging.AppLogger
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.monitor.MonitorClient
import com.huawei.wearengine.monitor.MonitorItem
import com.huawei.wearengine.monitor.MonitorListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class HuaweiHeartRateMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) : WearableHeartRateMonitor {

    private val authClient: AuthClient = HiWear.getAuthClient(context)
    private val deviceClient: DeviceClient = HiWear.getDeviceClient(context)
    private val monitorClient: MonitorClient = HiWear.getMonitorClient(context)

    private val _heartRate = MutableStateFlow(0)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private var registeredDevice: Device? = null
    private var registeredListener: MonitorListener? = null

    override fun heartRateFlow(): Flow<Int> = _heartRate.asStateFlow()
    override fun connectionStatus(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun startMonitoring(deviceId: String) {
        _connectionState.value = ConnectionState.Connecting

        val permissionsGranted = requestPermissions()
        if (!permissionsGranted) {
            _connectionState.value = ConnectionState.Error("Permission denied")
            return
        }

        val devices = getConnectedDevices().filter { it.isConnected }
        val device = devices.firstOrNull { it.uuid == deviceId } ?: devices.firstOrNull()

        if (device == null) {
            _connectionState.value = ConnectionState.Error("No device found")
            return
        }

        try {
            val huaweiDevice = findDeviceByUuid(device.uuid)
            if (huaweiDevice == null) {
                _connectionState.value = ConnectionState.Error("Device not found")
                return
            }

            registerHeartRateListener(huaweiDevice)
            _connectionState.value = ConnectionState.Connected
        } catch (e: Exception) {
            _connectionState.value =
                ConnectionState.Error(e.message ?: "Failed to start monitoring")
        }
    }

    override suspend fun stopMonitoring() {
        registeredListener?.let { listener ->
            try {
                monitorClient.unregister(listener)
                appLogger.i("HuaweiHeartRateMonitor", "Unregistered heart rate listener")
            } catch (e: Exception) {
                appLogger.e("HuaweiHeartRateMonitor", "Failed to unregister listener", e)
            }
        }

        registeredDevice = null
        registeredListener = null
        _heartRate.value = 0
        _connectionState.value = ConnectionState.Disconnected
    }

    private suspend fun requestPermissions(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val permissions = arrayOf(Permission.DEVICE_MANAGER, Permission.SENSOR)
            authClient.requestPermission(object : AuthCallback {
                override fun onOk(permissions: Array<out Permission>?) {
                    appLogger.i("HuaweiHeartRateMonitor", "Permissions granted")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancel() {
                    appLogger.i("HuaweiHeartRateMonitor", "Permissions denied")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, *permissions)
        }

    private suspend fun getConnectedDevices(): List<Device> =
        suspendCancellableCoroutine { continuation ->
            deviceClient.bondedDevices.addOnSuccessListener { deviceList ->
                continuation.resume(deviceList?.filter { it.isConnected } ?: emptyList())
            }.addOnFailureListener { e ->
                appLogger.e("HuaweiHeartRateMonitor", "Failed to get devices", e)
                continuation.resume(emptyList())
            }
        }

    private suspend fun findDeviceByUuid(uuid: String): Device? =
        getConnectedDevices().firstOrNull { it.uuid == uuid }

    private fun registerHeartRateListener(device: Device) {
        val heartRateItem = MonitorItem.MONITOR_ITEM_HEART_RATE_ALARM

        val listener = MonitorListener { errorCode, item, data ->
            if (errorCode == 0 && data != null) {
                val heartRateValue = data.asInt()
                _heartRate.value = heartRateValue
                appLogger.d("HuaweiHeartRateMonitor", "Heart rate: $heartRateValue")
            } else {
                appLogger.w("HuaweiHeartRateMonitor", "Heart rate error code: $errorCode")
            }
        }

        registeredDevice = device
        registeredListener = listener

        monitorClient.register(device, heartRateItem, listener)
        appLogger.i(
            "HuaweiHeartRateMonitor",
            "Registered heart rate listener for device: ${device.name}"
        )
    }
}