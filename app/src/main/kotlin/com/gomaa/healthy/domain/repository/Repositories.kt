package com.gomaa.healthy.domain.repository

import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.HeartRateRecord
import java.time.LocalDate

interface StepRepository {
    suspend fun saveDailySteps(dailySteps: DailySteps)
    suspend fun getDailySteps(date: LocalDate): DailySteps?
    suspend fun getDailyStepsRange(startDate: LocalDate, endDate: LocalDate): List<DailySteps>
    suspend fun getRecentDays(days: Int): List<DailySteps>
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