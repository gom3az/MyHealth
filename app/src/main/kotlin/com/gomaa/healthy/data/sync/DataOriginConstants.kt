package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.mapper.SOURCE_MANUAL
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import com.gomaa.healthy.data.mapper.SOURCE_PHONE_SENSOR
import com.gomaa.healthy.data.mapper.SOURCE_WEARABLE_HUAWEI
import com.gomaa.healthy.data.mapper.SOURCE_WEARABLE_OTHER

object DataOriginConstants {
    const val PACKAGE_ANDROID = "android"
    const val PACKAGE_GOOGLE_FIT = "com.google.android.gms"
    const val PACKAGE_HUAWEI_HEALTH = "com.huawei.health"
    const val PACKAGE_SAMSUNG_HEALTH = "com.samsung.android.shealth"

    val KNOWN_DATA_ORIGINS = setOf(
        PACKAGE_ANDROID,
        PACKAGE_GOOGLE_FIT,
        PACKAGE_HUAWEI_HEALTH,
        PACKAGE_SAMSUNG_HEALTH
    )

    enum class DataPrecision(val priority: Int) {
        LOW(0),
        STANDARD(1),
        HIGH(2),
        LOCAL(3)
    }

    fun getPrecisionForPackage(packageName: String?): DataPrecision {
        return when (packageName) {
            PACKAGE_ANDROID, PACKAGE_GOOGLE_FIT -> DataPrecision.STANDARD
            PACKAGE_HUAWEI_HEALTH, PACKAGE_SAMSUNG_HEALTH -> DataPrecision.HIGH
            else -> DataPrecision.LOW
        }
    }

    fun getPrecision(dataOrigin: String?, resolvedSource: String? = null): DataPrecision {
        return when (resolvedSource) {
            SOURCE_WEARABLE_HUAWEI, SOURCE_WEARABLE_OTHER -> DataPrecision.HIGH
            SOURCE_PHONE_SENSOR -> DataPrecision.STANDARD
            SOURCE_MY_HEALTH, SOURCE_MANUAL -> DataPrecision.LOCAL
            else -> getPrecisionForPackage(dataOrigin)
        }
    }
}
