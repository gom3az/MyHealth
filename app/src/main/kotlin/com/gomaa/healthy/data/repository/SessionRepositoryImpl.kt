package com.gomaa.healthy.data.repository

import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toEntity
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord
import com.gomaa.healthy.domain.repository.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: ExerciseSessionDao,
    private val heartRateDao: HeartRateDao
) : SessionRepository {

    override suspend fun saveSession(session: ExerciseSession) {
        sessionDao.insert(session.toEntity())
    }

    override suspend fun getSession(id: String): ExerciseSession? {
        val entity = sessionDao.getById(id) ?: return null
        val heartRates = heartRateDao.getForSession(id).map { it.toDomain() }
        return entity.toDomain(heartRates)
    }

    override suspend fun getAllSessions(): List<ExerciseSession> {
        return sessionDao.getAll().map { entity ->
            val heartRates = heartRateDao.getForSession(entity.id).map { it.toDomain() }
            entity.toDomain(heartRates)
        }
    }

    override suspend fun deleteSession(id: String) {
        heartRateDao.deleteForSession(id)
        sessionDao.delete(id)
    }

    override suspend fun addHeartRates(sessionId: String, heartRates: List<HeartRateRecord>) {
        heartRateDao.insertAll(heartRates.map { it.toEntity(sessionId) })
    }

    override suspend fun getHeartRatesForSession(sessionId: String): List<HeartRateRecord> {
        return heartRateDao.getForSession(sessionId).map { it.toDomain() }
    }
}