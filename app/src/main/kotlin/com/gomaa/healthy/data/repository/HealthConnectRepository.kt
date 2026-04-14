package com.gomaa.healthy.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import com.gomaa.healthy.data.mapper.mapExerciseSessionRecordToEntity
import com.gomaa.healthy.data.mapper.mapHeartRateRecordToEntity
import com.gomaa.healthy.data.mapper.mapStepsRecordToEntity
import com.gomaa.healthy.data.sync.ConflictResolver
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
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
    suspend fun syncHeartRates(): HealthConnectResult<Int>
    suspend fun getStepCount(): Int
    suspend fun getExerciseSessionCount(): Int
    suspend fun getHeartRateCount(): Int

    // Write operations for source-of-truth refactor
    suspend fun writeSteps(dailySteps: List<DailySteps>): HealthConnectResult<Int>

    suspend fun writeExerciseSession(session: ExerciseSession): HealthConnectResult<Boolean>
    suspend fun writeHeartRates(heartRates: List<HeartRateReading>): HealthConnectResult<Int>

    // DAO-accessor methods for Worker (replaces direct DAO access)
    suspend fun getUnsyncedLocalSteps(source: String): List<DailyStepsEntity>
    suspend fun markStepsAsSynced(dates: List<Long>, source: String)
    suspend fun getUnsyncedLocalSessions(source: String): List<ExerciseSessionEntity>
    suspend fun markSessionsAsSynced(ids: List<String>)
    suspend fun getUnsyncedLocalHeartRates(source: String): List<HeartRateEntity>
    suspend fun markHeartRatesAsSynced(timestamps: List<Long>, source: String)
}

data class SyncResult(val newRecordsCount: Int, val latestRecordTime: Long?)

