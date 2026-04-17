package com.gomaa.healthy.di

import android.content.Context
import androidx.work.WorkManager
import com.gomaa.healthy.BuildConfig
import com.gomaa.healthy.data.local.HealthDatabase
import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HeartRateBucketDao
import com.gomaa.healthy.data.repository.GoalRepositoryImpl
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectRepositoryInterface
import com.gomaa.healthy.data.repository.HeartRateRepositoryImpl
import com.gomaa.healthy.data.repository.SessionRepositoryImpl
import com.gomaa.healthy.data.repository.StepRepositoryImpl
import com.gomaa.healthy.data.security.EncryptedPreferencesManager
import com.gomaa.healthy.data.sync.DataMerger
import com.gomaa.healthy.data.sync.DataMergerImpl
import com.gomaa.healthy.domain.repository.GoalRepository
import com.gomaa.healthy.domain.repository.HeartRateRepository
import com.gomaa.healthy.domain.repository.SessionRepository
import com.gomaa.healthy.domain.repository.StepRepository
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import com.gomaa.healthy.domain.usecase.SaveSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Provides a Flow for Health Connect sync enabled status.
     * This replaces the deprecated EncryptedSharedPreferences for sync settings.
     */
    @Provides
    @Singleton
    @Named("health_connect_sync_enabled")
    fun provideHealthConnectSyncEnabledFlow(
        encryptedPrefsManager: EncryptedPreferencesManager
    ): Flow<Boolean> {
        return encryptedPrefsManager.observeEncryptedBoolean(
            EncryptedPreferencesManager.KEY_SYNC_ENABLED
        )
    }

    /**
     * Provides a Flow for Health Connect last sync time.
     * This replaces the deprecated EncryptedSharedPreferences for sync metadata.
     */
    @Provides
    @Singleton
    @Named("health_connect_last_sync")
    fun provideHealthConnectLastSyncFlow(
        encryptedPrefsManager: EncryptedPreferencesManager
    ): Flow<Long> {
        return encryptedPrefsManager.observeEncryptedLong(
            EncryptedPreferencesManager.KEY_LAST_SYNC_TIME
        )
    }

    @Provides
    fun provideEnableFileLogging(): Boolean {
        return BuildConfig.DEBUG
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptedPrefsManager: EncryptedPreferencesManager
    ): HealthDatabase {
        return HealthDatabase.getDatabase(context, encryptedPrefsManager)
    }

    @Provides
    @Singleton
    fun provideExerciseSessionDao(database: HealthDatabase): ExerciseSessionDao {
        return database.exerciseSessionDao()
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
    fun provideHeartRateBucketDao(database: HealthDatabase): HeartRateBucketDao {
        return database.heartRateBucketDao()
    }

    @Provides
    @Singleton
    fun provideBriefDao(database: HealthDatabase): BriefDao {
        return database.briefDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: ExerciseSessionDao, heartRateDao: HeartRateBucketDao
    ): SessionRepository {
        return SessionRepositoryImpl(sessionDao, heartRateDao)
    }

    @Provides
    @Singleton
    fun provideStepRepository(
        dailyStepsDao: DailyStepsDao
    ): StepRepository {
        return StepRepositoryImpl(dailyStepsDao)
    }

    @Provides
    @Singleton
    fun provideGoalRepository(
        goalDao: GoalDao
    ): GoalRepository {
        return GoalRepositoryImpl(goalDao)
    }

    @Provides
    @Singleton
    fun provideHeartRateRepository(
        heartRateDao: HeartRateBucketDao
    ): HeartRateRepository {
        return HeartRateRepositoryImpl(heartRateDao)
    }

    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        healthConnectRepository: HealthConnectRepository
    ): HealthConnectRepositoryInterface {
        return healthConnectRepository
    }

    @Provides
    @Singleton
    fun provideDataMerger(
        impl: DataMergerImpl
    ): DataMerger = impl
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