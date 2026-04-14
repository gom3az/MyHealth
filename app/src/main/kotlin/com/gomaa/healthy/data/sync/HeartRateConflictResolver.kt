package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import javax.inject.Inject
import javax.inject.Singleton

fun interface HeartRateConflictResolver {
    fun shouldApply(
        hcRecordId: String,
        existingRecordIds: Set<String>,
        existingLocal: HeartRateEntity?
    ): Boolean
}

@Singleton
class HeartRateConflictResolverImpl @Inject constructor() : HeartRateConflictResolver {

    override fun shouldApply(
        hcRecordId: String,
        existingRecordIds: Set<String>,
        existingLocal: HeartRateEntity?
    ): Boolean {
        if (hcRecordId in existingRecordIds) return false
        if (existingLocal != null && existingLocal.source == SOURCE_MY_HEALTH) return false
        return true
    }
}
