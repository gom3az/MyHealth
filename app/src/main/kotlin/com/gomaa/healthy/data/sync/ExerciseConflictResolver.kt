package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import javax.inject.Inject
import javax.inject.Singleton

fun interface ExerciseConflictResolver {
    fun shouldApply(
        hcRecordId: String,
        existingByHcId: ExerciseSessionEntity?,
        existingLocal: ExerciseSessionEntity?
    ): Boolean
}

@Singleton
class ExerciseConflictResolverImpl @Inject constructor() : ExerciseConflictResolver {

    override fun shouldApply(
        hcRecordId: String,
        existingByHcId: ExerciseSessionEntity?,
        existingLocal: ExerciseSessionEntity?
    ): Boolean {
        if (existingByHcId != null) return false
        if (existingLocal != null && existingLocal.source == SOURCE_MY_HEALTH) return false
        return true
    }
}
