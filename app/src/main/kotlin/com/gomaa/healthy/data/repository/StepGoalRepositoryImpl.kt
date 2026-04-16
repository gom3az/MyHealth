package com.gomaa.healthy.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toEntity
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.repository.GoalRepository
import com.gomaa.healthy.domain.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val dailyStepsDao: DailyStepsDao
) : StepRepository {


    override suspend fun getDailySteps(date: LocalDate): DailySteps? {
        // Get myhealth steps (source defaults to "myhealth")
        return dailyStepsDao.getByDateAndSource(date.toEpochDay(), "myhealth")?.toDomain()
    }

    override suspend fun getHealthConnectTotalSteps(date: LocalDate): Int {
        // Get health connect steps using the new unified table
        return dailyStepsDao.getByDateAndSource(date.toEpochDay(), "health_connect")?.totalSteps
            ?: 0
    }

    override suspend fun getPaginatedDailySteps(): Flow<PagingData<DailySteps>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            pagingSourceFactory = { dailyStepsDao.getPaginatedDailySteps() }).flow.map { it.map { entity -> entity.toDomain() } }
    }

    override suspend fun getPaginatedBySourceDailySteps(source: HeartRateSource): Flow<PagingData<DailySteps>> {
        val sourceString = source.dbString
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            pagingSourceFactory = { dailyStepsDao.getPaginatedDailyStepsBySource(sourceString) }).flow.map { it.map { entity -> entity.toDomain() } }
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