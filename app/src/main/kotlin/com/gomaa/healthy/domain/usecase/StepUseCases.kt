package com.gomaa.healthy.domain.usecase

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.CombinedSteps
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ReadingSource
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
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

class GetPaginatedBySourceDailyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(source: ReadingSource? = null): Flow<PagingData<DailySteps>> {
        return if (source == null)
            stepRepository.getPaginatedDailySteps()
        else
            stepRepository.getPaginatedBySourceDailySteps(source)

    }
}

class GetStepsAvailableFiltersUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(): List<SourceFilterOption> {
        val sources = stepRepository.getAvailableSources()
        return sources.map { source ->
            SourceFilterOption(
                id = source.dbString, displayName = source.displayName
            )
        }.distinctBy { it.displayName }
    }
}