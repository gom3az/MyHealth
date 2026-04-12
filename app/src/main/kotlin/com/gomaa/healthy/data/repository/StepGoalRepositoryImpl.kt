package com.gomaa.healthy.data.repository

import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HealthConnectStepsDao
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toEntity
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.repository.GoalRepository
import com.gomaa.healthy.domain.repository.StepRepository
import java.time.LocalDate
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val dailyStepsDao: DailyStepsDao,
    private val healthConnectStepsDao: HealthConnectStepsDao
) : StepRepository {

    override suspend fun saveDailySteps(dailySteps: DailySteps) {
        dailyStepsDao.insert(dailySteps.toEntity())
    }

    override suspend fun getDailySteps(date: LocalDate): DailySteps? {
        return dailyStepsDao.getByDate(date.toEpochDay())?.toDomain()
    }

    override suspend fun getDailyStepsRange(startDate: LocalDate, endDate: LocalDate): List<DailySteps> {
        return dailyStepsDao.getByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .map { it.toDomain() }
    }

    override suspend fun getRecentDays(days: Int): List<DailySteps> {
        return dailyStepsDao.getRecent(days).map { it.toDomain() }
    }

    override suspend fun getHealthConnectTotalSteps(date: LocalDate): Int {
        val startOfDay = date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        val endOfDay =
            date.plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        return healthConnectStepsDao.getTotalStepsByDateRange(startOfDay, endOfDay) ?: 0
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