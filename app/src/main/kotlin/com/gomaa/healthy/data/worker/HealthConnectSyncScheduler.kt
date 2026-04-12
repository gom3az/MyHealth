package com.gomaa.healthy.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SYNC_INTERVAL_HOURS = 6L
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthConnectSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(HealthConnectSyncWorker.WORK_NAME)
    }
}