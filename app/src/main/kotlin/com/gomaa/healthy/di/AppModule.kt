package com.gomaa.healthy.di

import android.content.Context
import androidx.room.Room
import com.gomaa.healthy.data.local.HealthDatabase
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HealthConnectExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HealthConnectStepsDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.repository.GoalRepositoryImpl
import com.gomaa.healthy.data.repository.SessionRepositoryImpl
import com.gomaa.healthy.data.repository.StepRepositoryImpl
import com.gomaa.healthy.domain.repository.GoalRepository
import com.gomaa.healthy.domain.repository.SessionRepository
import com.gomaa.healthy.domain.repository.StepRepository
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import com.gomaa.healthy.domain.usecase.SaveSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HealthDatabase {
        return Room.databaseBuilder(
            context,
            HealthDatabase::class.java,
            "health_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideExerciseSessionDao(database: HealthDatabase): ExerciseSessionDao {
        return database.exerciseSessionDao()
    }

    @Provides
    @Singleton
    fun provideHeartRateDao(database: HealthDatabase): HeartRateDao {
        return database.heartRateDao()
    }

    @Provides
    @Singleton
    fun provideDailyStepsDao(database: HealthDatabase): DailyStepsDao {
        return database.dailyStepsDao()
    }

    @Provides
    @Singleton
    fun provideGoalDao(database: HealthDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideHealthConnectStepsDao(database: HealthDatabase): HealthConnectStepsDao {
        return database.healthConnectStepsDao()
    }

    @Provides
    @Singleton
    fun provideHealthConnectExerciseSessionDao(database: HealthDatabase): HealthConnectExerciseSessionDao {
        return database.healthConnectExerciseSessionDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: ExerciseSessionDao,
        heartRateDao: HeartRateDao
    ): SessionRepository {
        return SessionRepositoryImpl(sessionDao, heartRateDao)
    }

    @Provides
    @Singleton
    fun provideStepRepository(
        dailyStepsDao: DailyStepsDao,
        healthConnectStepsDao: HealthConnectStepsDao
    ): StepRepository {
        return StepRepositoryImpl(dailyStepsDao, healthConnectStepsDao)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(
        goalDao: GoalDao
    ): GoalRepository {
        return GoalRepositoryImpl(goalDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideSaveSessionUseCase(
        sessionRepository: SessionRepository
    ): SaveSessionUseCase {
        return SaveSessionUseCase(sessionRepository)
    }

    @Provides
    fun provideGetSessionsUseCase(
        sessionRepository: SessionRepository
    ): GetSessionsUseCase {
        return GetSessionsUseCase(sessionRepository)
    }
}