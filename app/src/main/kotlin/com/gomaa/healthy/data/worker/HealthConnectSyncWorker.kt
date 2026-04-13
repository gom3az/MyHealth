package com.gomaa.healthy.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toDomainReading
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

            // Perform bidirectional sync with conflict resolution (local wins)
            val syncResult = performBidirectionalSync()

            return when (syncResult) {
                is HealthConnectResult.Success -> Result.success()
                else -> Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Performs bidirectional sync between local database and Health Connect.
     * Local data is the source of truth - wins in conflicts.
     *
     * Sync flow:
     * 1. Upload local changes to Health Connect (writes)
     * 2. Download changes from Health Connect (reads)
     * 3. Apply conflict resolution (local wins)
     */
    private suspend fun performBidirectionalSync(): HealthConnectResult<Unit> {
        return try {
            // Step 1: Upload local changes to Health Connect
            uploadLocalChangesToHC()

            // Step 2: Download changes from Health Connect and apply (local wins)
            downloadAndApplyHCChanges()

            HealthConnectResult.Success(Unit)
        } catch (e: Exception) {
            HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Uploads local-only changes to Health Connect.
     * Only uploads records that originated locally (not from HC).
     */
    private suspend fun uploadLocalChangesToHC() {
        // Upload local steps
        val localSteps = healthConnectRepository.getUnsyncedLocalSteps("myhealth")
        if (localSteps.isNotEmpty()) {
            val stepsResult = healthConnectRepository.writeSteps(
                localSteps.map { it.toDomain() })
            if (stepsResult is HealthConnectResult.Success) {
                val dates = localSteps.map { it.date }
                healthConnectRepository.markStepsAsSynced(dates, SOURCE_HEALTH_CONNECT)
            }
        }

        // Upload local exercise sessions
        val localExerciseSessions = healthConnectRepository.getUnsyncedLocalSessions("myhealth")
        if (localExerciseSessions.isNotEmpty()) {
            val successfulSessionIds = mutableListOf<String>()
            localExerciseSessions.forEach { session ->
                val sessionResult = healthConnectRepository.writeExerciseSession(session.toDomain())
                if (sessionResult is HealthConnectResult.Success) {
                    successfulSessionIds.add(session.id)
                }
            }
            if (successfulSessionIds.isNotEmpty()) {
                healthConnectRepository.markSessionsAsSynced(successfulSessionIds)
            }
        }

        // Upload local heart rate readings
        val localHeartRates = healthConnectRepository.getUnsyncedLocalHeartRates("myhealth")
        if (localHeartRates.isNotEmpty()) {
            val successfulTimestamps = mutableListOf<Long>()
            localHeartRates.forEach { heartRate ->
                val heartRateResult =
                    healthConnectRepository.writeHeartRate(heartRate.toDomainReading())
                if (heartRateResult is HealthConnectResult.Success) {
                    successfulTimestamps.add(heartRate.timestamp)
                }
            }
            if (successfulTimestamps.isNotEmpty()) {
                healthConnectRepository.markHeartRatesAsSynced(
                    successfulTimestamps, SOURCE_HEALTH_CONNECT
                )
            }
        }
    }

    /**
     * Downloads changes from Health Connect and applies them to local database.
     * Implements conflict resolution: local data wins.
     */
    private suspend fun downloadAndApplyHCChanges() {
        // Sync steps from HC (but don't overwrite local)
        val hcStepsResult = healthConnectRepository.syncSteps()
        if (hcStepsResult is HealthConnectResult.Success && hcStepsResult.data > 0) {
            // Steps are already handled by syncSteps which only adds new records
            // Local data wins by default since we don't overwrite existing local records
        }

        // Sync exercise sessions from HC (don't overwrite local)
        val hcExerciseResult = healthConnectRepository.syncExerciseSessions()
        if (hcExerciseResult is HealthConnectResult.Success && hcExerciseResult.data > 0) {
            // Exercise sessions handled by syncExerciseSessions which uses deduplication
            // Local data wins by not overwriting existing local records with same HC ID
        }

        // Sync heart rate from HC (don't overwrite local)
        val hcHeartRateResult = healthConnectRepository.syncHeartRates()
        if (hcHeartRateResult is HealthConnectResult.Success && hcHeartRateResult.data > 0) {
            // Heart rate handled by syncHeartRates which uses deduplication
            // Local data wins by not overwriting existing local records
        }
    }

    companion object {
        const val WORK_NAME = "health_connect_sync_work"
    }
}