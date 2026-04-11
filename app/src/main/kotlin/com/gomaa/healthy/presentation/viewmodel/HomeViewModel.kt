package com.gomaa.healthy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.provider.WearableManager
import com.gomaa.healthy.domain.usecase.GetActiveGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val heartRate: Int = 0,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val connectedDeviceBrand: String? = null,
    val availableProviders: List<String> = emptyList(),
    val recentSessions: List<ExerciseSession> = emptyList(),
    val todaySteps: DailySteps? = null,
    val activeGoals: List<FitnessGoal> = emptyList(),
    val stepGoalProgress: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wearableManager: WearableManager,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getDailyStepsUseCase: GetDailyStepsUseCase,
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeWearableData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentProviders = wearableManager.availableProviders
                _uiState.value = _uiState.value.copy(availableProviders = currentProviders)

                val sessions = getSessionsUseCase()
                _uiState.value = _uiState.value.copy(recentSessions = sessions.take(5))

                val todaySteps = getDailyStepsUseCase(LocalDate.now())
                _uiState.value = _uiState.value.copy(todaySteps = todaySteps)

                val goals = getActiveGoalsUseCase()
                _uiState.value = _uiState.value.copy(activeGoals = goals)

                val stepGoal = goals.find { it.type is GoalType.Steps }
                if (todaySteps != null && stepGoal != null) {
                    val target = (stepGoal.type as GoalType.Steps).target
                    val progress = if (target > 0) todaySteps.totalSteps.toFloat() / target else 0f
                    _uiState.value = _uiState.value.copy(stepGoalProgress = progress.coerceIn(0f, 1f))
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    private fun observeWearableData() {
        viewModelScope.launch {
            wearableManager.heartRate.collect { hr ->
                _uiState.value = _uiState.value.copy(heartRate = hr)
            }
        }
        viewModelScope.launch {
            wearableManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }
        viewModelScope.launch {
            wearableManager.currentProvider.collect { provider ->
                _uiState.value = _uiState.value.copy(connectedDeviceBrand = provider?.brand)
            }
        }
    }

    fun refresh() {
        loadInitialData()
    }

    fun selectProvider(brand: String) {
        val provider = wearableManager.getProvider(brand)
        provider?.let {
            wearableManager.setCurrentProvider(it)
            _uiState.value = _uiState.value.copy(connectedDeviceBrand = brand)
        }
    }

    fun connect() {
        viewModelScope.launch {
            wearableManager.currentProvider.collect { p ->
                p?.let {
                    try {
                        it.startMonitoring("device-1")
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            wearableManager.currentProvider.collect { p ->
                p?.stopMonitoring()
            }
        }
    }
}