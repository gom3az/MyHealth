package com.gomaa.healthy.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check if Health Connect is available and has permissions
            val isAvailableResult = healthConnectRepository.isAvailable()
            val isAvailable =
                isAvailableResult is HealthConnectResult.Success && isAvailableResult.data

            if (!isAvailable) {
                return Result.failure()
            }

            val hasPermsResult = healthConnectRepository.hasPermissions()
            val hasPerms = hasPermsResult is HealthConnectResult.Success && hasPermsResult.data

            if (!hasPerms) {
                return Result.failure()
            }

            // Sync steps
            val stepsResult = healthConnectRepository.syncSteps()
            val stepsSuccess = stepsResult is HealthConnectResult.Success

            // Sync exercise sessions
            val exerciseResult = healthConnectRepository.syncExerciseSessions()
            val exerciseSuccess = exerciseResult is HealthConnectResult.Success

            // Return success if at least one sync succeeded
            if (stepsSuccess || exerciseSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "health_connect_sync_work"
    }
}