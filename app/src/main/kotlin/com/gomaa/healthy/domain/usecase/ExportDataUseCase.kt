package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.data.local.dao.DailyStepsDao
import com.gomaa.healthy.data.local.dao.ExerciseSessionDao
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class ExportData(
    val steps: List<StepExportData>,
    val exerciseSessions: List<ExerciseSessionExportData>
)

data class StepExportData(
    val date: String,
    val count: Int,
    val source: String,
    val distance: Double,
    val activeMinutes: Int
)

data class ExerciseSessionExportData(
    val startTime: String,
    val endTime: String,
    val duration: String,
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val source: String
)

class ExportDataUseCase @Inject constructor(
    private val dailyStepsDao: DailyStepsDao,
    private val exerciseSessionDao: ExerciseSessionDao
) {
    suspend fun getExportData(
        startDate: LocalDate,
        endDate: LocalDate,
        includeHealthConnect: Boolean
    ): ExportData {
        val stepsList = mutableListOf<StepExportData>()

        // Get steps from unified daily_steps table
        val allSteps = dailyStepsDao.getByDateRange(startDate.toEpochDay(), endDate.toEpochDay())

        stepsList.addAll(allSteps.map { entity ->
            StepExportData(
                date = LocalDate.ofEpochDay(entity.date).toString(),
                count = entity.totalSteps,
                source = if (entity.source == "health_connect") "Health Connect" else "MyHealth",
                distance = entity.totalDistanceMeters,
                activeMinutes = entity.activeMinutes
            )
        })

        // Get exercise sessions
        val sessions = exerciseSessionDao.getAll()

        val startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        val filteredSessions = sessions.filter { entity ->
            entity.startTime >= startEpoch && entity.endTime <= endEpoch
        }

        val sessionsList = filteredSessions.map { entity ->
            ExerciseSessionExportData(
                startTime = java.time.Instant.ofEpochMilli(entity.startTime)
                    .atZone(ZoneOffset.UTC).toLocalDateTime().toString(),
                endTime = java.time.Instant.ofEpochMilli(entity.endTime)
                    .atZone(ZoneOffset.UTC).toLocalDateTime().toString(),
                duration = "${(entity.endTime - entity.startTime) / 60000} minutes",
                avgHeartRate = entity.avgHeartRate,
                maxHeartRate = entity.maxHeartRate,
                source = entity.source.ifEmpty { "MyHealth" }
            )
        }

        return ExportData(
            steps = stepsList.sortedBy { it.date },
            exerciseSessions = sessionsList.sortedBy { it.startTime }
        )
    }

    fun toCsvString(exportData: ExportData): String {
        val builder = StringBuilder()

        // Steps section
        builder.appendLine("Steps Data")
        builder.appendLine("Date,Count,Source,Distance (m),Active Minutes")
        exportData.steps.forEach { step ->
            builder.appendLine("${step.date},${step.count},${step.source},${step.distance.toInt()},${step.activeMinutes}")
        }

        builder.appendLine()

        // Exercise sessions section
        builder.appendLine("Exercise Sessions")
        builder.appendLine("Start Time,End Time,Duration,Avg Heart Rate,Max Heart Rate,Source")
        exportData.exerciseSessions.forEach { session ->
            builder.appendLine("${session.startTime},${session.endTime},${session.duration},${session.avgHeartRate},${session.maxHeartRate},${session.source}")
        }

        return builder.toString()
    }
}