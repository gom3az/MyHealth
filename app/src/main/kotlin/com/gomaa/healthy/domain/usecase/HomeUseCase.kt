package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.data.local.dto.HomeScreenDataDto
import com.gomaa.healthy.domain.model.ActiveGoalSummary
import com.gomaa.healthy.domain.model.DistanceUnit
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.model.HeartRateZone
import com.gomaa.healthy.domain.model.HomeScreenData
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val briefDao: BriefDao,
) {
    suspend operator fun invoke(clock: Clock = Clock.systemDefaultZone()): HomeScreenData? {
        val now = LocalDate.now(clock)
        val epoch = now.toEpochDay()
        val epochMillis = now.atStartOfDay(clock.zone).toInstant().toEpochMilli()

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
    val activeGoal =
        if (goalType == null || goalName == null || goalTarget == null) null else ActiveGoalSummary(
            activeGoalsCount = activeGoalsCount,
            goalTarget = goalTarget,
            goalName = goalName,
            goalType = domainGoalType
        )

    return HomeScreenData(
        date = date,
        totalSteps = totalSteps,
        activeMinutes = activeMinutes,
        totalDistanceMeters = totalDistanceMeters,
        avgBpm = avgBpm,
        minBpm = minBpm,
        maxBpm = maxBpm,
        heartRateCount = heartRateCount,
        activeGoal = activeGoal
    )
}