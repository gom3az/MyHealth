package com.gomaa.healthy.presentation.ui

import com.gomaa.healthy.domain.model.CombinedSteps
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.DeviceInfo
import com.gomaa.healthy.domain.model.DistanceUnit
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalPeriod
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.model.HeartRateRecord
import com.gomaa.healthy.domain.model.HeartRateZone
import com.gomaa.healthy.presentation.ui.analytics.AnalyticsUiState
import com.gomaa.healthy.presentation.ui.dashboard.DashboardUiState
import com.gomaa.healthy.presentation.ui.goals.GoalsUiState
import com.gomaa.healthy.presentation.ui.home.HomeUiState
import com.gomaa.healthy.presentation.ui.home.StepSourceFilter
import java.time.LocalDate

/**
 * Preview data for Compose previews.
 * Provides realistic mock data for UI development and testing.
 */
object PreviewData {

    // ========== Common Data ==========

    val connectedDevice = DeviceInfo(
        id = "device_001",
        name = "Huawei Watch GT 4",
        brand = "Huawei",
        isConnected = true
    )

    val todaySteps = DailySteps(
        date = LocalDate.now(),
        totalSteps = 8542,
        totalDistanceMeters = 6406.5,
        activeMinutes = 45,
        lightActivityMinutes = 120,
        moderateActivityMinutes = 35,
        vigorousActivityMinutes = 10
    )

    val activeGoals = listOf(
        FitnessGoal(
            id = "goal_001",
            name = "Daily Steps Challenge",
            type = GoalType.Steps(target = 10000),
            period = GoalPeriod.DAILY,
            createdAt = System.currentTimeMillis() - 86400000,
            isActive = true
        ),
        FitnessGoal(
            id = "goal_002",
            name = "Weekly Distance Run",
            type = GoalType.Distance(targetMeters = 25000.0, unit = DistanceUnit.KILOMETERS),
            period = GoalPeriod.WEEKLY,
            createdAt = System.currentTimeMillis() - 172800000,
            isActive = true
        ),
        FitnessGoal(
            id = "goal_003",
            name = "Cardio Zone Training",
            type = GoalType.HeartRateZone(zone = HeartRateZone.MODERATE, targetMinutes = 30),
            period = GoalPeriod.DAILY,
            createdAt = System.currentTimeMillis() - 259200000,
            isActive = true
        )
    )

    val recentSessions = listOf(
        ExerciseSession(
            id = "session_001",
            startTime = System.currentTimeMillis() - 86400000,
            endTime = System.currentTimeMillis() - 86400000 + 2700000,
            avgHeartRate = 142,
            maxHeartRate = 178,
            minHeartRate = 98,
            deviceBrand = "Huawei",
            heartRates = listOf(
                HeartRateRecord(timestamp = System.currentTimeMillis() - 86400000, bpm = 98),
                HeartRateRecord(
                    timestamp = System.currentTimeMillis() - 86400000 + 60000,
                    bpm = 142
                ),
                HeartRateRecord(
                    timestamp = System.currentTimeMillis() - 86400000 + 2700000,
                    bpm = 178
                )
            )
        ),
        ExerciseSession(
            id = "session_002",
            startTime = System.currentTimeMillis() - 172800000,
            endTime = System.currentTimeMillis() - 172800000 + 1800000,
            avgHeartRate = 135,
            maxHeartRate = 165,
            minHeartRate = 88,
            deviceBrand = "Huawei",
            heartRates = emptyList()
        )
    )

    // ========== Home Screen Preview States ==========

    val homeLoadedState = HomeUiState(
        isLoading = false,
        heartRate = 142,
        connectionState = ConnectionState.Connected,
        connectedDeviceBrand = "Huawei",
        availableProviders = listOf("Huawei", "Mock"),
        hasAvailableDevices = true,
        recentSessions = recentSessions,
        todaySteps = todaySteps,
        activeGoals = activeGoals,
        stepGoalProgress = 0.85f,
        stepSourceFilter = StepSourceFilter.ALL,
        combinedSteps = CombinedSteps(12542, 8542, 4000),
        healthConnectAvailable = true
    )

