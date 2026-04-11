package com.gomaa.healthy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity

@Database(
    entities = [
        ExerciseSessionEntity::class,
        HeartRateEntity::class,
        DailyStepsEntity::class,
        FitnessGoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun goalDao(): GoalDao
}