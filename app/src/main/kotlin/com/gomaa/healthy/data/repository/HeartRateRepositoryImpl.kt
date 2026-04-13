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

    override suspend fun getLatestHeartRateBySource(source: HeartRateSource): HeartRateReading? {
        val sourceString = when (source) {
            HeartRateSource.MY_HEALTH -> "myhealth"
            HeartRateSource.HEALTH_CONNECT -> SOURCE_HEALTH_CONNECT
        }
        return heartRateDao.getLatestBySource(sourceString)?.toDomainReading()
    }

    // HC-060: Implement actual database queries instead of emptyList()
    override suspend fun getHeartRatesForDateRange(
        startTime: Long,
        endTime: Long
    ): List<HeartRateReading> {
        return heartRateDao.getHeartRatesByDateRange(startTime, endTime)
            .map { it.toDomainReading() }
    }

    // HC-060: Implement actual database queries instead of emptyList()
    override suspend fun getAllHeartRates(): List<HeartRateReading> {
        // Collect from the flow or use the suspend version
        // For backward compatibility, we convert from the entity list
        val entities = heartRateDao.getHeartRatesByDateRange(0, Long.MAX_VALUE)
        return entities.map { it.toDomainReading() }
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
}
