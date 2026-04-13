package com.gomaa.healthy.data.repository

import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toEntity
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.repository.GoalRepository
import com.gomaa.healthy.domain.repository.StepRepository
import java.time.LocalDate
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val dailyStepsDao: DailyStepsDao
) : StepRepository {

    override suspend fun saveDailySteps(dailySteps: DailySteps) {
        dailyStepsDao.insert(dailySteps.toEntity())
    }

    override suspend fun getDailySteps(date: LocalDate): DailySteps? {
        // Get myhealth steps (source defaults to "myhealth")
        return dailyStepsDao.getByDateAndSource(date.toEpochDay(), "myhealth")?.toDomain()
    }

    override suspend fun getDailyStepsRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySteps> {
        return dailyStepsDao.getByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .filter { it.source == "myhealth" }
            .map { it.toDomain() }
    }

    override suspend fun getRecentDays(days: Int): List<DailySteps> {
        return dailyStepsDao.getRecent(days)
            .filter { it.source == "myhealth" }
            .map { it.toDomain() }
    }

    override suspend fun getHealthConnectTotalSteps(date: LocalDate): Int {
        // Get health connect steps using the new unified table
        return dailyStepsDao.getByDateAndSource(date.toEpochDay(), "health_connect")?.totalSteps
            ?: 0
    }
}

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override suspend fun saveGoal(goal: FitnessGoal) {
        goalDao.insert(goal.toEntity())
    }

    override suspend fun getGoal(id: String): FitnessGoal? {
        return goalDao.getById(id)?.toDomain()
    }

    override suspend fun getActiveGoals(): List<FitnessGoal> {
        return goalDao.getActiveGoals().map { it.toDomain() }
    }

    override suspend fun getAllGoals(): List<FitnessGoal> {
        return goalDao.getAllGoals().map { it.toDomain() }
    }

    override suspend fun updateGoalStatus(id: String, isActive: Boolean) {
        goalDao.updateStatus(id, isActive)
    }

    override suspend fun deleteGoal(id: String) {
        goalDao.delete(id)
    }
}