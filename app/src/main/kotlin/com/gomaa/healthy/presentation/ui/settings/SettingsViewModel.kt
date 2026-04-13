package com.gomaa.healthy.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsIntent {
    data object CheckHealthConnect : SettingsIntent()
    data object SyncNow : SettingsIntent()
    data object RequestHealthConnectPermissions : SettingsIntent()
    data object PermissionsRequested : SettingsIntent()
}

sealed class SettingsSideEffect {
    data object RequestPermissions : SettingsSideEffect()
    data class ShowError(val message: String) : SettingsSideEffect()
}

sealed interface SettingsUiState {
    data class Idle(
        val isAvailable: Boolean = false,
        val isConnected: Boolean = false,
        val stepCount: Int = 0,
        val exerciseSessionCount: Int = 0,
        val lastSyncTime: Long? = null,
        val isSyncing: Boolean = false
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<SettingsSideEffect>()
    val sideEffect: SharedFlow<SettingsSideEffect> = _sideEffect.asSharedFlow()

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.CheckHealthConnect -> checkHealthConnectStatus()
            is SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.RequestHealthConnectPermissions -> requestHealthConnectPermissions()
            is SettingsIntent.PermissionsRequested -> checkHealthConnectStatus()
        }
    }

    private fun requestHealthConnectPermissions() {
        viewModelScope.launch {
            _sideEffect.emit(SettingsSideEffect.RequestPermissions)
        }
    }

    private fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val isAvailableResult = healthConnectRepository.isAvailable()
            val hasPermsResult = healthConnectRepository.hasPermissions()

            val isAvailable = when (isAvailableResult) {
                is HealthConnectResult.Success -> isAvailableResult.data
                is HealthConnectResult.Error -> false
            }

            val hasPerms = when (hasPermsResult) {
                is HealthConnectResult.Success -> hasPermsResult.data
                is HealthConnectResult.Error -> false
            }

            val isConnected = isAvailable && hasPerms
            val stepCount = if (isConnected) healthConnectRepository.getStepCount() else 0
            val exerciseCount =
                if (isConnected) healthConnectRepository.getExerciseSessionCount() else 0

            _state.value = SettingsUiState.Idle(
                isAvailable = isAvailable,
                isConnected = isConnected,
                stepCount = stepCount,
                exerciseSessionCount = exerciseCount
            )
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is SettingsUiState.Idle) {
                _state.value = currentState.copy(isSyncing = true)
            }

            val stepsResult = healthConnectRepository.syncSteps()
            val exerciseResult = healthConnectRepository.syncExerciseSessions()

            val stepsSuccess = stepsResult is HealthConnectResult.Success
            val exerciseSuccess = exerciseResult is HealthConnectResult.Success

            val updatedState = _state.value
            if (updatedState is SettingsUiState.Idle) {
                if (stepsSuccess && exerciseSuccess) {
                    _state.value = updatedState.copy(
                        isSyncing = false,
                        stepCount = healthConnectRepository.getStepCount(),
                        exerciseSessionCount = healthConnectRepository.getExerciseSessionCount(),
                        lastSyncTime = System.currentTimeMillis()
                    )
                } else {
                    _state.value = updatedState.copy(isSyncing = false)
                    val error = when {
                        stepsResult is HealthConnectResult.Error -> stepsResult.exception.message
                        exerciseResult is HealthConnectResult.Error -> exerciseResult.exception.message
                        else -> "Unknown error"
                    }
                    _sideEffect.emit(SettingsSideEffect.ShowError(error ?: "Sync failed"))
                }
            }
        }
    }
}