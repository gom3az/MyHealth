package com.gomaa.healthy.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.CombinedSteps
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.usecase.ConnectWearableUseCase
import com.gomaa.healthy.domain.usecase.DisconnectWearableUseCase
import com.gomaa.healthy.domain.usecase.GetActiveGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetAvailableProvidersUseCase
import com.gomaa.healthy.domain.usecase.GetCombinedStepsUseCase
import com.gomaa.healthy.domain.usecase.GetDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import com.gomaa.healthy.domain.usecase.HasAvailableDevicesUseCase
import com.gomaa.healthy.domain.usecase.SelectWearableProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class StepSourceFilter {
    ALL, MY_HEALTH, HEALTH_CONNECT
}

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
    val stepSourceFilter: StepSourceFilter = StepSourceFilter.ALL,
    val combinedSteps: CombinedSteps = CombinedSteps(0, 0, 0),
    val healthConnectAvailable: Boolean = false
)

sealed class HomeIntent {
    data object OnLoadData : HomeIntent()
    data object OnRefresh : HomeIntent()
    data class OnSelectProvider(val brand: String) : HomeIntent()
    data class OnSwitchProvider(val brand: String) : HomeIntent()
    data object OnConnect : HomeIntent()
    data object OnDisconnect : HomeIntent()
    data class OnFilterChanged(val filter: StepSourceFilter) : HomeIntent()
}

sealed class HomeEffect {
    data class ShowError(val message: String) : HomeEffect()
    data class ShowSuccess(val message: String) : HomeEffect()
    data object NavigateToSettings : HomeEffect()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAvailableProvidersUseCase: GetAvailableProvidersUseCase,
    private val selectWearableProviderUseCase: SelectWearableProviderUseCase,
    private val hasAvailableDevicesUseCase: HasAvailableDevicesUseCase,
    private val connectWearableUseCase: ConnectWearableUseCase,
    private val disconnectWearableUseCase: DisconnectWearableUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val getDailyStepsUseCase: GetDailyStepsUseCase,
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase,
    private val getCombinedStepsUseCase: GetCombinedStepsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    private var heartRateJob: Job? = null
    private var connectionJob: Job? = null

    init {
        processIntent(HomeIntent.OnLoadData)
        observeWearableData()
    }

    fun processIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnLoadData -> loadInitialData()
            is HomeIntent.OnRefresh -> loadInitialData()
            is HomeIntent.OnSelectProvider -> selectProvider(intent.brand)
            is HomeIntent.OnSwitchProvider -> switchProvider(intent.brand)
            is HomeIntent.OnConnect -> connect()
            is HomeIntent.OnDisconnect -> disconnect()
            is HomeIntent.OnFilterChanged -> updateFilter(intent.filter)
        }
    }

    private fun updateFilter(filter: StepSourceFilter) {
        _uiState.value = _uiState.value.copy(stepSourceFilter = filter)
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

                // Get combined steps from both sources
                val combinedSteps = getCombinedStepsUseCase(LocalDate.now())
                _uiState.value = _uiState.value.copy(combinedSteps = combinedSteps)

                val goals = getActiveGoalsUseCase()
                _uiState.value = _uiState.value.copy(activeGoals = goals)

                // Calculate progress based on combined steps
                val stepGoal = goals.find { it.type is GoalType.Steps }
                val totalSteps = combinedSteps.totalSteps
                if (todaySteps != null && stepGoal != null) {
                    val target = (stepGoal.type as GoalType.Steps).target
                    val progress = if (target > 0) totalSteps.toFloat() / target else 0f
                    _uiState.value = _uiState.value.copy(stepGoalProgress = progress.coerceIn(0f, 1f))
                }

                // Check if Health Connect has data
                val healthConnectAvailable = combinedSteps.healthConnectSteps > 0
                _uiState.value =
                    _uiState.value.copy(healthConnectAvailable = healthConnectAvailable)

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _effect.emit(HomeEffect.ShowError(e.message ?: "Unknown error"))
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

    private fun selectProvider(brand: String) {
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

    private fun connect() {
        viewModelScope.launch {
            val provider = selectWearableProviderUseCase.getCurrentProvider()
            provider?.let { p ->
                try {
                    if (hasAvailableDevicesUseCase(p.brand)) {
                        val result = connectWearableUseCase("device-1")
                        if (result.isFailure) {
                            val errorMsg = result.exceptionOrNull()?.message ?: "Failed to connect"
                            _effect.emit(HomeEffect.ShowError(errorMsg))
                        }
                    } else {
                        val errorMsg =
                            "No available devices found. Please pair a wearable device first."
                        _effect.emit(HomeEffect.ShowError(errorMsg))
                    }
                } catch (e: Exception) {
                    _effect.emit(HomeEffect.ShowError(e.message ?: "Unknown error"))
                }
            }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            disconnectWearableUseCase()
        }
    }

    private fun switchProvider(brand: String) {
        viewModelScope.launch {
            try {
                disconnectWearableUseCase()
                val provider = selectWearableProviderUseCase(brand)
                if (provider != null) {
                    _effect.emit(HomeEffect.ShowSuccess("Provider changed to $brand"))
                } else {
                    _effect.emit(HomeEffect.ShowError("Failed to switch provider"))
                }
            } catch (e: Exception) {
                _effect.emit(HomeEffect.ShowError(e.message ?: "Failed to switch provider"))
            }
        }
    }
}