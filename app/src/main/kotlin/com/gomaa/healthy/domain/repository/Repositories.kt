package com.gomaa.healthy.domain.repository

import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord

interface SessionRepository {
    suspend fun saveSession(session: ExerciseSession)
    suspend fun getSession(id: String): ExerciseSession?
    suspend fun getAllSessions(): List<ExerciseSession>
    suspend fun deleteSession(id: String)
    suspend fun addHeartRates(sessionId: String, heartRates: List<HeartRateRecord>)
    suspend fun getHeartRatesForSession(sessionId: String): List<HeartRateRecord>
}

interface HealthConnectRepository {
    suspend fun writeExerciseSession(session: ExerciseSession): Result<Unit>
    suspend fun writeHeartRateData(sessionId: String, heartRates: List<HeartRateRecord>): Result<Unit>
    suspend fun isAvailable(): Boolean
    suspend fun hasPermissions(): Boolean
}