package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.CombinedSteps
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.repository.StepRepository
import java.time.LocalDate
import javax.inject.Inject

class GetDailyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(date: LocalDate): DailySteps? {
        return stepRepository.getDailySteps(date)
    }
}

class GetCombinedStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(date: LocalDate): CombinedSteps {
        val myHealthSteps = stepRepository.getDailySteps(date)?.totalSteps ?: 0
        val healthConnectSteps = stepRepository.getHealthConnectTotalSteps(date)
        return CombinedSteps(
            totalSteps = myHealthSteps + healthConnectSteps,
            myHealthSteps = myHealthSteps,
            healthConnectSteps = healthConnectSteps
        )
    }
}

class SaveDailyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(dailySteps: DailySteps) {
        stepRepository.saveDailySteps(dailySteps)
    }
}

class GetStepsHistoryUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(days: Int): List<DailySteps> {
        return stepRepository.getRecentDays(days)
    }
}

class GetStepsRangeUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): List<DailySteps> {
        return stepRepository.getDailyStepsRange(startDate, endDate)
    }
}

class CalculateDistanceUseCase @Inject constructor() {
    operator fun invoke(steps: Int, strideLengthMeters: Double = 0.762): Double {
        return steps * strideLengthMeters
    }
}

class CalculateActivityMinutesUseCase @Inject constructor() {
    operator fun invoke(stepsPerMinute: Int): Int {
        return if (stepsPerMinute >= 10) 1 else 0
    }
}