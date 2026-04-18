package com.gomaa.healthy.domain.usecase

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.DateRangeFilter
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.model.ReadingSource
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class HourHeader(
    val hour: Int,
    val date: String,
    val minBpm: Int,
    val avgBpm: Int,
    val maxBpm: Int
)

class GetRecentHeartRateReadingsUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    operator fun invoke(
        source: ReadingSource? = null, dateRange: DateRangeFilter = DateRangeFilter.All
    ): Flow<PagingData<HourHeader>> {
        if (source != null) {
            return heartRateRepository.getAggregatedBucketsBySourceAndDateRange(source, dateRange)
        }
        return heartRateRepository.getAggregatedBucketsByDateRange(dateRange)
    }
}

class GetLatestHeartRateUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(): HeartRateReading? {
        return heartRateRepository.getLatestHeartRate()
    }
}

class GetHeartRateSummaryUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(): HeartRateSummary? {
        return heartRateRepository.getOverallSummary()
    }
}

class GetTodayHeartRateSummaryUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(date: java.time.LocalDate = java.time.LocalDate.now()): HeartRateSummary? {
        return heartRateRepository.getTodaySummary(date)
    }
}

class GetAvailableSourcesUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(): List<SourceFilterOption> {
        val sources = heartRateRepository.getAvailableSources()
        return sources.map { source ->
            SourceFilterOption(
                id = source.dbString, displayName = source.displayName
            )
        }.distinctBy { it.displayName }
    }
}