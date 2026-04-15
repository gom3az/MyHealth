package com.gomaa.healthy.data.repository

import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.toDomainReading
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.repository.HeartRateRepository
import javax.inject.Inject

class HeartRateRepositoryImpl @Inject constructor(
    private val heartRateDao: HeartRateDao
) : HeartRateRepository {

    override suspend fun getLatestHeartRate(): HeartRateReading? {
        return heartRateDao.getLatest()?.toDomainReading()
    }

    // HC-060: Implement actual database queries instead of emptyList()
    override suspend fun getAllHeartRates(): List<HeartRateReading> {
        // Collect from the flow or use the suspend version
        // For backward compatibility, we convert from the entity list
        val entities = heartRateDao.getHeartRatesByDateRange(0, Long.MAX_VALUE)
        return entities.map { it.toDomainReading() }
    }

    override suspend fun getAllHeartRatesBySource(source: HeartRateSource): List<HeartRateReading> {
        val sourceString = when (source) {
            HeartRateSource.MY_HEALTH -> "myhealth"
            HeartRateSource.HEALTH_CONNECT -> SOURCE_HEALTH_CONNECT
            HeartRateSource.WEARABLE_HUAWEI_CLOUD -> "wearable_huawei_cloud"
        }
        val entities = heartRateDao.getAllBySource(sourceString)
        return entities.map { it.toDomainReading() }
    }

    // HC-066: Get all sources within date range (for ALL filter)
    override suspend fun getAllHeartRatesByDateRange(
        startTime: Long,
        endTime: Long
    ): List<HeartRateReading> {
        return heartRateDao.getHeartRatesByDateRange(startTime, endTime)
            .map { it.toDomainReading() }
    }

    // HC-066: Get specific source within date range (for MY_HEALTH/HEALTH_CONNECT filters)
    override suspend fun getHeartRatesBySourceAndDateRange(
        source: HeartRateSource,
        startTime: Long,
        endTime: Long
    ): List<HeartRateReading> {
        val sourceString = when (source) {
            HeartRateSource.MY_HEALTH -> "myhealth"
            HeartRateSource.HEALTH_CONNECT -> SOURCE_HEALTH_CONNECT
            HeartRateSource.WEARABLE_HUAWEI_CLOUD -> "wearable_huawei_cloud"
        }
        return heartRateDao.getBySourceAndDateRange(sourceString, startTime, endTime)
            .map { it.toDomainReading() }
    }

    override suspend fun getAverageHeartRate(startTime: Long, endTime: Long): Int? {
        return heartRateDao.getAverageHeartRate(startTime, endTime)?.toInt()
    }

    override suspend fun getMaxHeartRate(startTime: Long, endTime: Long): Int? {
        return heartRateDao.getMaxHeartRate(startTime, endTime)
    }

    override suspend fun getMinHeartRate(startTime: Long, endTime: Long): Int? {
        return heartRateDao.getMinHeartRate(startTime, endTime)
    }

    override suspend fun getHeartRateCount(startTime: Long, endTime: Long): Int {
        return heartRateDao.getHeartRateCount(startTime, endTime)
    }

    // Methods with source filter for filtering summary calculations
    private fun sourceToString(source: HeartRateSource): String {
        return when (source) {
            HeartRateSource.MY_HEALTH -> "myhealth"
            HeartRateSource.HEALTH_CONNECT -> SOURCE_HEALTH_CONNECT
            HeartRateSource.WEARABLE_HUAWEI_CLOUD -> "wearable_huawei_cloud"
        }
    }

    override suspend fun getAverageHeartRateBySource(
        source: HeartRateSource,
        startTime: Long,
        endTime: Long
    ): Int? {
        return heartRateDao.getAverageHeartRateBySource(sourceToString(source), startTime, endTime)
            ?.toInt()
    }

    override suspend fun getMaxHeartRateBySource(
        source: HeartRateSource,
        startTime: Long,
        endTime: Long
    ): Int? {
        return heartRateDao.getMaxHeartRateBySource(sourceToString(source), startTime, endTime)
    }

    override suspend fun getMinHeartRateBySource(
        source: HeartRateSource,
        startTime: Long,
        endTime: Long
    ): Int? {
        return heartRateDao.getMinHeartRateBySource(sourceToString(source), startTime, endTime)
    }

    override suspend fun getHeartRateCountBySource(
        source: HeartRateSource,
        startTime: Long,
        endTime: Long
    ): Int {
        return heartRateDao.getHeartRateCountBySource(sourceToString(source), startTime, endTime)
    }

    override suspend fun getAvailableSources(): List<String> {
        return heartRateDao.getDistinctSources()
    }
}
