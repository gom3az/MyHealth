package com.gomaa.healthy.domain.repository

import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateRecord
import com.gomaa.healthy.domain.model.HeartRateSource
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
    suspend fun addHeartRates(sessionId: String, heartRates: List<HeartRateRecord>)
    suspend fun getHeartRatesForSession(sessionId: String): List<HeartRateRecord>
}

interface HeartRateRepository {
    suspend fun getLatestHeartRate(): HeartRateReading?
    suspend fun getLatestHeartRateBySource(source: HeartRateSource): HeartRateReading?
    suspend fun getHeartRatesForDateRange(startTime: Long, endTime: Long): List<HeartRateReading>
    suspend fun getAllHeartRates(): List<HeartRateReading>
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long): Int?
    suspend fun getMaxHeartRate(startTime: Long, endTime: Long): Int?
    suspend fun getMinHeartRate(startTime: Long, endTime: Long): Int?
    suspend fun getHeartRateCount(startTime: Long, endTime: Long): Int
}