package com.gomaa.healthy.domain.usecase

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

sealed class HeartRateUiItem {
    data class HourHeader(
        val hour: Int, val avgBpm: Int, val count: Int
    ) : HeartRateUiItem()

    data class Reading(
        val heartRateReading: HeartRateReading
    ) : HeartRateUiItem()
}

fun HeartRateReading.getHour(): Int {
    return Instant.ofEpochMilli(this.timestamp).atZone(ZoneId.systemDefault()).hour
}

class GetRecentHeartRateReadingsUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    operator fun invoke(
        source: HeartRateSource? = null
    ): Flow<PagingData<HeartRateUiItem>> {
        val rawFlow = if (source != null) {
            heartRateRepository.getHeartRatesBySourcePaged(source)
        } else {
            heartRateRepository.getHeartRatesPaged()
        }

        return rawFlow.map { pagingData ->
            pagingData.map { HeartRateUiItem.Reading(it) }.insertSeparators { before, after ->
                val beforeReading = before?.heartRateReading
                val afterReading = after?.heartRateReading

                when {
                    afterReading == null -> null
                    beforeReading == null -> {
                        HeartRateUiItem.HourHeader(afterReading.getHour(), afterReading.bpm, 1)
                    }

                    beforeReading.getHour() != afterReading.getHour() -> {
                        HeartRateUiItem.HourHeader(afterReading.getHour(), afterReading.bpm, 1)
                    }

                    else -> null
                }
            }
        }
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

class SourceFilterOption(
    val id: String, val displayName: String
)

class GetAvailableSourcesUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    suspend operator fun invoke(): List<SourceFilterOption> {
        val sources = heartRateRepository.getAvailableSources()
        return sources.map { source ->
            SourceFilterOption(
                id = source, displayName = source.toTitleCase()
            )
        }
    }

    private fun String.toTitleCase(): String {
        return split("_").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
    }
}