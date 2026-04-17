package com.gomaa.healthy.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gomaa.healthy.data.local.entity.AggregatedHeartRateBucket
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateBucketEntity
import com.gomaa.healthy.domain.model.HomeScreenData

@Dao
interface DailyStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailySteps: DailyStepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyStepsList: List<DailyStepsEntity>)

    @Query("SELECT * FROM daily_steps WHERE date = :date AND source = :source")
    suspend fun getByDateAndSource(date: Long, source: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: Long): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAll(): List<DailyStepsEntity>

    @Query("DELETE FROM daily_steps")
    suspend fun deleteAll()

    @Query("SELECT * FROM daily_steps WHERE source = :source AND synced_to_hc = 0")
    suspend fun getBySourceNotSynced(source: String): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE source = :source AND date IN (:dates)")
    suspend fun getBySourceAndDates(source: String, dates: Set<Long>): List<DailyStepsEntity>

    @Query("UPDATE daily_steps SET synced_to_hc = 1 WHERE date IN (:dates) AND source = :source")
    suspend fun markAsSynced(dates: List<Long>, source: String)

    @Query("SELECT * FROM daily_steps WHERE source = :source ORDER BY date DESC")
    fun getPaginatedDailyStepsBySource(source: String): PagingSource<Int, DailyStepsEntity>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun getPaginatedDailySteps(): PagingSource<Int, DailyStepsEntity>

    @Query("SELECT DISTINCT source FROM daily_steps ORDER BY source")
    suspend fun getDistinctSources(): List<String>
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

    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND startTime BETWEEN :startTime AND :endTime")
    suspend fun getBySourceAndDateRange(
        source: String, startTime: Long, endTime: Long
    ): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND healthConnectRecordId = :recordId")
    suspend fun getByHealthConnectRecordId(source: String, recordId: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE source = :source AND startTime = :startTime AND endTime = :endTime")
    suspend fun getBySourceAndTimeRange(
        source: String, startTime: Long, endTime: Long
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
interface HeartRateBucketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bucket: HeartRateBucketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buckets: List<HeartRateBucketEntity>)

    @Query("SELECT * FROM heart_rate_buckets ORDER BY bucketId DESC LIMIT 1")
    suspend fun getLatest(): HeartRateBucketEntity?

    @Query("SELECT DISTINCT source FROM heart_rate_buckets WHERE bucketId LIKE '____-__-__-__' ORDER BY source")
    suspend fun getDistinctSources(): List<String>

    @Query("SELECT * FROM heart_rate_buckets WHERE bucketId = :bucketId")
    suspend fun getByBucketId(bucketId: String): HeartRateBucketEntity?

    // All-time summary queries (date-agnostic - no date range)
    @Query("SELECT AVG(avgBpm) FROM heart_rate_buckets")
    suspend fun getOverallAverageBpm(): Double?

    @Query("SELECT MAX(maxBpm) FROM heart_rate_buckets")
    suspend fun getOverallMaxBpm(): Int?

    @Query("SELECT MIN(minBpm) FROM heart_rate_buckets")
    suspend fun getOverallMinBpm(): Int?

    @Query("SELECT COUNT(*) FROM heart_rate_buckets")
    suspend fun getOverallCount(): Int

    // Today's summary queries
    @Query(
        """
        SELECT AVG(avgBpm) 
        FROM heart_rate_buckets 
        WHERE dayTimestamp >= :startOfDay AND dayTimestamp < :endOfDay
    """
    )
    suspend fun getTodayAverageBpm(startOfDay: Long, endOfDay: Long): Double?

    @Query(
        """
        SELECT MAX(maxBpm) 
        FROM heart_rate_buckets 
        WHERE dayTimestamp >= :startOfDay AND dayTimestamp < :endOfDay
    """
    )
    suspend fun getTodayMaxBpm(startOfDay: Long, endOfDay: Long): Int?

    @Query(
        """
        SELECT MIN(minBpm) 
        FROM heart_rate_buckets 
        WHERE dayTimestamp >= :startOfDay AND dayTimestamp < :endOfDay
    """
    )
    suspend fun getTodayMinBpm(startOfDay: Long, endOfDay: Long): Int?

    @Query(
        """
        SELECT COUNT(*) 
        FROM heart_rate_buckets 
        WHERE dayTimestamp >= :startOfDay AND dayTimestamp < :endOfDay
    """
    )
    suspend fun getTodayCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM heart_rate_buckets WHERE source = :source AND synced_to_hc = 0")
    suspend fun getBySourceNotSynced(source: String): List<HeartRateBucketEntity>

    @Query("UPDATE heart_rate_buckets SET synced_to_hc = 1 WHERE dayTimestamp IN (:timestamps) AND source = :source")
    suspend fun markAsSynced(timestamps: List<Long>, source: String)

    @Query("SELECT healthConnectRecordId FROM heart_rate_buckets WHERE source = :source AND healthConnectRecordId IS NOT NULL")
    suspend fun getAllRecordIdsBySource(source: String): List<String>

    @Query("SELECT * FROM heart_rate_buckets WHERE source = :source AND dayTimestamp >= :startTime AND dayTimestamp <= :endTime ORDER BY dayTimestamp DESC")
    suspend fun getBySourceAndDateRange(
        source: String, startTime: Long, endTime: Long
    ): List<HeartRateBucketEntity>

    @Query("SELECT * FROM heart_rate_buckets WHERE source = :source ORDER BY bucketId DESC")
    suspend fun getAllBySource(source: String): List<HeartRateBucketEntity>

    @Query("SELECT * FROM heart_rate_buckets WHERE sessionId = :sessionId ORDER BY bucketId ASC")
    suspend fun getForSession(sessionId: String): List<HeartRateBucketEntity>

    @Query("DELETE FROM heart_rate_buckets WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query(
        """
        SELECT bucketId, source, dayTimestamp, minBpm, avgBpm, maxBpm, count
        FROM heart_rate_buckets
        WHERE bucketId LIKE '____-__-__-__'
        ORDER BY bucketId DESC
    """
    )
    fun getAggregatedBucketsPaged(): PagingSource<Int, AggregatedHeartRateBucket>

    @Query(
        """
        SELECT bucketId, source, dayTimestamp, minBpm, avgBpm, maxBpm, count
        FROM heart_rate_buckets
        WHERE source = :source AND bucketId LIKE '____-__-__-__'
        ORDER BY bucketId DESC
    """
    )
    fun getAggregatedBucketsBySourcePaged(source: String): PagingSource<Int, AggregatedHeartRateBucket>

    @Transaction
    @Query("SELECT 1")
    suspend fun upsertBuckets(buckets: List<HeartRateBucketEntity>): Unit {
        for (bucket in buckets) {
            val existing = getByBucketId(bucket.bucketId)
            if (existing != null) {
                val mergedCount = existing.count + bucket.count
                val mergedMinBpm = minOf(existing.minBpm, bucket.minBpm)
                val mergedMaxBpm = maxOf(existing.maxBpm, bucket.maxBpm)
                val mergedAvgBpm = if (mergedCount > 0) {
                    (existing.avgBpm * existing.count + bucket.avgBpm * bucket.count) / mergedCount
                } else {
                    bucket.avgBpm
                }
                val mergedSamplesJson =
                    existing.samplesJson.dropLast(1) + "," + bucket.samplesJson.drop(1)
                insert(
                    existing.copy(
                        minBpm = mergedMinBpm,
                        maxBpm = mergedMaxBpm,
                        avgBpm = mergedAvgBpm,
                        count = mergedCount,
                        samplesJson = mergedSamplesJson
                    )
                )
            } else {
                insert(bucket)
            }
        }
    }
}

