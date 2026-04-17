package com.gomaa.healthy.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.CombinedSteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.usecase.GetActiveGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetCombinedStepsUseCase
import com.gomaa.healthy.domain.usecase.GetDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.GetLatestHeartRateUseCase
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import com.gomaa.healthy.domain.usecase.GetTodayHeartRateSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    // Loading state
    val isLoading: Boolean = false,

    // Today's Steps
    val todaySteps: Int = 0,
    val stepGoal: Int = 10000,
    val stepGoalProgress: Float = 0f,
    val combinedSteps: CombinedSteps = CombinedSteps(0, 0, 0),

    // Today's Heart Rate Summary (NEW - replaces real-time HR)
    val todayHeartRateSummary: com.gomaa.healthy.domain.model.HeartRateSummary? = null,
    val isLoadingHeartRate: Boolean = false,

    // Activity Metrics
    val activeMinutes: Int = 0,
    val caloriesBurned: Int = 0,

    // Goals
    val activeGoals: List<FitnessGoal> = emptyList(),
    val primaryGoalProgress: Float = 0f,

    // Recent Sessions
    val recentSessions: List<ExerciseSession> = emptyList(),

    val healthConnectAvailable: Boolean = false,
)

sealed class HomeIntent {
    data object OnLoadData : HomeIntent()
    data object OnRefresh : HomeIntent()
    data object OnSyncData : HomeIntent()
    data object OnViewGoals : HomeIntent()
}

sealed class HomeEffect {
    data class ShowError(val message: String) : HomeEffect()
    data class ShowSuccess(val message: String) : HomeEffect()
    data object NavigateToGoals : HomeEffect()
    data object NavigateToDashboard : HomeEffect()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase,
    private val getCombinedStepsUseCase: GetCombinedStepsUseCase,
    private val getDailyStepsUseCase: GetDailyStepsUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getTodayHeartRateSummaryUseCase: GetTodayHeartRateSummaryUseCase,
    private val getLatestHeartRateUseCase: GetLatestHeartRateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        processIntent(HomeIntent.OnLoadData)
    }

    fun processIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnLoadData -> loadInitialData()
            is HomeIntent.OnRefresh -> loadInitialData()
            is HomeIntent.OnSyncData -> syncData()
            is HomeIntent.OnViewGoals -> navigateToGoals()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Load steps
                val combinedSteps = getCombinedStepsUseCase(LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    combinedSteps = combinedSteps, todaySteps = combinedSteps.totalSteps
                )

                // Load today's activity metrics (active minutes, calories)
                val todaySteps = getDailyStepsUseCase(LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    activeMinutes = todaySteps?.activeMinutes ?: 0,
                    caloriesBurned = calculateCaloriesFromSteps(combinedSteps.totalSteps)
                )

                // Load goals and calculate progress
                val goals = getActiveGoalsUseCase()
                _uiState.value = _uiState.value.copy(activeGoals = goals)

                val stepGoal = goals.find { it.type is GoalType.Steps }
                if (stepGoal != null) {
                    val target = (stepGoal.type as GoalType.Steps).target
                    val progress =
                        if (target > 0) combinedSteps.totalSteps.toFloat() / target else 0f
                    _uiState.value = _uiState.value.copy(
                        stepGoalProgress = progress.coerceIn(0f, 1f), stepGoal = target
                    )
                }

                // Load today's heart rate summary
                loadTodayHeartRateSummary()

                // Load recent sessions
                val sessions = getSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    recentSessions = sessions.take(2),
                    healthConnectAvailable = combinedSteps.healthConnectSteps > 0
                )

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _effect.emit(HomeEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun calculateCaloriesFromSteps(steps: Int): Int {
        // Rough estimate: ~0.04 kcal per step
        return (steps * 0.04).toInt()
    }

    private fun loadTodayHeartRateSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHeartRate = true)
            try {
                // Try to get today's summary first
                val summary = getTodayHeartRateSummaryUseCase()
                _uiState.value = _uiState.value.copy(
                    todayHeartRateSummary = summary, isLoadingHeartRate = false
                )
            } catch (e: Exception) {
                // Fallback to latest if today's not available
                val latest = getLatestHeartRateUseCase()
                if (latest != null) {
                    _uiState.value = _uiState.value.copy(
                        todayHeartRateSummary = com.gomaa.healthy.domain.model.HeartRateSummary(
                            averageBpm = latest.bpm,
                            maxBpm = latest.bpm,
                            minBpm = latest.bpm,
                            readingCount = 1,
                            source = latest.source
                        ), isLoadingHeartRate = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingHeartRate = false)
                }
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _effect.emit(HomeEffect.ShowSuccess("Syncing health data..."))
                loadInitialData()
            } catch (e: Exception) {
                _effect.emit(HomeEffect.ShowError("Sync failed: ${e.message}"))
            }
        }
    }

    private fun navigateToGoals() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToGoals)
        }
    }
}
