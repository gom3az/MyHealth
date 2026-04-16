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
