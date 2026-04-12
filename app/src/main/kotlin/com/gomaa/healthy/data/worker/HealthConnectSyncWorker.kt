package com.gomaa.healthy.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomaa.healthy.data.repository.HealthConnectRepository
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
            if (!healthConnectRepository.isAvailable()) {
                return Result.failure()
            }

            if (!healthConnectRepository.hasPermissions()) {
                return Result.failure()
            }

            // Sync steps
            val stepsResult = healthConnectRepository.syncSteps()
            val stepsSuccess = stepsResult.isSuccess

            // Sync exercise sessions
            val exerciseResult = healthConnectRepository.syncExerciseSessions()
            val exerciseSuccess = exerciseResult.isSuccess

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