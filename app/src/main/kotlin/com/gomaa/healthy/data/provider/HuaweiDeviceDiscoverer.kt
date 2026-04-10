package com.gomaa.healthy.data.provider

import android.content.Context
import android.util.Log
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.WearableDeviceDiscoverer
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.HiWear
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class HuaweiDeviceDiscoverer @Inject constructor(
    @ApplicationContext private val context: Context
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

    private suspend fun getConnectedDevicesList(): List<Device> =
        suspendCancellableCoroutine { continuation ->
            deviceClient.bondedDevices.addOnSuccessListener { deviceList ->
                Log.i("HuaweiDeviceDiscoverer", "Found ${deviceList?.size ?: 0} devices")
                continuation.resume(deviceList ?: emptyList())
            }.addOnFailureListener { e ->
                Log.e("HuaweiDeviceDiscoverer", "Failed to get devices", e)
                continuation.resume(emptyList())
            }
        }
}