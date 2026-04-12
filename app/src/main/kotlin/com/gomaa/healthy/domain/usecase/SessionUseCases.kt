package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.repository.SessionRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(session: ExerciseSession): Result<Unit> {
        return Result.success(sessionRepository.saveSession(session))
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