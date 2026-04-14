package com.gomaa.healthy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailySteps: DailyStepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyStepsList: List<DailyStepsEntity>)

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: Long): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date AND source = :source")
    suspend fun getByDateAndSource(date: Long, source: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAll(): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date ORDER BY source")
    suspend fun getByDateAllSources(date: Long): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DailyStepsEntity>

    @Query("DELETE FROM daily_steps")
    suspend fun deleteAll()

    @Query("DELETE FROM daily_steps WHERE source = :source")
    suspend fun deleteBySource(source: String)

    // Source-of-truth refactor: Track sync status
    @Query("SELECT * FROM daily_steps WHERE date = :date AND source = :source AND synced_to_hc = 0")
    suspend fun getByDateAndSourceNotSynced(date: Long, source: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps WHERE source = :source AND synced_to_hc = 0")
    suspend fun getBySourceNotSynced(source: String): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE source = :source AND date IN (:dates)")
    suspend fun getBySourceAndDates(source: String, dates: Set<Long>): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE source = :source")
    suspend fun getBySource(source: String): List<DailyStepsEntity>

    @Query("UPDATE daily_steps SET synced_to_hc = 1 WHERE date = :date AND source = :source")
    suspend fun markAsSynced(date: Long, source: String)

    @Query("UPDATE daily_steps SET synced_to_hc = 1 WHERE date IN (:dates) AND source = :source")
    suspend fun markAsSynced(dates: List<Long>, source: String)
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: FitnessGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<FitnessGoalEntity>)

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

    @Query("DELETE FROM fitness_goals")
    suspend fun deleteAll()
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

    @Query("DELETE FROM exercise_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM exercise_sessions WHERE source = :source")
    suspend fun getBySource(source: String): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND healthConnectRecordId = :recordId")
    suspend fun getByHealthConnectRecordId(source: String, recordId: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND startTime = :startTime AND endTime = :endTime")
    suspend fun getBySourceAndTimeRange(
        source: String,
        startTime: Long,
        endTime: Long
    ): ExerciseSessionEntity?

    // Source-of-truth refactor: Track sync status
    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND synced_to_hc = 0")
    suspend fun getBySourceNotSynced(source: String): List<ExerciseSessionEntity>

    @Query("UPDATE exercise_sessions SET synced_to_hc = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE exercise_sessions SET synced_to_hc = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}

@Dao
interface HeartRateDao {
    // HC-061: Change to IGNORE to prevent overwriting existing records
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(heartRates: List<HeartRateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(heartRate: HeartRateEntity)

    @Query("SELECT * FROM heart_rates WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: String): List<HeartRateEntity>

    @Query("SELECT * FROM heart_rates WHERE source = :source ORDER BY timestamp DESC")
    fun getBySource(source: String): Flow<List<HeartRateEntity>>

    // HC-064: Get all readings by source (not just latest)
    @Query("SELECT * FROM heart_rates WHERE source = :source ORDER BY timestamp DESC")
    suspend fun getAllBySource(source: String): List<HeartRateEntity>

    @Query("SELECT * FROM heart_rates WHERE source = :source AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getBySourceAndDateRange(
        source: String,
        startTime: Long,
        endTime: Long
    ): List<HeartRateEntity>

    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC")
    fun getAllHeartRates(): Flow<List<HeartRateEntity>>

    // HC-060: Get all readings for date range - returns all readings, not empty
    @Query("SELECT * FROM heart_rates WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getHeartRatesByDateRange(startTime: Long, endTime: Long): List<HeartRateEntity>

    @Query("SELECT * FROM heart_rates WHERE source = :source AND timestamp = :timestamp LIMIT 1")
    suspend fun getBySourceAndTimestamp(source: String, timestamp: Long): HeartRateEntity?

    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): HeartRateEntity?

    @Query("SELECT * FROM heart_rates WHERE source = :source ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBySource(source: String): HeartRateEntity?

    // HC-059: Query ALL existing record IDs for deduplication
    // Fixed: Return List<String> instead of List<String?>
    @Query("SELECT healthConnectRecordId FROM heart_rates WHERE source = :source AND healthConnectRecordId IS NOT NULL")
    suspend fun getAllRecordIdsBySource(source: String): List<String>

    @Query("SELECT AVG(bpm) FROM heart_rates WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long): Double?

    @Query("SELECT MAX(bpm) FROM heart_rates WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getMaxHeartRate(startTime: Long, endTime: Long): Int?

    @Query("SELECT MIN(bpm) FROM heart_rates WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getMinHeartRate(startTime: Long, endTime: Long): Int?

    @Query("SELECT COUNT(*) FROM heart_rates WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getHeartRateCount(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM heart_rates WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query("DELETE FROM heart_rates")
    suspend fun deleteAll()

    @Query("DELETE FROM heart_rates WHERE source = :source")
    suspend fun deleteBySource(source: String)

    // Source-of-truth refactor: Track sync status
    @Query("SELECT * FROM heart_rates WHERE source = :source AND synced_to_hc = 0")
    suspend fun getBySourceNotSynced(source: String): List<HeartRateEntity>

    @Query("UPDATE heart_rates SET synced_to_hc = 1 WHERE timestamp = :timestamp AND source = :source")
    suspend fun markAsSynced(timestamp: Long, source: String)

    @Query("UPDATE heart_rates SET synced_to_hc = 1 WHERE timestamp IN (:timestamps) AND source = :source")
    suspend fun markAsSynced(timestamps: List<Long>, source: String)
}