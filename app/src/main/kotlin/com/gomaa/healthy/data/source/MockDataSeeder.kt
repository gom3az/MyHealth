package com.gomaa.healthy.data.source

import android.content.Context
import android.util.Log
import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import com.gomaa.healthy.data.local.dao.GoalDao
import com.gomaa.healthy.data.local.dao.HeartRateDao
import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.FitnessGoalEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Mock Data Seeder - Seeds the Room database with realistic mock data from JSON files.
 *
 * Usage:
 * ```kotlin
 * val seeder = MockDataSeeder(
 *     stepsDao = dailyStepsDao,
 *     goalDao = goalDao,
 *     sessionDao = exerciseSessionDao,
 *     heartRateDao = heartRateDao
 * )
 * seeder.seedDatabase(context, clearFirst = true)
 * ```
 */
class MockDataSeeder(
    private val stepsDao: DailyStepsDao,
    private val goalDao: GoalDao,
    private val sessionDao: ExerciseSessionDao,
    private val heartRateDao: HeartRateDao
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val tag = "MockDataSeeder"

    /**
     * Seed database with all mock data from JSON files.
     *
     * @param context Application context for asset access
     * @param clearFirst If true, clear existing data before seeding
     */
    suspend fun seedDatabase(context: Context, clearFirst: Boolean = true) {
        try {
            if (clearFirst) {
                Log.d(tag, "Clearing existing data...")
                clearAllTables()
            }

            Log.d(tag, "Seeding exercise sessions...")
            seedExerciseSessions(context)

            Log.d(tag, "Seeding daily steps...")
            seedDailySteps(context)

            Log.d(tag, "Seeding fitness goals...")
            seedFitnessGoals(context)

            Log.d(tag, "Database seeding completed successfully!")
        } catch (e: Exception) {
            Log.e(tag, "Failed to seed database", e)
            throw e
        }
    }

    /**
     * Clear all data from mock-related tables.
     */
    private suspend fun clearAllTables() {
        heartRateDao.deleteAll()
        sessionDao.deleteAll()
        goalDao.deleteAll()
        stepsDao.deleteAll()
        Log.d(tag, "All tables cleared")
    }

    /**
     * Seed exercise sessions from JSON file.
     */
    private suspend fun seedExerciseSessions(context: Context) {
        val sessionsJson = loadJsonFromAssets(context, "mock_data/exercise_sessions.json")
        val sessionDtos = json.decodeFromString<List<ExerciseSessionDto>>(sessionsJson)

        var count = 0
        sessionDtos.forEach { dto ->
            val sessionEntity = dto.toEntity()
            sessionDao.insert(sessionEntity)

            // Insert heart rate records for this session
            dto.heartRates.forEach { hrDto ->
                val hrEntity = hrDto.toEntity(sessionEntity.id)
                heartRateDao.insert(hrEntity)
            }
            count++
        }

        Log.d(tag, "Inserted $count exercise sessions with heart rate records")
    }

    /**
     * Seed daily steps from JSON file.
     */
    private suspend fun seedDailySteps(context: Context) {
        val stepsJson = loadJsonFromAssets(context, "mock_data/daily_steps.json")
        val stepsDtos = json.decodeFromString<List<DailyStepsDto>>(stepsJson)

        val entities = stepsDtos.map { it.toEntity() }
        stepsDao.insertAll(entities)

        Log.d(tag, "Inserted ${entities.size} daily step records")
    }

    /**
     * Seed fitness goals from JSON file.
     */
    private suspend fun seedFitnessGoals(context: Context) {
        val goalsJson = loadJsonFromAssets(context, "mock_data/fitness_goals.json")
        val goalDtos = json.decodeFromString<List<FitnessGoalDto>>(goalsJson)

        val entities = goalDtos.map { it.toEntity() }
        goalDao.insertAll(entities)

        Log.d(tag, "Inserted ${entities.size} fitness goals")
    }

    /**
     * Load JSON file from assets directory.
     */
    private suspend fun loadJsonFromAssets(context: Context, filePath: String): String {
        return withContext(Dispatchers.IO) {
            context.assets.open(filePath)
                .bufferedReader()
                .use { it.readText() }
        }
    }

    // ========== DTOs (Data Transfer Objects) ==========

    @Serializable
    private data class ExerciseSessionDto(
        val id: String,
        val startTime: Long,
        val endTime: Long,
        val avgHeartRate: Int,
        val maxHeartRate: Int,
        val minHeartRate: Int,
        val deviceBrand: String,
        val heartRates: List<HeartRateDto> = emptyList()
    ) {
        fun toEntity(): ExerciseSessionEntity {
            return ExerciseSessionEntity(
                id = id,
                startTime = startTime,
                endTime = endTime,
                avgHeartRate = avgHeartRate,
                maxHeartRate = maxHeartRate,
                minHeartRate = minHeartRate,
                deviceBrand = deviceBrand
            )
        }
    }

    @Serializable
    private data class HeartRateDto(
        val timestamp: Long,
        val bpm: Int
    ) {
        fun toEntity(sessionId: String): HeartRateEntity {
            return HeartRateEntity(
                sessionId = sessionId,
                timestamp = timestamp,
                bpm = bpm
            )
        }
    }

    @Serializable
    private data class DailyStepsDto(
        val date: Long,
        val totalSteps: Int,
        val totalDistanceMeters: Double,
        val activeMinutes: Int,
        val lightActivityMinutes: Int,
        val moderateActivityMinutes: Int,
        val vigorousActivityMinutes: Int
    ) {
        fun toEntity(): DailyStepsEntity {
            return DailyStepsEntity(
                date = date,
                totalSteps = totalSteps,
                totalDistanceMeters = totalDistanceMeters,
                activeMinutes = activeMinutes,
                lightActivityMinutes = lightActivityMinutes,
                moderateActivityMinutes = moderateActivityMinutes,
                vigorousActivityMinutes = vigorousActivityMinutes
            )
        }
    }

    @Serializable
    private data class FitnessGoalDto(
        val id: String,
        val name: String,
        val type: String,
        val targetValue: Int,
        val period: String,
        val createdAt: Long,
        val isActive: Boolean
    ) {
        fun toEntity(): FitnessGoalEntity {
            return FitnessGoalEntity(
                id = id,
                name = name,
                type = type,
                targetValue = targetValue,
                period = period,
                createdAt = createdAt,
                isActive = isActive
            )
        }
    }
}
