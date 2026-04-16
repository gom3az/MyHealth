package com.gomaa.healthy.data.healthkit

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateBucketDao
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.data.sync.DataMerger
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager Worker for periodic sync with Huawei Health Cloud.
 * 
 * This implements the polling-based data synchronization from Phase 3 of the migration plan.
 * Runs every 15 minutes to fetch data from Huawei Health Kit cloud APIs.
 * 
 * Data flow:
 * 1. WorkManager triggers periodic execution (15 min interval)
 * 2. AuthManager validates OAuth token
 * 3. DataSource fetches data from Huawei Health Cloud
 * 4. Data stored to Room (staging)
 * 5. DataMerger merges with existing local data
 * 6. HealthConnectRepository writes merged data
 */
@HiltWorker
class HuaweiHealthKitSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthKitDataSource: HuaweiHealthKitDataSource,
    private val authManager: HuaweiHealthKitAuthManager,
    private val dailyStepsDao: DailyStepsDao,
    private val heartRateDao: HeartRateBucketDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val dataMerger: DataMerger,
    private val healthConnectRepository: HealthConnectRepository,
    private val syncPreferencesManager: SyncPreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HealthKitSyncWorker"
        const val WORK_NAME = "huawei_healthkit_sync"
        const val SYNC_INTERVAL_MINUTES = 15L

        // Default time range for fetching (last 24 hours)
        private const val DEFAULT_SYNC_WINDOW_MS = 24 * 60 * 60 * 1000L

        // Network constraints
        private val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "doWork: Starting Health Kit sync")

                // Step 1: Check authentication
                if (!authManager.isSignedIn()) {
                    Log.w(TAG, "doWork: Not signed in, skipping sync")
                    return@withContext Result.success()  // Don't retry, just skip
                }

                // Step 2: Calculate time range based on sync window preference
                val syncWindowDays = syncPreferencesManager.getSyncWindowDays()
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (syncWindowDays * 24L * 60L * 60L * 1000L)

                Log.d(
                    TAG, "doWork: Sync window = $syncWindowDays days (from $startTime to $endTime)"
                )

                // Step 3: Fetch steps data
                fetchStepsData(startTime, endTime)

                // Step 4: Fetch heart rate data
                fetchHeartRateData(startTime, endTime)

                // Step 5: Fetch workout data
                fetchWorkoutData(startTime, endTime)

                // Step 6: Update last sync timestamps for incremental sync
                syncPreferencesManager.setLastHealthKitSyncTime(endTime)

                Log.i(TAG, "doWork: Sync completed successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "doWork: Sync failed", e)

                // Retry with exponential backoff on network failure
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    private suspend fun fetchStepsData(startTime: Long, endTime: Long) {
        when (val result = healthKitDataSource.readSteps(startTime, endTime)) {
            is HealthKitResult.Success -> {
                val stepsList = result.data
                Log.d(TAG, "fetchStepsData: Retrieved ${stepsList.size} step records")

                if (stepsList.isEmpty()) {
                    Log.d(TAG, "fetchStepsData: No data to process")
                    return
                }

                // Convert cloud data to entities
                val cloudEntities = stepsList.map { HealthKitEntityMapper.stepsToEntity(it) }

                // Step 1: Insert cloud data to staging
                dailyStepsDao.insertAll(cloudEntities)
                Log.d(TAG, "fetchStepsData: Stored ${cloudEntities.size} steps to staging")

                // Step 2: Get existing local data for merging
                val datesToQuery = cloudEntities.map { it.date }.toSet()
                val existingLocalData = dailyStepsDao.getBySourceAndDates(
                    SOURCE_MY_HEALTH, datesToQuery
                )

                // Step 3: Merge cloud data with local data using DataMerger
                val mergedSteps = dataMerger.mergeSteps(cloudEntities, existingLocalData)

                // Step 4: Update merged data in Room
                dailyStepsDao.insertAll(mergedSteps)
                Log.d(TAG, "fetchStepsData: Merged ${mergedSteps.size} step records")

                // Step 5: Write merged data to Health Connect
                val domainSteps = mergedSteps.map { entity ->
                    DailySteps(
                        date = java.time.LocalDate.ofEpochDay(entity.date),
                        totalSteps = entity.totalSteps,
                        totalDistanceMeters = entity.totalDistanceMeters,
                        activeMinutes = entity.activeMinutes,
                        lightActivityMinutes = entity.lightActivityMinutes,
                        moderateActivityMinutes = entity.moderateActivityMinutes,
                        vigorousActivityMinutes = entity.vigorousActivityMinutes
                    )
                }

                when (val writeResult = healthConnectRepository.writeSteps(domainSteps)) {
                    is HealthConnectResult.Success -> {
                        Log.d(
                            TAG, "fetchStepsData: Wrote ${writeResult.data} steps to Health Connect"
                        )
                    }

                    is HealthConnectResult.Error -> {
                        Log.w(
                            TAG,
                            "fetchStepsData: Failed to write to Health Connect: ${writeResult.exception.message}"
                        )
                    }
                }
            }

            is HealthKitResult.Error -> {
                Log.w(TAG, "fetchStepsData: ${result.message}")
            }
        }
    }

    private suspend fun fetchHeartRateData(startTime: Long, endTime: Long) {
        when (val result = healthKitDataSource.readHeartRate(startTime, endTime)) {
            is HealthKitResult.Success -> {
                val heartRates = result.data
                Log.d(TAG, "fetchHeartRateData: Retrieved ${heartRates.size} heart rate records")

                if (heartRates.isEmpty()) {
                    Log.d(TAG, "fetchHeartRateData: No data to process")
                    return
                }

                // Convert cloud data to entities
                val cloudEntities = heartRates.map { HealthKitEntityMapper.heartRateToEntity(it) }

                // Step 1: Upsert cloud data to merge with existing buckets
                heartRateDao.upsertBuckets(cloudEntities)
                Log.d(
                    TAG, "fetchHeartRateData: Stored ${cloudEntities.size} heart rates to staging"
                )

                // Step 2: Get existing local data for merging
                val existingLocalData = heartRateDao.getBySourceAndDateRange(
                    SOURCE_MY_HEALTH, startTime, endTime
                )

                // Step 3: Merge cloud data with local data using DataMerger
                val mergedHeartRates = dataMerger.mergeHeartRates(cloudEntities, existingLocalData)

                // Step 4: Update merged data in Room
                heartRateDao.upsertBuckets(mergedHeartRates)
                Log.d(TAG, "fetchHeartRateData: Merged ${mergedHeartRates.size} heart rate records")

                // Step 5: Write merged data to Health Connect
                val domainHeartRates = mergedHeartRates.map { entity ->
                    HeartRateReading(
                        id = entity.bucketId,
                        bpm = entity.avgBpm,
                        timestamp = entity.dayTimestamp,
                        source = HeartRateSource.WEARABLE_HUAWEI_CLOUD
                    )
                }

                when (val writeResult = healthConnectRepository.writeHeartRates(domainHeartRates)) {
                    is HealthConnectResult.Success -> {
                        Log.d(
                            TAG,
                            "fetchHeartRateData: Wrote ${writeResult.data} heart rates to Health Connect"
                        )
                    }

                    is HealthConnectResult.Error -> {
                        Log.w(
                            TAG,
                            "fetchHeartRateData: Failed to write to Health Connect: ${writeResult.exception.message}"
                        )
                    }
                }
            }

            is HealthKitResult.Error -> {
                Log.w(TAG, "fetchHeartRateData: ${result.message}")
            }
        }
    }

    private suspend fun fetchWorkoutData(startTime: Long, endTime: Long) {
        when (val result = healthKitDataSource.readWorkouts(startTime, endTime)) {
            is HealthKitResult.Success -> {
                val workouts = result.data
                Log.d(TAG, "fetchWorkoutData: Retrieved ${workouts.size} workout records")

                if (workouts.isEmpty()) {
                    Log.d(TAG, "fetchWorkoutData: No data to process")
                    return
                }

                // Convert cloud data to entities
                val cloudEntities = workouts.map { HealthKitEntityMapper.workoutToEntity(it) }

                // Step 1: Insert cloud data to staging
                for (entity in cloudEntities) {
                    exerciseSessionDao.insert(entity)
                }
                Log.d(TAG, "fetchWorkoutData: Stored ${cloudEntities.size} workouts to staging")

                // Step 2: Get existing local data for merging
                val existingLocalData = exerciseSessionDao.getBySourceAndDateRange(
                    SOURCE_MY_HEALTH, startTime, endTime
                )

                // Step 3: Merge cloud data with local data using DataMerger
                val mergedWorkouts =
                    dataMerger.mergeExerciseSessions(cloudEntities, existingLocalData)

                // Step 4: Update merged data in Room
                for (entity in mergedWorkouts) {
                    exerciseSessionDao.insert(entity)
                }
                Log.d(TAG, "fetchWorkoutData: Merged ${mergedWorkouts.size} workout records")

                // Step 5: Write merged data to Health Connect
                for (entity in mergedWorkouts) {
                    val domainWorkout = ExerciseSession(
                        id = entity.id,
                        startTime = entity.startTime,
                        endTime = entity.endTime,
                        avgHeartRate = entity.avgHeartRate,
                        maxHeartRate = entity.maxHeartRate,
                        minHeartRate = entity.minHeartRate,
                        deviceBrand = entity.deviceBrand,
                        exerciseType = entity.exerciseType,
                        title = entity.title
                    )

                    when (val writeResult =
                        healthConnectRepository.writeExerciseSession(domainWorkout)) {
                        is HealthConnectResult.Success -> {
                            Log.d(
                                TAG,
                                "fetchWorkoutData: Wrote workout '${entity.title}' to Health Connect"
                            )
                        }

                        is HealthConnectResult.Error -> {
                            Log.w(
                                TAG,
                                "fetchWorkoutData: Failed to write workout to Health Connect: ${writeResult.exception.message}"
                            )
                        }
                    }
                }
            }

            is HealthKitResult.Error -> {
                Log.w(TAG, "fetchWorkoutData: ${result.message}")
            }
        }
    }
}

/**
 * Schedules the periodic sync work request.
 * Call this on app startup to begin background syncing.
 */
@Singleton
class HuaweiHealthKitScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthKitScheduler"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    private val constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule() {
        val workRequest = PeriodicWorkRequestBuilder<HuaweiHealthKitSyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).setConstraints(constraints).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HuaweiHealthKitSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest
        )

        Log.i(TAG, "schedule: Health Kit sync scheduled every $SYNC_INTERVAL_MINUTES minutes")
    }

    /**
     * Cancels the periodic sync.
     */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(HuaweiHealthKitSyncWorker.WORK_NAME)
        Log.i(TAG, "cancel: Health Kit sync cancelled")
    }

    /**
     * Runs an immediate one-time sync.
     * Useful for manual refresh from UI.
     */
    fun runImmediate() {
        val workRequest =
            OneTimeWorkRequestBuilder<HuaweiHealthKitSyncWorker>().setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.i(TAG, "runImmediate: Immediate sync triggered")
    }
}