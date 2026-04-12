package com.gomaa.healthy

import android.app.Application
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthApplication : Application() {

    @Inject
    lateinit var syncScheduler: HealthConnectSyncScheduler

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic Health Connect sync
        syncScheduler.schedulePeriodicSync()
    }
}