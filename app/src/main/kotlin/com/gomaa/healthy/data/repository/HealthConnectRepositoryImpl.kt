package com.gomaa.healthy.data.repository

import android.content.Context
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord as DomainHeartRateRecord
import com.gomaa.healthy.domain.repository.HealthConnectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HealthConnectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HealthConnectRepository {

    override suspend fun writeExerciseSession(session: ExerciseSession): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeHeartRateData(
        sessionId: String,
        heartRates: List<DomainHeartRateRecord>
    ): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        return false
    }

    override suspend fun hasPermissions(): Boolean {
        return false
    }
}