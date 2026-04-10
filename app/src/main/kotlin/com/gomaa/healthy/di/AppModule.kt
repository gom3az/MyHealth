package com.gomaa.healthy.di

import android.content.Context
import androidx.room.Room
import com.gomaa.healthy.data.local.HealthDatabase
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.repository.HealthConnectRepositoryImpl
import com.gomaa.healthy.data.repository.SessionRepositoryImpl
import com.gomaa.healthy.domain.repository.HealthConnectRepository
import com.gomaa.healthy.domain.repository.SessionRepository
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
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context
    ): HealthConnectRepository {
        return HealthConnectRepositoryImpl(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideSaveSessionUseCase(
        sessionRepository: SessionRepository,
        healthConnectRepository: HealthConnectRepository
    ): SaveSessionUseCase {
        return SaveSessionUseCase(sessionRepository, healthConnectRepository)
    }

    @Provides
    fun provideGetSessionsUseCase(
        sessionRepository: SessionRepository
    ): GetSessionsUseCase {
        return GetSessionsUseCase(sessionRepository)
    }
}