package com.gomaa.healthy

import android.app.Application
import com.gomaa.healthy.data.preferences.AppPreferencesManager
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HealthApplication : Application() {

    @Inject
    lateinit var syncScheduler: HealthConnectSyncScheduler

    @Inject
    lateinit var syncPreferencesManager: SyncPreferencesManager

    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeFirstRun()
        scheduleSyncWithPreferences()
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