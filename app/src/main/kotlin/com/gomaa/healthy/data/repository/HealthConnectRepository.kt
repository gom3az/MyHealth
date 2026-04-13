package com.gomaa.healthy.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gomaa.healthy.data.local.dao.HealthConnectExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HealthConnectStepsDao
import com.gomaa.healthy.data.local.entity.HealthConnectExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HealthConnectStepEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sealed class hierarchy for HealthConnect operation results.
 * Provides specific error types instead of generic Result.
 */
sealed class HealthConnectResult<out T> {
    data class Success<T>(val data: T) : HealthConnectResult<T>()

    sealed class Error(val exception: Throwable) : HealthConnectResult<Nothing>() {
        /** Health Connect app is not installed or not available */
        object NotAvailable : Error(Exception("Health Connect is not available"))

        /** Permission denied or not granted */
        object PermissionDenied : Error(Exception("Health Connect permission denied"))

        /** Network-related errors */
        object NetworkError : Error(Exception("Network error occurred"))

        /** Unknown/unexpected errors */
        object Unknown : Error(Exception("Unknown error occurred"))
    }
}

/**
 * Interface for HealthConnect repository operations.
 * Provides abstraction for dependency injection and testing.
 */
interface HealthConnectRepositoryInterface {
    suspend fun isAvailable(): HealthConnectResult<Boolean>
    suspend fun hasPermissions(): HealthConnectResult<Boolean>
    suspend fun syncSteps(): HealthConnectResult<Int>
    suspend fun syncExerciseSessions(): HealthConnectResult<Int>
    suspend fun getStepCount(): Int
    suspend fun getExerciseSessionCount(): Int
}

/**
 * Enum representing different exercise types from Health Connect.
 */
enum class ExerciseType(val displayName: String) {
    EXERCISE("Exercise"),
    RUNNING("Running"),
    CYCLING("Cycling"),
    SWIMMING("Swimming"),
    WALKING("Walking"),
    HIKING("Hiking"),
    YOGA("Yoga"),
    SPORTS("Sports"),
    OTHER("Other")
}

/** Milliseconds per minute for duration calculations */
private const val MILLIS_PER_MINUTE = 60_000L

/**
 * Data class for sync operation result containing count and latest record timestamp.
 */
data class SyncResult(val newRecordsCount: Int, val latestRecordTime: Long?)

/**
 * Pure function to map StepsRecord to HealthConnectStepEntity.
 * Extracted from repository for single responsibility.
 */
fun mapStepsRecordToEntity(record: StepsRecord): HealthConnectStepEntity {
    return HealthConnectStepEntity(
        count = record.count.toInt(),
        startTime = record.startTime.toEpochMilli(),
        endTime = record.endTime.toEpochMilli(),
        healthConnectRecordId = record.metadata.id
    )
}

/**
 * Pure function to map ExerciseSessionRecord to HealthConnectExerciseSessionEntity.
 * Extracted from repository for single responsibility.
 */
fun mapExerciseSessionToEntity(record: ExerciseSessionRecord): HealthConnectExerciseSessionEntity {
    val start = record.startTime.toEpochMilli()
    val end = record.endTime.toEpochMilli()
    val exerciseTypeName = mapExerciseSessionType(record.exerciseType)

    return HealthConnectExerciseSessionEntity(
        startTime = start,
        endTime = end,
        exerciseType = exerciseTypeName,
        durationMinutes = ((end - start) / MILLIS_PER_MINUTE).toInt(),
        caloriesBurned = null,
        healthConnectRecordId = record.metadata.id
    )
}

/**
 * Maps Health Connect exercise type to our ExerciseType enum.
 */
private fun mapExerciseSessionType(hcType: Int): String {
    return when (hcType) {
        1 -> ExerciseType.RUNNING.displayName
        2 -> ExerciseType.CYCLING.displayName
        3 -> ExerciseType.SWIMMING.displayName
        4 -> ExerciseType.WALKING.displayName
        5 -> ExerciseType.HIKING.displayName
        6 -> ExerciseType.YOGA.displayName
        7 -> ExerciseType.SPORTS.displayName
        else -> ExerciseType.OTHER.displayName
    }
}

