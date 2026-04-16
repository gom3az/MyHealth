package com.gomaa.healthy.domain.usecase

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
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
        source: HeartRateSource? = null
    ): Flow<PagingData<HourHeader>> {
        if (source != null) {
            return heartRateRepository.getAggregatedBucketsBySourcePaged(source)
        }
        return heartRateRepository.getAggregatedBucketsPaged()
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

class SourceFilterOption(val id: String, val displayName: String)

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