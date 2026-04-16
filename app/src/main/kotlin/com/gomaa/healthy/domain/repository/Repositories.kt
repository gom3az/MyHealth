package com.gomaa.healthy.domain.repository

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.usecase.HourHeader
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StepRepository {
    suspend fun saveDailySteps(dailySteps: DailySteps)
    suspend fun getDailySteps(date: LocalDate): DailySteps?
    suspend fun getDailyStepsRange(startDate: LocalDate, endDate: LocalDate): List<DailySteps>
    suspend fun getRecentDays(days: Int): List<DailySteps>
    suspend fun getHealthConnectTotalSteps(date: LocalDate): Int
}

interface GoalRepository {
    suspend fun saveGoal(goal: FitnessGoal)
    suspend fun getGoal(id: String): FitnessGoal?
    suspend fun getActiveGoals(): List<FitnessGoal>
    suspend fun getAllGoals(): List<FitnessGoal>
    suspend fun updateGoalStatus(id: String, isActive: Boolean)
    suspend fun deleteGoal(id: String)
}

interface SessionRepository {
    suspend fun saveSession(session: ExerciseSession)
    suspend fun getSession(id: String): ExerciseSession?
    suspend fun getAllSessions(): List<ExerciseSession>
    suspend fun deleteSession(id: String)
}

interface HeartRateRepository {
    suspend fun getLatestHeartRate(): HeartRateReading?
    suspend fun getAvailableSources(): List<HeartRateSource>

    fun getAggregatedBucketsPaged(): Flow<PagingData<HourHeader>>
    fun getAggregatedBucketsBySourcePaged(source: HeartRateSource): Flow<PagingData<HourHeader>>

    // All-time summary (date-agnostic)
    suspend fun getOverallSummary(): HeartRateSummary?

    // Bucket operations for performance optimization
    suspend fun upsertHeartRateWithBucket(
        timestamp: Long,
        source: String,
        bpm: Int,
        sessionId: String? = null
    )
}