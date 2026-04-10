package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord
import com.gomaa.healthy.domain.repository.HealthConnectRepository
import com.gomaa.healthy.domain.repository.SessionRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(session: ExerciseSession): Result<Unit> {
        sessionRepository.saveSession(session)
        val hcResult = healthConnectRepository.writeExerciseSession(session)
        session.heartRates.takeIf { it.isNotEmpty() }?.let {
            healthConnectRepository.writeHeartRateData(session.id, it)
        }
        return hcResult
    }
}

class GetSessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): List<ExerciseSession> {
        return sessionRepository.getAllSessions()
    }
}

class GetSessionDetailUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: String): ExerciseSession? {
        return sessionRepository.getSession(sessionId)
    }
}

class DeleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: String) {
        sessionRepository.deleteSession(sessionId)
    }
}