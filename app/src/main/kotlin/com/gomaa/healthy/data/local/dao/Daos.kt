package com.gomaa.healthy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity

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