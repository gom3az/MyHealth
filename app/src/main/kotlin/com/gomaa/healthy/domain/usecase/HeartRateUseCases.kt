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
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HourHeader(
    val hour: Int, val date: String, val minBpm: Int, val avgBpm: Int, val maxBpm: Int
)

fun HeartRateReading.getDateHour(): Pair<String, Int> {
    val instant = Instant.ofEpochMilli(this.timestamp)
    val zone = ZoneId.systemDefault()
    val localDateTime = instant.atZone(zone)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val date = localDateTime.format(dateFormatter)
    val hour = localDateTime.hour
    return Pair(date, hour)
}

class GetRecentHeartRateReadingsUseCase @Inject constructor(
    private val heartRateRepository: HeartRateRepository
) {
    operator fun invoke(
        source: HeartRateSource? = null
    ): Flow<PagingData<HourHeader>> {
        val rawFlow = if (source != null) {
            heartRateRepository.getHeartRatesBySourcePaged(source)
        } else {
            heartRateRepository.getHeartRatesPaged()
        }

        return rawFlow.map { pagingData ->
            val uiItemFlow: PagingData<HourHeader> = pagingData.map { reading ->
                val (date, hour) = reading.getDateHour()
                HourHeader(
                    hour = hour,
                    date = date,
                    minBpm = reading.bpm,
                    avgBpm = reading.bpm,
                    maxBpm = reading.bpm,
                )
            }

            uiItemFlow.insertSeparators { beforeHeader, afterHeader ->
                when {
                    afterHeader == null -> null // End of list
                    beforeHeader == null -> null // Keep the first item
                    beforeHeader.date == afterHeader.date && beforeHeader.hour == afterHeader.hour -> null
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
        return when (this) {
            "health_connect" -> "Health Connect"
            "net.gomaa.healthy" -> "My Health"
            "wearable_huawei_cloud" -> "Huawei"
            else -> split("_").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
        }
    }
}