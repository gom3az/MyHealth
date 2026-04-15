package com.gomaa.healthy.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.gomaa.healthy.data.local.dao.HeartRateBucketDao
import com.gomaa.healthy.data.local.entity.HeartRateBucketEntity
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.toDomainReadings
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class HeartRateRepositoryImpl @Inject constructor(
    private val heartRateBucketDao: HeartRateBucketDao
) : HeartRateRepository {

    override suspend fun getLatestHeartRate(): HeartRateReading? {
        return heartRateBucketDao.getLatest()?.toDomainReadings()?.lastOrNull()
    }

    override suspend fun getAvailableSources(): List<String> {
        return heartRateBucketDao.getDistinctSources()
    }

    override fun getHeartRatesPaged(): Flow<PagingData<HeartRateReading>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ), pagingSourceFactory = { heartRateBucketDao.getHeartRateReadingsPaged() }
        ).flow.map { pagingData ->
            pagingData
                .filter { it.toDomainReadings().isNotEmpty() }
                .map { it.toDomainReadings().last() }
        }
    }

    override fun getHeartRatesBySourcePaged(source: HeartRateSource): Flow<PagingData<HeartRateReading>> {
        val sourceString = sourceToString(source)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ), pagingSourceFactory = {
                heartRateBucketDao.getHeartRateReadingsBySourcePaged(
                    sourceString
                )
            }
        ).flow.map { pagingData ->
            pagingData
                .filter { it.toDomainReadings().isNotEmpty() }
                .map { it.toDomainReadings().last() }
        }
    }

    override suspend fun getOverallSummary(): HeartRateSummary? {
        val avg = heartRateBucketDao.getOverallAverageBpm() ?: return null
        val max = heartRateBucketDao.getOverallMaxBpm() ?: return null
        val min = heartRateBucketDao.getOverallMinBpm() ?: return null
        val count = heartRateBucketDao.getOverallCount()
        return HeartRateSummary(avg.toInt(), max, min, count)
    }

    /**
     * Upserts a heart rate reading to both the legacy table and the bucket table.
     * This ensures real-time data is available while preparing for bucket-based storage.
     */
    override suspend fun upsertHeartRateWithBucket(
        timestamp: Long, source: String, bpm: Int, sessionId: String?
    ) {
        val bucketId = generateBucketId(timestamp)
        val dayTimestamp = generateDayTimestamp(timestamp)

        val existingBucket = heartRateBucketDao.getByBucketId(bucketId)

        if (existingBucket != null) {
            // Merge with existing bucket - recalculate min/avg/max
            val newCount = existingBucket.count + 1
            val newMin = minOf(existingBucket.minBpm, bpm)
            val newMax = maxOf(existingBucket.maxBpm, bpm)
            val newAvg = ((existingBucket.avgBpm * existingBucket.count) + bpm) / newCount

            // Add sample to samplesJson
            val samplesJson = addSampleToJson(existingBucket.samplesJson, timestamp, bpm)

            val updatedBucket = existingBucket.copy(
                minBpm = newMin,
                avgBpm = newAvg,
                maxBpm = newMax,
                count = newCount,
                samplesJson = samplesJson
            )
            heartRateBucketDao.insert(updatedBucket)
        } else {
            // Create new bucket with single sample
            val samplesJson = createSamplesJson(timestamp, bpm)
            val bucket = HeartRateBucketEntity(
                bucketId = bucketId,
                source = source,
                dayTimestamp = dayTimestamp,
                minBpm = bpm,
                avgBpm = bpm,
                maxBpm = bpm,
                count = 1,
                samplesJson = samplesJson,
                healthConnectRecordId = "",
            )
            heartRateBucketDao.insert(bucket)
        }
    }

    private fun generateBucketId(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    private fun generateDayTimestamp(timestamp: Long): Long {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun createSamplesJson(timestamp: Long, bpm: Int): String {
        val sample = JSONObject().put("t", timestamp / 1000).put("v", bpm)
        return JSONArray(listOf(sample)).toString()
    }

    private fun addSampleToJson(existingJson: String, timestamp: Long, bpm: Int): String {
        return try {
            val array = JSONArray(existingJson)
            val sample = JSONObject().put("t", timestamp / 1000).put("v", bpm)
            array.put(sample)
            array.toString()
        } catch (e: Exception) {
            createSamplesJson(timestamp, bpm)
        }
    }

    // Methods with source filter for filtering summary calculations
    private fun sourceToString(source: HeartRateSource): String {
        return when (source) {
            HeartRateSource.MY_HEALTH -> "myhealth"
            HeartRateSource.HEALTH_CONNECT -> SOURCE_HEALTH_CONNECT
            HeartRateSource.WEARABLE_HUAWEI_CLOUD -> "wearable_huawei_cloud"
        }
    }

}
