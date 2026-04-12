package com.gomaa.healthy.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.data.repository.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsIntent {
    data object CheckHealthConnect : SettingsIntent()
    data object SyncNow : SettingsIntent()
    data object ClearError : SettingsIntent()
}

data class SettingsState(
    val isLoading: Boolean = false,
    val isAvailable: Boolean = false,
    val isConnected: Boolean = false,
    val stepCount: Int = 0,
    val exerciseSessionCount: Int = 0,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.CheckHealthConnect -> checkHealthConnectStatus()
            is SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.ClearError -> clearError()
        }
    }

    private fun checkHealthConnectStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val isAvailable = healthConnectRepository.isAvailable()
            val hasPerms = healthConnectRepository.hasPermissions()
            val isConnected = isAvailable && hasPerms
            val stepCount = if (isConnected) healthConnectRepository.getStepCount() else 0
            val exerciseCount =
                if (isConnected) healthConnectRepository.getExerciseSessionCount() else 0

            _state.value = _state.value.copy(
                isLoading = false,
                isAvailable = isAvailable,
                isConnected = isConnected,
                stepCount = stepCount,
                exerciseSessionCount = exerciseCount
            )
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, errorMessage = null)

            val stepsResult = healthConnectRepository.syncSteps()
            val exerciseResult = healthConnectRepository.syncExerciseSessions()

            if (stepsResult.isSuccess && exerciseResult.isSuccess) {
                _state.value = _state.value.copy(
                    isSyncing = false,
                    stepCount = healthConnectRepository.getStepCount(),
                    exerciseSessionCount = healthConnectRepository.getExerciseSessionCount(),
                    lastSyncTime = System.currentTimeMillis()
                )
            } else {
                val error = stepsResult.exceptionOrNull()?.message
                    ?: exerciseResult.exceptionOrNull()?.message
                    ?: "Unknown error"
                _state.value = _state.value.copy(
                    isSyncing = false,
                    errorMessage = error
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}