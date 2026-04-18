package com.gomaa.healthy.domain.repository

import androidx.paging.PagingData
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.DateRangeFilter
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.model.ReadingSource
import com.gomaa.healthy.domain.usecase.HourHeader
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface StepRepository {
    suspend fun getDailySteps(date: LocalDate): DailySteps?
    suspend fun getPaginatedDailySteps(): Flow<PagingData<DailySteps>>
    suspend fun getPaginatedBySourceDailySteps(source: ReadingSource): Flow<PagingData<DailySteps>>
    suspend fun getPaginatedByDateRange(dateRange: DateRangeFilter): Flow<PagingData<DailySteps>>
    suspend fun getPaginatedBySourceAndDateRange(
        source: ReadingSource,
        dateRange: DateRangeFilter
    ): Flow<PagingData<DailySteps>>
    suspend fun getAvailableSources(): List<ReadingSource>
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
    suspend fun getAvailableSources(): List<ReadingSource>

    fun getAggregatedBucketsPaged(): Flow<PagingData<HourHeader>>
    fun getAggregatedBucketsBySourcePaged(source: ReadingSource): Flow<PagingData<HourHeader>>
    fun getAggregatedBucketsByDateRange(dateRange: DateRangeFilter): Flow<PagingData<HourHeader>>
    fun getAggregatedBucketsBySourceAndDateRange(
        source: ReadingSource,
        dateRange: DateRangeFilter
    ): Flow<PagingData<HourHeader>>

    // All-time summary (date-agnostic)
    suspend fun getOverallSummary(): HeartRateSummary?

    // Today's specific summary
    suspend fun getTodaySummary(date: LocalDate): HeartRateSummary?

    // Bucket operations for performance optimization
    suspend fun upsertHeartRateWithBucket(
        timestamp: Long,
        source: String,
        bpm: Int,
        sessionId: String? = null
    )
}