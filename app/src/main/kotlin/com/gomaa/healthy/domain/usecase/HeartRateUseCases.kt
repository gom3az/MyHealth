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

class GetLatestHeartRateBySourceUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(source: HeartRateSource): HeartRateReading? {
        return heartRateRepository.getLatestHeartRateBySource(source)
    }
}

class GetHeartRateSummaryUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(startTime: Long, endTime: Long): HeartRateSummary? {
        val avg = heartRateRepository.getAverageHeartRate(startTime, endTime)
        val max = heartRateRepository.getMaxHeartRate(startTime, endTime)
        val min = heartRateRepository.getMinHeartRate(startTime, endTime)
        val count = heartRateRepository.getHeartRateCount(startTime, endTime)

        if (avg == null || max == null || min == null) return null

        return HeartRateSummary(
            averageBpm = avg,
            maxBpm = max,
            minBpm = min,
            readingCount = count
        )
    }
}