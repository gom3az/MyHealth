package com.gomaa.healthy.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import com.gomaa.healthy.data.mapper.toDomain
import com.gomaa.healthy.data.mapper.toDomainReadings
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HealthConnectSyncWorker"
        const val WORK_NAME = "health_connect_sync_work"
    }

    override suspend fun doWork(): Result {
        return try {
            // Check for cancellation
            if (!currentCoroutineContext().isActive) {
                Log.d(TAG, "doWork: Worker cancelled")
                return Result.failure()
            }

            // Read sync preferences from inputData
            val masterSyncEnabled = inputData.getBoolean(KEY_MASTER_SYNC, true)
            val syncStepsEnabled = inputData.getBoolean(KEY_STEPS_SYNC, true)
            val syncExerciseEnabled = inputData.getBoolean(KEY_EXERCISE_SYNC, true)
            val syncHeartRateEnabled = inputData.getBoolean(KEY_HEART_RATE_SYNC, true)

            // Check master toggle
            if (!masterSyncEnabled) {
                Log.d(TAG, "doWork: Master sync disabled, skipping sync")
                return Result.success()
            }

            // Check if Health Connect is available and has permissions
            val isAvailableResult = healthConnectRepository.isAvailable()
            val isAvailable =
                isAvailableResult is HealthConnectResult.Success && isAvailableResult.data

            if (!isAvailable) {
                Log.w(TAG, "doWork: Health Connect not available")
                return Result.failure()
            }

            val hasPermsResult = healthConnectRepository.hasPermissions()
            val hasPerms = hasPermsResult is HealthConnectResult.Success && hasPermsResult.data

            if (!hasPerms) {
                Log.w(TAG, "doWork: Health Connect permissions not granted")
                return Result.failure()
            }

            // Perform bidirectional sync with conflict resolution (local wins)
            // Pass per-data-type toggles to control what gets synced
            val syncResult = performBidirectionalSync(
                syncStepsEnabled = syncStepsEnabled,
                syncExerciseEnabled = syncExerciseEnabled,
                syncHeartRateEnabled = syncHeartRateEnabled
            )

            return when (syncResult) {
                is HealthConnectResult.Success -> {
                    Log.d(TAG, "doWork: Sync completed successfully")
                    Result.success()
                }

                else -> {
                    Log.w(TAG, "doWork: Sync failed, requesting retry")
                    Result.retry()
                }
            }
        } catch (_: CancellationException) {
            Log.d(TAG, "doWork: Worker cancelled")
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: Exception during sync", e)
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
    private suspend fun performBidirectionalSync(
        syncStepsEnabled: Boolean, syncExerciseEnabled: Boolean, syncHeartRateEnabled: Boolean
    ): HealthConnectResult<Unit> {
        return try {
            // Step 1: Upload local changes to Health Connect
            val uploadSuccess = uploadLocalChangesToHC(
                syncStepsEnabled = syncStepsEnabled,
                syncExerciseEnabled = syncExerciseEnabled,
                syncHeartRateEnabled = syncHeartRateEnabled
            )

            // Step 2: Download changes from Health Connect and apply (local wins)
            // Always attempt download even if upload failed, to maximize data sync
            downloadAndApplyHCChanges(
                syncStepsEnabled = syncStepsEnabled,
                syncExerciseEnabled = syncExerciseEnabled,
                syncHeartRateEnabled = syncHeartRateEnabled
            )

            if (uploadSuccess) {
                HealthConnectResult.Success(Unit)
            } else {
                HealthConnectResult.Error.Unknown
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "performBidirectionalSync: Sync cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "performBidirectionalSync: Sync failed", e)
            HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Uploads local-only changes to Health Connect.
     * Only uploads records that originated locally (not from HC).
     * Returns true if all uploads succeeded, false otherwise.
     */
    private suspend fun uploadLocalChangesToHC(
        syncStepsEnabled: Boolean, syncExerciseEnabled: Boolean, syncHeartRateEnabled: Boolean
    ): Boolean {
        var allUploadsSucceeded = true

        // Upload local steps
        if (syncStepsEnabled) {
            val localSteps = healthConnectRepository.getUnsyncedLocalSteps(SOURCE_MY_HEALTH)
            if (localSteps.isNotEmpty()) {
                Log.d(TAG, "uploadLocalChangesToHC: Uploading ${localSteps.size} steps records")
                val stepsResult = healthConnectRepository.writeSteps(
                    localSteps.map { it.toDomain() })
                if (stepsResult is HealthConnectResult.Success) {
                    val dates = localSteps.map { it.date }
                    healthConnectRepository.markStepsAsSynced(dates, SOURCE_MY_HEALTH)
                    Log.d(TAG, "uploadLocalChangesToHC: Steps marked as synced")
                } else {
                    Log.w(TAG, "uploadLocalChangesToHC: Steps upload failed")
                    allUploadsSucceeded = false
                }
            } else {
                Log.d(TAG, "uploadLocalChangesToHC: No unsynced steps")
            }
        } else {
            Log.d(TAG, "uploadLocalChangesToHC: Steps sync disabled")
        }

        // Check for cancellation
        if (!currentCoroutineContext().isActive) {
            throw CancellationException("Upload cancelled")
        }

        // Upload local exercise sessions
        if (syncExerciseEnabled) {
            val localExerciseSessions =
                healthConnectRepository.getUnsyncedLocalSessions(SOURCE_MY_HEALTH)
            if (localExerciseSessions.isNotEmpty()) {
                Log.d(
                    TAG,
                    "uploadLocalChangesToHC: Uploading ${localExerciseSessions.size} exercise sessions"
                )
                val successfulSessionIds = mutableListOf<String>()
                localExerciseSessions.forEach { session ->
                    if (!currentCoroutineContext().isActive) {
                        throw CancellationException("Upload cancelled")
                    }
                    val sessionResult =
                        healthConnectRepository.writeExerciseSession(session.toDomain())
                    if (sessionResult is HealthConnectResult.Success) {
                        successfulSessionIds.add(session.id)
                    }
                }
                if (successfulSessionIds.isNotEmpty()) {
                    healthConnectRepository.markSessionsAsSynced(successfulSessionIds)
                    Log.d(
                        TAG,
                        "uploadLocalChangesToHC: ${successfulSessionIds.size} sessions marked as synced"
                    )
                } else {
                    Log.w(TAG, "uploadLocalChangesToHC: No sessions uploaded successfully")
                    allUploadsSucceeded = false
                }
            } else {
                Log.d(TAG, "uploadLocalChangesToHC: No unsynced exercise sessions")
            }
        } else {
            Log.d(TAG, "uploadLocalChangesToHC: Exercise sync disabled")
        }

        // Check for cancellation
        if (!currentCoroutineContext().isActive) {
            throw CancellationException("Upload cancelled")
        }

        // Upload local heart rate readings (batch)
        if (syncHeartRateEnabled) {
            val localHeartRates =
                healthConnectRepository.getUnsyncedLocalHeartRates(SOURCE_MY_HEALTH)
            if (localHeartRates.isNotEmpty()) {
                Log.d(
                    TAG,
                    "uploadLocalChangesToHC: Uploading ${localHeartRates.size} heart rate readings"
                )
                val heartRateResult = healthConnectRepository.writeHeartRates(
                    localHeartRates.flatMap { it.toDomainReadings() })
                if (heartRateResult is HealthConnectResult.Success && heartRateResult.data > 0) {
                    // Mark only successfully synced records
                    val syncedTimestamps = localHeartRates.map { it.dayTimestamp }
                    healthConnectRepository.markHeartRatesAsSynced(
                        syncedTimestamps, SOURCE_MY_HEALTH
                    )
                    Log.d(TAG, "uploadLocalChangesToHC: Heart rates marked as synced")
                } else {
                    Log.w(TAG, "uploadLocalChangesToHC: Heart rate upload failed")
                    allUploadsSucceeded = false
                }
            } else {
                Log.d(TAG, "uploadLocalChangesToHC: No unsynced heart rate readings")
            }
        } else {
            Log.d(TAG, "uploadLocalChangesToHC: Heart rate sync disabled")
        }

        return allUploadsSucceeded
    }

    /**
     * Downloads changes from Health Connect and applies them to local database.
     * Implements conflict resolution: local data wins.
     */
    private suspend fun downloadAndApplyHCChanges(
        syncStepsEnabled: Boolean, syncExerciseEnabled: Boolean, syncHeartRateEnabled: Boolean
    ) {
        // Check for cancellation
        if (!currentCoroutineContext().isActive) {
            throw CancellationException("Download cancelled")
        }

        // Sync steps from HC (but don't overwrite local)
        if (syncStepsEnabled) {
            Log.d(TAG, "downloadAndApplyHCChanges: Syncing steps from Health Connect")
            val hcStepsResult = healthConnectRepository.syncSteps()
            if (hcStepsResult is HealthConnectResult.Success) {
                Log.d(
                    TAG,
                    "downloadAndApplyHCChanges: Synced ${hcStepsResult.data} steps records from HC"
                )
            } else {
                Log.w(TAG, "downloadAndApplyHCChanges: Steps sync failed")
            }
        } else {
            Log.d(TAG, "downloadAndApplyHCChanges: Steps sync disabled")
        }

        // Check for cancellation
        if (!currentCoroutineContext().isActive) {
            throw CancellationException("Download cancelled")
        }

        // Sync exercise sessions from HC (don't overwrite local)
        if (syncExerciseEnabled) {
            Log.d(TAG, "downloadAndApplyHCChanges: Syncing exercise sessions from Health Connect")
            val hcExerciseResult = healthConnectRepository.syncExerciseSessions()
            if (hcExerciseResult is HealthConnectResult.Success) {
                Log.d(
                    TAG,
                    "downloadAndApplyHCChanges: Synced ${hcExerciseResult.data} exercise sessions from HC"
                )
            } else {
                Log.w(TAG, "downloadAndApplyHCChanges: Exercise sync failed")
            }
        } else {
            Log.d(TAG, "downloadAndApplyHCChanges: Exercise sync disabled")
        }

        // Check for cancellation
        if (!currentCoroutineContext().isActive) {
            throw CancellationException("Download cancelled")
        }

        // Sync heart rate from HC (don't overwrite local)
        if (syncHeartRateEnabled) {
            Log.d(TAG, "downloadAndApplyHCChanges: Syncing heart rate from Health Connect")
            val hcHeartRateResult = healthConnectRepository.syncHeartRates()
            if (hcHeartRateResult is HealthConnectResult.Success) {
                Log.d(
                    TAG,
                    "downloadAndApplyHCChanges: Synced ${hcHeartRateResult.data} heart rate records from HC"
                )
            } else {
                Log.w(TAG, "downloadAndApplyHCChanges: Heart rate sync failed")
            }
        } else {
            Log.d(TAG, "downloadAndApplyHCChanges: Heart rate sync disabled")
        }
    }
}