package com.gomaa.healthy.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.toDomainReading
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HeartRateRepositoryImpl @Inject constructor(
    private val heartRateDao: HeartRateDao
) : HeartRateRepository {

    override suspend fun getLatestHeartRate(): HeartRateReading? {
        return heartRateDao.getLatest()?.toDomainReading()
    }

    override suspend fun getAvailableSources(): List<String> {
        return heartRateDao.getDistinctSources()
    }

    override fun getHeartRatesPaged(): Flow<PagingData<HeartRateReading>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { heartRateDao.getHeartRateReadingsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomainReading() }
        }
    }

    override fun getHeartRatesBySourcePaged(source: HeartRateSource): Flow<PagingData<HeartRateReading>> {
        val sourceString = sourceToString(source)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { heartRateDao.getHeartRateReadingsBySourcePaged(sourceString) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomainReading() }
        }
    }

    override suspend fun getOverallSummary(): HeartRateSummary? {
        val avg = heartRateDao.getOverallAverageBpm() ?: return null
        val max = heartRateDao.getOverallMaxBpm() ?: return null
        val min = heartRateDao.getOverallMinBpm() ?: return null
        val count = heartRateDao.getOverallCount()
        return HeartRateSummary(avg.toInt(), max, min, count)
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