@Singleton
class HealthConnectRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dailyStepsDao: DailyStepsDao,
    private val heartRateDao: HeartRateDao,
    private val exerciseSessionDao: ExerciseSessionDao
) : HealthConnectRepositoryInterface {

    companion object {
        private const val TAG = "HealthConnectRepository"
        private val READ_STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        private val WRITE_STEPS = HealthPermission.getWritePermission(StepsRecord::class)
        private val READ_EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val WRITE_EXERCISE =
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        private val READ_HEART_RATE = HealthPermission.getReadPermission(HeartRateRecord::class)
        val PERMISSIONS =
            setOf(READ_STEPS, WRITE_STEPS, READ_EXERCISE, WRITE_EXERCISE, READ_HEART_RATE)

        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        /** SharedPreferences key for last sync timestamp */
        private const val PREFS_NAME = "health_connect_sync"
        private const val KEY_LAST_STEPS_SYNC = "last_steps_sync"
        private const val KEY_LAST_EXERCISE_SYNC = "last_exercise_sync"
        private const val KEY_LAST_HEART_RATE_SYNC = "last_heart_rate_sync"

        /** SharedPreferences key for last upload timestamp (local -> HC) */
        private const val KEY_LAST_STEPS_UPLOAD = "last_steps_upload"
        private const val KEY_LAST_EXERCISE_UPLOAD = "last_exercise_upload"
        private const val KEY_LAST_HEART_RATE_UPLOAD = "last_heart_rate_upload"

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

            // Save last sync time regardless of whether new records were found
            saveLastStepsSyncTime(result.latestRecordTime ?: System.currentTimeMillis())

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
        // First check permissions
        val permResult = hasPermissions()
        if (permResult !is HealthConnectResult.Success || !permResult.data) {
            return when (permResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.PermissionDenied
                else -> permResult as HealthConnectResult.Error
            }
        }

        try {
            val lastSyncTime = getLastExerciseSyncTime()
            val result = syncExerciseSessionsWithPagination(lastSyncTime)

            // Save last sync time regardless of whether new records were found
            saveLastExerciseSyncTime(result.latestRecordTime ?: System.currentTimeMillis())

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
     * Syncs exercise sessions with pagination support.
     */
    private suspend fun syncExerciseSessionsWithPagination(lastSyncTime: Long?): SyncResult {
        val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it) } ?: Instant.EPOCH

        var totalNewRecords = 0
        var latestRecordTime: Long? = null
        var pageToken: String? = null

        do {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Sync operation cancelled")
            }

            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime),
                pageToken = pageToken
            )

            val response = executeWithRetry {
                healthConnectClient.readRecords(request)
            }
            val records = response.records

            // Convert to entities with conflict resolution
            val entities = records.mapNotNull { record ->
                val recordId = record.metadata.id
                val entity = mapExerciseSessionRecordToEntity(record, recordId)

                // Check for existing HC record (deduplication)
                val existingByHcId = exerciseSessionDao.getByHealthConnectRecordId(
                    SOURCE_HEALTH_CONNECT, recordId
                )
                // Check for local MY_HEALTH session at same time range (conflict)
                val existingLocal = exerciseSessionDao.getBySourceAndTimeRange(
                    SOURCE_MY_HEALTH, entity.startTime, entity.endTime
                )

                val shouldApply = ConflictResolver.shouldApplyExercise(
                    hcRecordId = recordId,
                    existingByHcId = existingByHcId,
                    existingLocal = existingLocal
                )
                if (!shouldApply) {
                    Log.d(
                        TAG,
                        "syncExerciseSessions: Skipping HC session - conflict (recordId=$recordId)"
                    )
                    null
                } else {
                    entity
                }
            }

            if (entities.isNotEmpty()) {
                entities.forEach { exerciseSessionDao.insert(it) }
                totalNewRecords += entities.size

                val maxEndTime = records.maxOfOrNull { it.endTime.toEpochMilli() }
                if (maxEndTime != null && (latestRecordTime == null || maxEndTime > latestRecordTime)) {
                    latestRecordTime = maxEndTime
                }
            }

            pageToken = response.pageToken
        } while (pageToken != null)

        return SyncResult(totalNewRecords, latestRecordTime)
    }

    /**
     * Syncs heart rate data from Health Connect to local database with pagination support.
     * Only fetches records newer than last sync time.
     * HC-065: Check hasPermissions() before sync
     * @return HealthConnectResult with number of new heart rate records synced
     */
    override suspend fun syncHeartRates(): HealthConnectResult<Int> {
        // First check permissions
        val permResult = hasPermissions()
        if (permResult !is HealthConnectResult.Success || !permResult.data) {
            return when (permResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.PermissionDenied
                else -> permResult as HealthConnectResult.Error
            }
        }

        try {
            val lastSyncTime = getLastHeartRateSyncTime()
            val result = syncHeartRatesWithPagination(lastSyncTime)

            // Save last sync time regardless of whether new records were found
            saveLastHeartRateSyncTime(result.latestRecordTime ?: System.currentTimeMillis())

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
        maxAttempts: Int = MAX_RETRY_ATTEMPTS, operation: suspend () -> T
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
                    is SecurityException, is PackageManager.NameNotFoundException -> throw e
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
     * Syncs steps with pagination support using unified DailyStepsEntity.
     * Groups steps by date and aggregates.
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

            // Map each record to entity, then aggregate by date
            val entities = records.map { record ->
                val date = Instant.ofEpochMilli(record.startTime.toEpochMilli())
                    .atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                mapStepsRecordToEntity(record, date)
            }.groupBy { it.date }.map { (date, entitiesForDate) ->
                val totalSteps = entitiesForDate.sumOf { it.totalSteps }
                entitiesForDate.first().copy(
                    date = date, totalSteps = totalSteps
                )
            }

            // Apply conflict resolution: local data wins
            val filteredEntities = entities.filter { hcEntity ->
                val localEntity = dailyStepsDao.getByDate(hcEntity.date)
                ConflictResolver.shouldApplySteps(localEntity)
            }

            // Insert aggregated steps (only if no local conflict)
            if (filteredEntities.isNotEmpty()) {
                dailyStepsDao.insertAll(filteredEntities)
                totalNewRecords += filteredEntities.size

                val maxEndTime = records.maxOfOrNull { it.endTime.toEpochMilli() }
                if (maxEndTime != null && (latestRecordTime == null || maxEndTime > latestRecordTime)) {
                    latestRecordTime = maxEndTime
                }
            }

            pageToken = response.pageToken
        } while (pageToken != null)

        return SyncResult(totalNewRecords, latestRecordTime)
    }

    /**
     * Syncs heart rates with pagination support using unified HeartRateEntity.
     * HC-059: Query ALL existing record IDs for proper deduplication
     * HC-066: Use UUID.randomUUID() for recordId to prevent collisions
     */
    private suspend fun syncHeartRatesWithPagination(lastSyncTime: Long?): SyncResult {
        // Default to last 30 days on first sync to avoid importing massive historical data
        val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
            .minus(30, java.time.temporal.ChronoUnit.DAYS)

        var totalNewRecords = 0
        var latestRecordTime: Long? = null
        var pageToken: String? = null

        // Query all existing record IDs for deduplication
        val existingRecordIds = heartRateDao.getAllRecordIdsBySource(SOURCE_HEALTH_CONNECT).toSet()

        do {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Sync operation cancelled")
            }

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.after(startTime),
                pageToken = pageToken
            )

            val response = executeWithRetry {
                healthConnectClient.readRecords(request)
            }
            val records = response.records

            // Convert to entities with UUID record ID for deduplication
            // HC-066: Use UUID instead of timestamp to prevent collisions
            val entities = records.flatMap { record ->
                val recordId = UUID.randomUUID().toString()
                mapHeartRateRecordToEntity(record, recordId, SOURCE_HEALTH_CONNECT)
            }

            // Filter out duplicates AND apply conflict resolution
            val newEntities = entities.filter { hcEntity ->
                // Conflict check: no local MY_HEALTH reading at same timestamp
                val existingLocal = heartRateDao.getBySourceAndTimestamp(
                    SOURCE_MY_HEALTH, hcEntity.timestamp
                )
                ConflictResolver.shouldApplyHeartRate(
                    hcRecordId = hcEntity.healthConnectRecordId ?: "",
                    existingRecordIds = existingRecordIds,
                    existingLocal = existingLocal
                )
            }

            // Insert new records (with IGNORE strategy - no overwrite)
            if (newEntities.isNotEmpty()) {
                heartRateDao.insertAll(newEntities)
                totalNewRecords += newEntities.size

                val maxMeasurementTime =
                    records.flatMap { it.samples }.maxOfOrNull { it.time.toEpochMilli() }
                if (maxMeasurementTime != null && (latestRecordTime == null || maxMeasurementTime > latestRecordTime)) {
                    latestRecordTime = maxMeasurementTime
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

    private fun getLastHeartRateSyncTime(): Long? {
        val time = sharedPreferences.getLong(KEY_LAST_HEART_RATE_SYNC, -1L)
        return if (time == -1L) null else time
    }

    private fun saveLastHeartRateSyncTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_HEART_RATE_SYNC, time) }
    }

    private fun getLastExerciseSyncTime(): Long? {
        val time = sharedPreferences.getLong(KEY_LAST_EXERCISE_SYNC, -1L)
        return if (time == -1L) null else time
    }

    private fun saveLastExerciseSyncTime(time: Long) {
        sharedPreferences.edit { putLong(KEY_LAST_EXERCISE_SYNC, time) }
    }

    fun getLastSyncTime(): Long? {
        val stepsTime = getLastStepsSyncTime()
        val exerciseTime = getLastExerciseSyncTime()
        val heartRateTime = getLastHeartRateSyncTime()
        return listOfNotNull(stepsTime, exerciseTime, heartRateTime).maxOrNull()
    }

    override suspend fun getStepCount(): Int {
        val steps = dailyStepsDao.getByDateAndSource(
            LocalDate.now().toEpochDay(), SOURCE_HEALTH_CONNECT
        )
        return steps?.totalSteps ?: 0
    }

    override suspend fun getExerciseSessionCount(): Int {
        return exerciseSessionDao.getBySource(SOURCE_HEALTH_CONNECT).size
    }

    override suspend fun getHeartRateCount(): Int {
        // Get all readings, not just latest
        val allReadings = heartRateDao.getAllBySource(SOURCE_HEALTH_CONNECT)
        return allReadings.size
    }

    /**
     * Write steps data to Health Connect.
     * Converts DailySteps domain objects to StepsRecord and writes to HC.
     * @param dailySteps List of DailySteps to write
     * @return HealthConnectResult with count of successfully written records
     */
    override suspend fun writeSteps(dailySteps: List<DailySteps>): HealthConnectResult<Int> {
        // Validate data
        val validSteps = dailySteps.filter { it.totalSteps >= 0 }
        if (validSteps.isEmpty()) {
            Log.w(TAG, "writeSteps: No valid steps records to write")
            return HealthConnectResult.Success(0)
        }

        // Check availability and permissions first
        val availabilityResult = isAvailable()
        if (availabilityResult !is HealthConnectResult.Success || !availabilityResult.data) {
            return when (availabilityResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.NotAvailable
                else -> availabilityResult as HealthConnectResult.Error
            }
        }

        val permissionsResult = hasPermissions()
        if (permissionsResult !is HealthConnectResult.Success || !permissionsResult.data) {
            return when (permissionsResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.PermissionDenied
                else -> permissionsResult as HealthConnectResult.Error
            }
        }

        return try {
            withContext(Dispatchers.IO) {
                val stepsRecords = validSteps.map { steps ->
                    val startInstant = Instant.ofEpochMilli(
                        steps.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    val endInstant = Instant.ofEpochMilli(
                        (steps.date.plusDays(1)).atStartOfDay(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli()
                    )
                    StepsRecord(
                        startTime = startInstant,
                        startZoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant),
                        endTime = endInstant,
                        endZoneOffset = ZoneId.systemDefault().rules.getOffset(endInstant),
                        count = steps.totalSteps.toLong(),
                        metadata = Metadata.manualEntry(),
                    )
                }

                Log.d(TAG, "writeSteps: Writing ${stepsRecords.size} steps records")
                // Batch insert to Health Connect
                healthConnectClient.insertRecords(stepsRecords)
                val insertedCount = stepsRecords.size
                Log.d(TAG, "writeSteps: Successfully wrote $insertedCount records")

                HealthConnectResult.Success(insertedCount)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "writeSteps: Permission denied", e)
            HealthConnectResult.Error.PermissionDenied
        } catch (e: IOException) {
            Log.e(TAG, "writeSteps: Network error", e)
            HealthConnectResult.Error.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "writeSteps: Unknown error", e)
            HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Write exercise session data to Health Connect.
     * Converts ExerciseSession domain object to ExerciseSessionRecord and writes to HC.
     * @param session ExerciseSession to write
     * @return HealthConnectResult indicating success
     */
    override suspend fun writeExerciseSession(session: ExerciseSession): HealthConnectResult<Boolean> {
        // Validate data
        if (session.startTime >= session.endTime) {
            Log.w(
                TAG,
                "writeExerciseSession: Invalid time range - start=${session.startTime}, end=${session.endTime}"
            )
            return HealthConnectResult.Error.Unknown
        }

        // Check availability and permissions first
        val availabilityResult = isAvailable()
        if (availabilityResult !is HealthConnectResult.Success || !availabilityResult.data) {
            return when (availabilityResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.NotAvailable
                else -> availabilityResult as HealthConnectResult.Error
            }
        }

        val permissionsResult = hasPermissions()
        if (permissionsResult !is HealthConnectResult.Success || !permissionsResult.data) {
            return when (permissionsResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.PermissionDenied
                else -> permissionsResult as HealthConnectResult.Error
            }
        }

        return try {
            withContext(Dispatchers.IO) {
                val startInstant = Instant.ofEpochMilli(session.startTime)
                val endInstant = Instant.ofEpochMilli(session.endTime)

                val exerciseType = if (session.exerciseType != 0) {
                    session.exerciseType
                } else {
                    ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
                }

                val record = ExerciseSessionRecord(
                    metadata = Metadata.manualEntry(),
                    startTime = startInstant,
                    startZoneOffset = ZoneOffset.of(ZoneOffset.systemDefault().id),
                    endTime = endInstant,
                    endZoneOffset = ZoneOffset.of(ZoneOffset.systemDefault().id),
                    exerciseType = exerciseType,
                    title = session.title.takeIf { it.isNotBlank() })
                Log.d(
                    TAG,
                    "writeExerciseSession: Writing session '${record.title}' type=$exerciseType"
                )
                healthConnectClient.insertRecords(listOf(record))
                HealthConnectResult.Success(true)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "writeExerciseSession: Permission denied", e)
            HealthConnectResult.Error.PermissionDenied
        } catch (e: IOException) {
            Log.e(TAG, "writeExerciseSession: Network error", e)
            HealthConnectResult.Error.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "writeExerciseSession: Unknown error", e)
            HealthConnectResult.Error.Unknown
        }
    }

    /**
     * Write heart rate data to Health Connect.
     * Converts HeartRateReading domain objects to HeartRateRecord and writes to HC in batches.
     * @param heartRates List of HeartRateReading to write
     * @return HealthConnectResult with count of successfully written records
     */
    override suspend fun writeHeartRates(heartRates: List<HeartRateReading>): HealthConnectResult<Int> {
        // Validate data: BPM must be in realistic range (30-220)
        val validHeartRates = heartRates.filter { it.bpm in 30..220 }
        if (validHeartRates.isEmpty()) {
            Log.w(TAG, "writeHeartRates: No valid heart rate records to write")
            return HealthConnectResult.Success(0)
        }

        // Check availability and permissions first
        val availabilityResult = isAvailable()
        if (availabilityResult !is HealthConnectResult.Success || !availabilityResult.data) {
            return when (availabilityResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.NotAvailable
                else -> availabilityResult as HealthConnectResult.Error
            }
        }

        val permissionsResult = hasPermissions()
        if (permissionsResult !is HealthConnectResult.Success || !permissionsResult.data) {
            return when (permissionsResult) {
                is HealthConnectResult.Success -> HealthConnectResult.Error.PermissionDenied
                else -> permissionsResult as HealthConnectResult.Error
            }
        }

        return try {
            withContext(Dispatchers.IO) {
                // Group by timestamp to batch samples within same time window
                val records = validHeartRates.map { heartRate ->
                    val timeInstant = Instant.ofEpochMilli(heartRate.timestamp)
                    HeartRateRecord(
                        startTime = timeInstant,
                        startZoneOffset = ZoneId.systemDefault().rules.getOffset(timeInstant),
                        endTime = timeInstant,
                        endZoneOffset = ZoneId.systemDefault().rules.getOffset(timeInstant),
                        samples = listOf(
                            HeartRateRecord.Sample(
                                time = timeInstant, beatsPerMinute = heartRate.bpm.toLong()
                            )
                        ),
                        metadata = Metadata.manualEntry(),
                    )
                }

                Log.d(TAG, "writeHeartRates: Writing ${records.size} heart rate records")
                healthConnectClient.insertRecords(records)
                val insertedCount = records.size
                Log.d(TAG, "writeHeartRates: Successfully wrote $insertedCount records")

                HealthConnectResult.Success(insertedCount)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "writeHeartRates: Permission denied", e)
            HealthConnectResult.Error.PermissionDenied
        } catch (e: IOException) {
            Log.e(TAG, "writeHeartRates: Network error", e)
            HealthConnectResult.Error.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "writeHeartRates: Unknown error", e)
            HealthConnectResult.Error.Unknown
        }
    }

    // DAO-accessor implementations for Worker (replaces direct DAO access)

    override suspend fun getUnsyncedLocalSteps(source: String): List<DailyStepsEntity> {
        return dailyStepsDao.getBySourceNotSynced(source)
    }

    override suspend fun markStepsAsSynced(dates: List<Long>, source: String) {
        dailyStepsDao.markAsSynced(dates, source)
    }

    override suspend fun getUnsyncedLocalSessions(source: String): List<ExerciseSessionEntity> {
        return exerciseSessionDao.getBySourceNotSynced(source)
    }

    override suspend fun markSessionsAsSynced(ids: List<String>) {
        exerciseSessionDao.markAsSynced(ids)
    }

    override suspend fun getUnsyncedLocalHeartRates(source: String): List<HeartRateEntity> {
        return heartRateDao.getBySourceNotSynced(source)
    }

    override suspend fun markHeartRatesAsSynced(timestamps: List<Long>, source: String) {
        heartRateDao.markAsSynced(timestamps, source)
    }
}