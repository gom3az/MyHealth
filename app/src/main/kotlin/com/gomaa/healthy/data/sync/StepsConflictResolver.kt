package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import javax.inject.Inject
import javax.inject.Singleton

fun interface StepsConflictResolver {
    fun shouldApply(localSteps: DailyStepsEntity?): Boolean
}

@Singleton
class StepsConflictResolverImpl @Inject constructor() : StepsConflictResolver {

    override fun shouldApply(localSteps: DailyStepsEntity?): Boolean {
        return localSteps?.source != SOURCE_MY_HEALTH
    }
}
