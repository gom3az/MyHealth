package com.gomaa.healthy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity

@Dao
interface DailyStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailySteps: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: Long): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DailyStepsEntity>
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: FitnessGoalEntity)

    @Query("SELECT * FROM fitness_goals WHERE id = :id")
    suspend fun getById(id: String): FitnessGoalEntity?

    @Query("SELECT * FROM fitness_goals WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveGoals(): List<FitnessGoalEntity>

    @Query("SELECT * FROM fitness_goals ORDER BY createdAt DESC")
    suspend fun getAllGoals(): List<FitnessGoalEntity>

    @Query("UPDATE fitness_goals SET isActive = :isActive WHERE id = :id")
    suspend fun updateStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM fitness_goals WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ExerciseSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions WHERE id = :id")
    suspend fun getById(id: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<ExerciseSessionEntity>

    @Query("DELETE FROM exercise_sessions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heartRates: List<HeartRateEntity>)

    @Query("SELECT * FROM heart_rates WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: String): List<HeartRateEntity>

    @Query("DELETE FROM heart_rates WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}