@Singleton
class HealthConnectRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val healthConnectStepsDao: HealthConnectStepsDao,
    private val healthConnectExerciseSessionDao: HealthConnectExerciseSessionDao
) : HealthConnectRepositoryInterface {

    companion object {
        private val READ_STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        private val WRITE_STEPS = HealthPermission.getWritePermission(StepsRecord::class)
        private val READ_EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val WRITE_EXERCISE =
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        val PERMISSIONS = setOf(READ_STEPS, WRITE_STEPS, READ_EXERCISE, WRITE_EXERCISE)

        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        /** SharedPreferences key for last sync timestamp */
        private const val PREFS_NAME = "health_connect_sync"
        private const val KEY_LAST_STEPS_SYNC = "last_steps_sync"
        private const val KEY_LAST_EXERCISE_SYNC = "last_exercise_sync"

        /** Retry configuration */
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 5000L
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if Health Connect is available on this device.
     * @return HealthConnectResult with Boolean indicating availability
     */
    override suspend fun isAvailable(): HealthConnectResult<Boolean> {
        try {
            withContext(Dispatchers.IO) {
                context.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, 0)
            }
            val sdkStatus = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
            return executeWithRetry {
                if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                    HealthConnectResult.Success(true)
                } else {
                    HealthConnectResult.Error.NotAvailable
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return HealthConnectResult.Error.NotAvailable
        } catch (_: SecurityException) {
            return HealthConnectResult.Error.PermissionDenied
        } catch (_: IOException) {
            return HealthConnectResult.Error.NetworkError
        } catch (_: Exception) {
            return HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Checks if all required permissions are granted.
     * @return HealthConnectResult with Boolean indicating permission status
     */
    override suspend fun hasPermissions(): HealthConnectResult<Boolean> {
        return try {
            executeWithRetry {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val allGranted = PERMISSIONS.all { granted.contains(it) }
                HealthConnectResult.Success(allGranted)
            }
        } catch (_: SecurityException) {
            HealthConnectResult.Error.PermissionDenied
        } catch (_: IOException) {
            HealthConnectResult.Error.NetworkError
        } catch (_: Exception) {
            HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Syncs steps data from Health Connect to local database with pagination support.
     * Only fetches records newer than last sync time.
     * @return HealthConnectResult with number of new records synced
     */
    override suspend fun syncSteps(): HealthConnectResult<Int> {
        try {
            val lastSyncTime = getLastStepsSyncTime()
            val result = syncStepsWithPagination(lastSyncTime)

            // Save last sync time
            if (result.newRecordsCount > 0) {
                saveLastStepsSyncTime(result.latestRecordTime ?: System.currentTimeMillis())
            }

            return HealthConnectResult.Success(result.newRecordsCount)
        } catch (_: PackageManager.NameNotFoundException) {
            return HealthConnectResult.Error.NotAvailable
        } catch (_: SecurityException) {
            return HealthConnectResult.Error.PermissionDenied
        } catch (_: IOException) {
            return HealthConnectResult.Error.NetworkError
        } catch (_: Exception) {
            return HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Syncs exercise sessions from Health Connect to local database with pagination support.
     * Only fetches records newer than last sync time.
     * @return HealthConnectResult with number of new sessions synced
     */
    override suspend fun syncExerciseSessions(): HealthConnectResult<Int> {
        try {
            val lastSyncTime = getLastExerciseSyncTime()
            val result = syncExerciseWithPagination(lastSyncTime)

            // Save last sync time
            if (result.newRecordsCount > 0) {
                saveLastExerciseSyncTime(result.latestRecordTime ?: System.currentTimeMillis())
            }

            return HealthConnectResult.Success(result.newRecordsCount)
        } catch (_: PackageManager.NameNotFoundException) {
            return HealthConnectResult.Error.NotAvailable
        } catch (_: SecurityException) {
            return HealthConnectResult.Error.PermissionDenied
        } catch (_: IOException) {
            return HealthConnectResult.Error.NetworkError
        } catch (_: Exception) {
            return HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Supports cancellation via coroutine's isActive check.
     */
    private suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var currentDelay = INITIAL_RETRY_DELAY_MS

        repeat(maxAttempts) { attempt ->
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Operation cancelled")
            }

            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                // Don't retry on certain exceptions
                when (e) {
                    is SecurityException,
                    is PackageManager.NameNotFoundException -> throw e
                }

                // Exponential backoff, capped at max delay
                if (attempt < maxAttempts - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                }
            }
        }

        throw lastException ?: Exception("Retry exhausted")
    }

    /**
     * Syncs steps with pagination support using generic approach.
     * Shares common pagination logic with exercise sessions.
     */
    private suspend fun syncStepsWithPagination(lastSyncTime: Long?): SyncResult {
        val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH

        var totalNewRecords = 0
        var latestRecordTime: Long? = null
        var pageToken: String? = null

        do {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Sync operation cancelled")
            }

            // Read page of steps records with retry
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime),
                pageToken = pageToken
            )

            val response = executeWithRetry {
                healthConnectClient.readRecords(request)
            }
            val records = response.records

            // Filter duplicates using null-safe check
            val newRecords = records.filter { record ->
                // getByRecordId returns nullable type, use explicit == null check as required by Room
                healthConnectStepsDao.getByRecordId(record.metadata.id) == null
            }

            // Map to entities
            val entities = newRecords.map { mapStepsRecordToEntity(it) }

            // Insert and track
            if (entities.isNotEmpty()) {
                healthConnectStepsDao.insertAll(entities)
                totalNewRecords += entities.size

                val maxEndTime = newRecords.maxOfOrNull { it.endTime.toEpochMilli() }
                if (maxEndTime != null && (latestRecordTime == null || maxEndTime > latestRecordTime)) {
                    latestRecordTime = maxEndTime
                }
            }

            pageToken = response.pageToken
        } while (pageToken != null)

        return SyncResult(totalNewRecords, latestRecordTime)
    }

    /**
     * Syncs exercise sessions with pagination support using generic approach.
     * Shares common pagination logic with steps.
     */
    private suspend fun syncExerciseWithPagination(lastSyncTime: Long?): SyncResult {
        val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH

        var totalNewRecords = 0
        var latestRecordTime: Long? = null
        var pageToken: String? = null

        do {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Sync operation cancelled")
            }

            // Read page of exercise session records with retry
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime),
                pageToken = pageToken
            )

            val response = executeWithRetry {
                healthConnectClient.readRecords(request)
            }
            val records = response.records

            // Filter duplicates
            val newRecords = records.filter { record ->
                healthConnectExerciseSessionDao.getByRecordId(record.metadata.id) == null
            }

            // Map to entities
            val entities = newRecords.map { mapExerciseSessionToEntity(it) }

            // Insert and track
            if (entities.isNotEmpty()) {
                healthConnectExerciseSessionDao.insertAll(entities)
                totalNewRecords += entities.size

                val maxEndTime = newRecords.maxOfOrNull { it.endTime.toEpochMilli() }
                if (maxEndTime != null && (latestRecordTime == null || maxEndTime > latestRecordTime)) {
                    latestRecordTime = maxEndTime
                }
            }

            pageToken = response.pageToken
        } while (pageToken != null)

        return SyncResult(totalNewRecords, latestRecordTime)
    }

    private fun getLastStepsSyncTime(): Long? {
        val time = sharedPreferences.getLong(KEY_LAST_STEPS_SYNC, -1L)
        return if (time == -1L) null else time
    }

    private fun saveLastStepsSyncTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_STEPS_SYNC, time) }
    }

    private fun getLastExerciseSyncTime(): Long? {
        val time = sharedPreferences.getLong(KEY_LAST_EXERCISE_SYNC, -1L)
        return if (time == -1L) null else time
    }

    private fun saveLastExerciseSyncTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_EXERCISE_SYNC, time) }
    }

    override suspend fun getStepCount(): Int {
        return healthConnectStepsDao.getStepCount()
    }

    override suspend fun getExerciseSessionCount(): Int {
        return healthConnectExerciseSessionDao.getSessionCount()
    }

}