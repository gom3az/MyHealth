package com.gomaa.healthy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.usecase.ConnectWearableUseCase
import com.gomaa.healthy.domain.usecase.DisconnectWearableUseCase
import com.gomaa.healthy.domain.usecase.GetActiveGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetAvailableProvidersUseCase
import com.gomaa.healthy.domain.usecase.GetDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import com.gomaa.healthy.domain.usecase.HasAvailableDevicesUseCase
import com.gomaa.healthy.domain.usecase.SelectWearableProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val heartRate: Int = 0,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val connectedDeviceBrand: String? = null,
    val availableProviders: List<String> = emptyList(),
    val hasAvailableDevices: Boolean = false,
    val recentSessions: List<ExerciseSession> = emptyList(),
    val todaySteps: DailySteps? = null,
    val activeGoals: List<FitnessGoal> = emptyList(),
    val stepGoalProgress: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAvailableProvidersUseCase: GetAvailableProvidersUseCase,
    private val selectWearableProviderUseCase: SelectWearableProviderUseCase,
    private val hasAvailableDevicesUseCase: HasAvailableDevicesUseCase,
    private val connectWearableUseCase: ConnectWearableUseCase,
    private val disconnectWearableUseCase: DisconnectWearableUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getDailyStepsUseCase: GetDailyStepsUseCase,
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var heartRateJob: Job? = null
    private var connectionJob: Job? = null

    init {
        loadInitialData()
        observeWearableData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentProviders = getAvailableProvidersUseCase()
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
            selectWearableProviderUseCase.selectedProvider.collectLatest { provider ->
                _uiState.value = _uiState.value.copy(connectedDeviceBrand = provider?.brand)

                heartRateJob?.cancel()
                connectionJob?.cancel()

                provider?.let { p ->
                    heartRateJob = viewModelScope.launch {
                        p.heartRateFlow().collectLatest { hr ->
                            _uiState.value = _uiState.value.copy(heartRate = hr)
                        }
                    }
                    connectionJob = viewModelScope.launch {
                        p.connectionStatus().collectLatest { state ->
                            _uiState.value = _uiState.value.copy(connectionState = state)
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        loadInitialData()
    }

    fun selectProvider(brand: String) {
        viewModelScope.launch {
            val provider = selectWearableProviderUseCase(brand)
            provider?.let {
                val hasDevices = hasAvailableDevicesUseCase(brand)
                _uiState.value = _uiState.value.copy(
                    connectedDeviceBrand = brand,
                    hasAvailableDevices = hasDevices
                )
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            val provider = selectWearableProviderUseCase.getCurrentProvider()
            provider?.let { p ->
                try {
                    if (hasAvailableDevicesUseCase(p.brand)) {
                        val result = connectWearableUseCase("device-1")
                        if (result.isFailure) {
                            _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "No available devices found. Please pair a wearable device first."
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectWearableUseCase()
        }
    }
}