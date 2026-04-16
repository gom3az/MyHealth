package com.gomaa.healthy.data.repository

import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateBucketDao
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toDomainReadings
import com.gomaa.healthy.data.mapper.toEntity
import com.gomaa.healthy.data.mapper.toHeartRateRecord
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.repository.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: ExerciseSessionDao, private val heartRateDao: HeartRateBucketDao
) : SessionRepository {

    override suspend fun saveSession(session: ExerciseSession) {
        sessionDao.insert(session.toEntity())
    }

    override suspend fun getSession(id: String): ExerciseSession? {
        val entity = sessionDao.getById(id) ?: return null
        val heartRates = heartRateDao.getForSession(id).flatMap { it.toDomainReadings() }
            .map { it.toHeartRateRecord() }
        return entity.toDomain(heartRates)
    }

    override suspend fun getAllSessions(): List<ExerciseSession> {
        return sessionDao.getAll().map { entity ->
            val heartRates = heartRateDao.getForSession(entity.id).flatMap { it.toDomainReadings() }
                .map { it.toHeartRateRecord() }
            entity.toDomain(heartRates)
        }
    }

    override suspend fun deleteSession(id: String) {
        heartRateDao.deleteForSession(id)
        sessionDao.delete(id)
    }
}