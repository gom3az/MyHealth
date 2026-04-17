package com.gomaa.healthy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.gomaa.healthy.data.preferences.AppPreferencesManager
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import com.gomaa.healthy.logging.CrashExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HealthApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncScheduler: HealthConnectSyncScheduler

    @Inject
    lateinit var syncPreferencesManager: SyncPreferencesManager

    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashExceptionHandler: CrashExceptionHandler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
        crashExceptionHandler.initialize()
        initializeFirstRun()
        scheduleSyncWithPreferences()
        WorkManager.initialize(this, workManagerConfiguration)
    }

    private fun initializeFirstRun() {
        applicationScope.launch {
            val prefs = appPreferencesManager.getPreferences()
            if (prefs.isFirstRun) {
                appPreferencesManager.setFirstRunComplete()
            }
        }
    }

    private fun scheduleSyncWithPreferences() {
        applicationScope.launch {
            val prefs = syncPreferencesManager.getPreferences()
            syncScheduler.schedulePeriodicSync(
                masterSyncEnabled = prefs.masterSyncEnabled,
                syncStepsEnabled = prefs.syncStepsEnabled,
                syncExerciseEnabled = prefs.syncExerciseEnabled,
                syncHeartRateEnabled = prefs.syncHeartRateEnabled
            )
        }
    }
}