@Dao
interface BriefDao {

    @Query(
        """
        SELECT
            ds.date as date,
            ds.totalSteps as totalSteps,
            ds.activeMinutes as activeMinutes,
            ds.totalDistanceMeters as totalDistanceMeters,
            hr.avgBpm as avgBpm,
            hr.minBpm as minBpm,
            hr.maxBpm as maxBpm,
            COALESCE(hr.heartRateCount, 0) as heartRateCount,
            fs.fitnessGoals as activeGoalsCount,
            fs.targetValue as goalTarget,
            fs.name as goalName,
            fs.type as goalType
        FROM daily_steps ds
        LEFT OUTER JOIN (
            SELECT
                dayTimestamp,
                AVG(avgBpm) as avgBpm,
                MIN(minBpm) as minBpm,
                MAX(maxBpm) as maxBpm,
                SUM(count) as heartRateCount
            FROM heart_rate_buckets
            GROUP BY dayTimestamp
        ) hr ON hr.dayTimestamp = :epochMillis
        LEFT OUTER JOIN (
            SELECT 
                targetValue,
                count(*) as fitnessGoals,
                name,
                type
            FROM fitness_goals
            LIMIT 1
        ) fs ON 1=1
        WHERE ds.date = :epochDay
    """
    )
    suspend fun getHomeScreenData(epochDay: Long, epochMillis: Long): HomeScreenData?
}