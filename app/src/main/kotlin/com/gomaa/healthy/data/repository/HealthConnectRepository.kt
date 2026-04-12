package com.gomaa.healthy.data.repository

import android.content.Context
import android.content.pm.PackageManager
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
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectStepsDao: HealthConnectStepsDao,
    private val healthConnectExerciseSessionDao: HealthConnectExerciseSessionDao
) {
    companion object {
        private val READ_STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        private val WRITE_STEPS = HealthPermission.getWritePermission(StepsRecord::class)
        private val READ_EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val WRITE_EXERCISE =
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        val PERMISSIONS = setOf(READ_STEPS, WRITE_STEPS, READ_EXERCISE, WRITE_EXERCISE)

        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, 0)
            HealthConnectClient.getSdkStatus(
                context, HEALTH_CONNECT_PACKAGE
            ) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPermissions(): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            PERMISSIONS.all { granted.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncSteps(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class, timeRangeFilter = TimeRangeFilter.before(now)
            )

            val response = healthConnectClient.readRecords(request)
            val records = response.records

            val newRecords = records.filter { record ->
                healthConnectStepsDao.getByRecordId(record.metadata.id) == null
            }

            val entities = newRecords.map { record ->
                HealthConnectStepEntity(
                    count = record.count.toInt(),
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    healthConnectRecordId = record.metadata.id
                )
            }

            if (entities.isNotEmpty()) {
                healthConnectStepsDao.insertAll(entities)
            }

            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncExerciseSessions(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()

            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.before(now)
            )

            val response = healthConnectClient.readRecords(request)
            val records = response.records

            val newRecords = records.filter { record ->
                healthConnectExerciseSessionDao.getByRecordId(record.metadata.id) == null
            }

            val entities = newRecords.map { record ->
                val start = record.startTime.toEpochMilli()
                val end = record.endTime.toEpochMilli()
                HealthConnectExerciseSessionEntity(
                    startTime = start,
                    endTime = end,
                    exerciseType = "EXERCISE",
                    durationMinutes = ((end - start) / 60000).toInt(),
                    caloriesBurned = null,
                    healthConnectRecordId = record.metadata.id
                )
            }

            if (entities.isNotEmpty()) {
                healthConnectExerciseSessionDao.insertAll(entities)
            }

            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStepCount(): Int {
        return healthConnectStepsDao.getStepCount()
    }

    suspend fun getExerciseSessionCount(): Int {
        return healthConnectExerciseSessionDao.getSessionCount()
    }

}