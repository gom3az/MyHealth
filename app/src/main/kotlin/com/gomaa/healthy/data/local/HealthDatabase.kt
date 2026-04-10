package com.gomaa.healthy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity

@Database(
    entities = [ExerciseSessionEntity::class, HeartRateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun heartRateDao(): HeartRateDao
}