    val homeLoadingState = HomeUiState(
        isLoading = true,
        heartRate = 0,
        connectionState = ConnectionState.Connecting,
        connectedDeviceBrand = null,
        availableProviders = emptyList(),
        hasAvailableDevices = false,
        recentSessions = emptyList(),
        todaySteps = null,
        activeGoals = emptyList(),
        stepGoalProgress = 0f,
        stepSourceFilter = StepSourceFilter.ALL,
        combinedSteps = CombinedSteps(0, 0, 0),
        healthConnectAvailable = false
    )

    val homeDisconnectedState = homeLoadedState.copy(
        connectionState = ConnectionState.Disconnected,
        connectedDeviceBrand = null,
        heartRate = 0
    )

    val homeEmptyState = HomeUiState(
        isLoading = false,
        heartRate = 0,
        connectionState = ConnectionState.Disconnected,
        connectedDeviceBrand = null,
        availableProviders = listOf("Huawei", "Mock"),
        hasAvailableDevices = false,
        recentSessions = emptyList(),
        todaySteps = null,
        activeGoals = emptyList(),
        stepGoalProgress = 0f,
        stepSourceFilter = StepSourceFilter.ALL,
        combinedSteps = CombinedSteps(0, 0, 0),
        healthConnectAvailable = false
    )

    // ========== Dashboard Screen Preview States ==========

    val dashboardIdleState = DashboardUiState(
        isTracking = false,
        heartRate = 72,
        connectionState = ConnectionState.Connected,
        avgHeartRate = 0,
        maxHeartRate = 0,
        minHeartRate = 0,
        heartRateZone = com.gomaa.healthy.presentation.ui.dashboard.HeartRateZone.REST
    )

    val dashboardTrackingState = DashboardUiState(
        isTracking = true,
        heartRate = 152,
        connectionState = ConnectionState.Connected,
        avgHeartRate = 145,
        maxHeartRate = 168,
        minHeartRate = 98,
        heartRateZone = com.gomaa.healthy.presentation.ui.dashboard.HeartRateZone.MODERATE
    )

    val dashboardErrorState = DashboardUiState(
        isTracking = false,
        heartRate = 0,
        connectionState = ConnectionState.Error("Device not found"),
        avgHeartRate = 0,
        maxHeartRate = 0,
        minHeartRate = 0,
        heartRateZone = com.gomaa.healthy.presentation.ui.dashboard.HeartRateZone.REST
    )

    // ========== Goals Screen Preview States ==========

    val goalsLoadedState = GoalsUiState(
        isLoading = false,
        goals = activeGoals + listOf(
            FitnessGoal(
                id = "goal_004",
                name = "Morning Walk Routine",
                type = GoalType.Steps(target = 5000),
                period = GoalPeriod.DAILY,
                createdAt = System.currentTimeMillis() - 604800000,
                isActive = false
            )
        ),
        goalProgress = mapOf(
            "goal_001" to 0.85f,
            "goal_002" to 0.62f,
            "goal_003" to 1.0f,
            "goal_004" to 1.0f
        )
    )

    val goalsLoadingState = GoalsUiState(
        isLoading = true,
        goals = emptyList(),
        goalProgress = emptyMap()
    )

    val goalsEmptyState = GoalsUiState(
        isLoading = false,
        goals = emptyList(),
        goalProgress = emptyMap()
    )

    // ========== Analytics Screen Preview States ==========

    val analyticsLoadedState = AnalyticsUiState(
        isLoading = false,
        sessions = recentSessions + listOf(
            ExerciseSession(
                id = "session_003",
                startTime = System.currentTimeMillis() - 259200000,
                endTime = System.currentTimeMillis() - 259200000 + 3600000,
                avgHeartRate = 148,
                maxHeartRate = 182,
                minHeartRate = 102,
                deviceBrand = "Huawei",
                heartRates = emptyList()
            ),
            ExerciseSession(
                id = "session_004",
                startTime = System.currentTimeMillis() - 345600000,
                endTime = System.currentTimeMillis() - 345600000 + 1500000,
                avgHeartRate = 128,
                maxHeartRate = 152,
                minHeartRate = 92,
                deviceBrand = "Huawei",
                heartRates = emptyList()
            )
        ),
    )

    val analyticsLoadingState = AnalyticsUiState(
        isLoading = true,
        sessions = emptyList(),
    )

    val analyticsEmptyState = AnalyticsUiState(
        isLoading = false,
        sessions = emptyList(),
    )
}
