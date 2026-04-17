package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.data.local.dto.HomeScreenDataDto
import com.gomaa.healthy.domain.model.ActiveGoalSummary
import com.gomaa.healthy.domain.model.DistanceUnit
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.model.HeartRateZone
import com.gomaa.healthy.domain.model.HomeScreenData
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val briefDao: BriefDao
) {
    suspend operator fun invoke(date: LocalDate): HomeScreenData? {
        val epoch = date.toEpochDay() // 20560
        val epochMillis =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() //1776376800000
        return briefDao.getHomeScreenData(epoch, epochMillis)?.toDomain()
    }
}

private fun HomeScreenDataDto.toDomain(): HomeScreenData {
    val domainGoalType = when (goalType) {
        "steps" -> GoalType.Steps(goalTarget ?: 10000)
        "distance" -> GoalType.Distance(
            goalTarget?.toDouble() ?: 0.0, DistanceUnit.KILOMETERS
        )

        "activity_minutes" -> GoalType.ActivityMinutes(goalTarget ?: 0)
        "heart_rate_zone" -> GoalType.HeartRateZone(HeartRateZone.MODERATE, goalTarget ?: 0)
        else -> GoalType.Steps(goalTarget ?: 10000)
    }

    return HomeScreenData(
        date = date,
        totalSteps = totalSteps,
        activeMinutes = activeMinutes,
        totalDistanceMeters = totalDistanceMeters,
        avgBpm = avgBpm,
        minBpm = minBpm,
        maxBpm = maxBpm,
        heartRateCount = heartRateCount,
        activeGoal = ActiveGoalSummary(
            activeGoalsCount = activeGoalsCount,
            goalTarget = goalTarget,
            goalName = goalName,
            goalType = domainGoalType
        )
    )
}