package com.gomaa.healthy.domain.usecase

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.DateRangeFilter
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

class GetPaginatedBySourceDailyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(
        source: ReadingSource? = null, dateRange: DateRangeFilter = DateRangeFilter.All
    ): Flow<PagingData<DailySteps>> {
        return when {
            source == null -> stepRepository.getPaginatedByDateRange(dateRange)
            else -> stepRepository.getPaginatedBySourceAndDateRange(source, dateRange)
        }
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