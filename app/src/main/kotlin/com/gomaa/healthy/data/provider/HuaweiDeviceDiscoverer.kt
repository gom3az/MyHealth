package com.gomaa.healthy.data.provider

import android.content.Context
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.WearableDeviceDiscoverer
import com.gomaa.healthy.logging.AppLogger
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class HuaweiDeviceDiscoverer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) : WearableDeviceDiscoverer {

    override val brand: String = "Huawei"

    private val deviceClient: DeviceClient = HiWear.getDeviceClient(context)

    override suspend fun getConnectedDevices(): List<DeviceInfo> {
        return getConnectedDevicesList().map { device ->
            DeviceInfo(
                id = device.uuid ?: "",
                name = device.name ?: "Huawei Watch",
                brand = "Huawei",
                isConnected = device.isConnected
            )
        }
    }

    override suspend fun isDeviceConnected(deviceId: String): Boolean {
        return getConnectedDevicesList().any { it.uuid == deviceId && it.isConnected }
    }

    override suspend fun hasAvailableDevices(): Boolean = checkAvailableDevices()

    private suspend fun getConnectedDevicesList(): List<Device> =
        suspendCancellableCoroutine { continuation ->
            deviceClient.bondedDevices.addOnSuccessListener { deviceList ->
                appLogger.i("HuaweiDeviceDiscoverer", "Found ${deviceList?.size ?: 0} devices")
                continuation.resume(deviceList ?: emptyList())
            }.addOnFailureListener { e ->
                appLogger.e("HuaweiDeviceDiscoverer", "Failed to get devices", e)
                continuation.resume(emptyList())
            }
        }

    private suspend fun checkAvailableDevices(): Boolean =
        suspendCancellableCoroutine { continuation ->
            deviceClient.hasAvailableDevices()
                .addOnSuccessListener { result ->
                    appLogger.i("HuaweiDeviceDiscoverer", "hasAvailableDevices: $result")
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    appLogger.e("HuaweiDeviceDiscoverer", "hasAvailableDevices failed", e)
                    continuation.resume(false)
                }
        }
}