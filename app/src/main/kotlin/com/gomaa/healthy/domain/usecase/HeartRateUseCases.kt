package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.repository.HeartRateRepository
import javax.inject.Inject

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
    suspend operator fun invoke(
        startTime: Long,
        endTime: Long,
        source: HeartRateSource? = null
    ): HeartRateSummary? {
        val (avg, max, min, count) = if (source != null) {
            // Use source-filtered queries
            val avg = heartRateRepository.getAverageHeartRateBySource(source, startTime, endTime)
            val max = heartRateRepository.getMaxHeartRateBySource(source, startTime, endTime)
            val min = heartRateRepository.getMinHeartRateBySource(source, startTime, endTime)
            val count = heartRateRepository.getHeartRateCountBySource(source, startTime, endTime)
            Quad(avg, max, min, count)
        } else {
            // Use unfiltered queries (all sources)
            val avg = heartRateRepository.getAverageHeartRate(startTime, endTime)
            val max = heartRateRepository.getMaxHeartRate(startTime, endTime)
            val min = heartRateRepository.getMinHeartRate(startTime, endTime)
            val count = heartRateRepository.getHeartRateCount(startTime, endTime)
            Quad(avg, max, min, count)
        }

        if (avg == null || max == null || min == null) return null

        return HeartRateSummary(
            averageBpm = avg,
            maxBpm = max,
            minBpm = min,
            readingCount = count
        )
    }
}

// Simple data class for tuple-like returns
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class GetRecentHeartRateReadingsUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(
        limit: Int = 20,
        source: HeartRateSource? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<HeartRateReading> {
        val readings = when {
            source != null && startTime != null && endTime != null -> {
                // Filter by source AND date range
                heartRateRepository.getHeartRatesBySourceAndDateRange(source, startTime, endTime)
            }

            source != null -> {
                // Filter by source only (all time)
                heartRateRepository.getAllHeartRatesBySource(source)
            }

            startTime != null && endTime != null -> {
                // Filter by date range only (all sources)
                heartRateRepository.getAllHeartRatesByDateRange(startTime, endTime)
            }

            else -> {
                // No filtering - return all from all time
                heartRateRepository.getAllHeartRates()
            }
        }
        return readings.take(limit)
    }
}

class SourceFilterOption(
    val id: String,
    val displayName: String
)

class GetAvailableSourcesUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(): List<SourceFilterOption> {
        val sources = heartRateRepository.getAvailableSources()
        return sources.map { source ->
            SourceFilterOption(
                id = source,
                displayName = source.toTitleCase()
            )
        }
    }

    private fun String.toTitleCase(): String {
        return split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
    }
}