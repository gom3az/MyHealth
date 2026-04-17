package com.gomaa.healthy.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.usecase.GetActiveGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetHomeScreenDataUseCase
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
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

    // Today's Heart info
    val averageBpm: Int? = null,
    val maxBpm: Int? = null,
    val minBpm: Int? = null,
    val readingCount: Int? = null,

    // Activity Metrics
    val activeMinutes: Int = 0,
    val caloriesBurned: Int = 0,

    // Goals
    val activeGoals: List<FitnessGoal> = emptyList(),
    val primaryGoalProgress: Float = 0f,

    // Recent Sessions
    val recentSessions: List<ExerciseSession> = emptyList(),

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
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getHomeScreenDataUseCase: GetHomeScreenDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

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

                val homeData = getHomeScreenDataUseCase(LocalDate.now())
                // Load today's steps
                _uiState.value = _uiState.value.copy(
                    todaySteps = homeData?.totalSteps ?: 0,
                    activeMinutes = homeData?.activeMinutes ?: 0,
                    caloriesBurned = calculateCaloriesFromSteps(homeData?.totalSteps ?: 0),
                    averageBpm = homeData?.avgBpm ?: 0,
                    maxBpm = homeData?.maxBpm ?: 0,
                    minBpm = homeData?.minBpm ?: 0,
                    readingCount = homeData?.heartRateCount ?: 0
                )

                // Load goals and calculate progress
                val goals = getActiveGoalsUseCase()
                _uiState.value = _uiState.value.copy(activeGoals = goals)

                val stepGoal = goals.find { it.type is GoalType.Steps }
                if (stepGoal != null) {
                    val target = (stepGoal.type as GoalType.Steps).target
                    val progress =
                        if (target > 0) (homeData?.totalSteps ?: 0).toFloat() / target else 0f
                    _uiState.value = _uiState.value.copy(
                        stepGoalProgress = progress.coerceIn(0f, 1f), stepGoal = target
                    )
                }

                // Load recent sessions
                val sessions = getSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    recentSessions = sessions.take(2),
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
