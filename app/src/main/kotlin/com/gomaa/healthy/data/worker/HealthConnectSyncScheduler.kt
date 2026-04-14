package com.gomaa.healthy.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val KEY_MASTER_SYNC = "master_sync_enabled"
const val KEY_STEPS_SYNC = "sync_steps_enabled"
const val KEY_EXERCISE_SYNC = "sync_exercise_enabled"
const val KEY_HEART_RATE_SYNC = "sync_heart_rate_enabled"

@Singleton
class HealthConnectSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SYNC_INTERVAL_HOURS = 6L
    }

    /**
     * Schedule periodic sync with intelligent constraints.
     * @param preferences Optional sync preferences; if null, uses defaults (all enabled)
     */
    fun schedulePeriodicSync(
        masterSyncEnabled: Boolean = true,
        syncStepsEnabled: Boolean = true,
        syncExerciseEnabled: Boolean = true,
        syncHeartRateEnabled: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        // Pass sync preferences via inputData
        val inputData = Data.Builder()
            .putBoolean(KEY_MASTER_SYNC, masterSyncEnabled)
            .putBoolean(KEY_STEPS_SYNC, syncStepsEnabled)
            .putBoolean(KEY_EXERCISE_SYNC, syncExerciseEnabled)
            .putBoolean(KEY_HEART_RATE_SYNC, syncHeartRateEnabled)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthConnectSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(HealthConnectSyncWorker.WORK_NAME)
    }

    fun enqueueImmediateSync(
        masterSyncEnabled: Boolean = true,
        syncStepsEnabled: Boolean = true,
        syncExerciseEnabled: Boolean = true,
        syncHeartRateEnabled: Boolean = true
    ): UUID {
        val inputData = Data.Builder()
            .putBoolean(KEY_MASTER_SYNC, masterSyncEnabled)
            .putBoolean(KEY_STEPS_SYNC, syncStepsEnabled)
            .putBoolean(KEY_EXERCISE_SYNC, syncExerciseEnabled)
            .putBoolean(KEY_HEART_RATE_SYNC, syncHeartRateEnabled)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<HealthConnectSyncWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
        return syncRequest.id
    }
}