package com.gomaa.healthy.domain.model

class SourceFilterOption(val id: String, val displayName: String)

enum class ReadingSource(
    val dbString: String,
    val displayName: String
) {
    HEALTH_CONNECT("health_connect", "Health Connect"),
    WEARABLE_HUAWEI_CLOUD("wearable_huawei_cloud", "Huawei"),
    WEARABLE_HUAWEI("wearable_huawei", "Huawei Device");

    companion object {
        fun fromDbString(dbString: String): ReadingSource? {
            return entries.find { it.dbString == dbString }
        }
    }
}
