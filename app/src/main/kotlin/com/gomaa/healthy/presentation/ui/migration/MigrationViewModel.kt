package com.gomaa.healthy.presentation.ui.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.data.preferences.AppPreferencesManager
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

sealed class MigrationIntent {
    data object StartMigration : MigrationIntent()
    data object CancelMigration : MigrationIntent()
    data object SkipMigration : MigrationIntent()
}

sealed class MigrationSideEffect {
    data object MigrationComplete : MigrationSideEffect()
    data class ShowError(val message: String) : MigrationSideEffect()
    data object NavigateToHome : MigrationSideEffect()
}

sealed class MigrationUiState {
    data object Idle : MigrationUiState()
    data class InProgress(
        val stepsImported: Int = 0,
        val exerciseImported: Int = 0,
        val heartRateImported: Int = 0
    ) : MigrationUiState()

    data class Success(
        val stepsImported: Int,
        val exerciseImported: Int,
        val heartRateImported: Int
    ) : MigrationUiState()

    data class Error(val message: String) : MigrationUiState()
}

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
    private val appPreferencesManager: AppPreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow<MigrationUiState>(MigrationUiState.Idle)
    val state: StateFlow<MigrationUiState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<MigrationSideEffect>()
    val sideEffect: SharedFlow<MigrationSideEffect> = _sideEffect.asSharedFlow()

    fun processIntent(intent: MigrationIntent) {
        when (intent) {
            is MigrationIntent.StartMigration -> startMigration()
            is MigrationIntent.CancelMigration -> cancelMigration()
            is MigrationIntent.SkipMigration -> skipMigration()
        }
    }

    private fun startMigration() {
        viewModelScope.launch {
            _state.value = MigrationUiState.InProgress()

            try {
                val stepsResult = healthConnectRepository.syncSteps()
                val stepsCount = when (stepsResult) {
                    is HealthConnectResult.Success -> stepsResult.data
                    else -> 0
                }
                _state.value = MigrationUiState.InProgress(stepsImported = stepsCount)

                val exerciseResult = healthConnectRepository.syncExerciseSessions()
                val exerciseCount = when (exerciseResult) {
                    is HealthConnectResult.Success -> exerciseResult.data
                    else -> 0
                }
                _state.value = MigrationUiState.InProgress(
                    stepsImported = stepsCount,
                    exerciseImported = exerciseCount
                )

                val heartRateResult = healthConnectRepository.syncHeartRates()
                val heartRateCount = when (heartRateResult) {
                    is HealthConnectResult.Success -> heartRateResult.data
                    else -> 0
                }

                appPreferencesManager.setMigrationComplete()

                _state.value = MigrationUiState.Success(
                    stepsImported = stepsCount,
                    exerciseImported = exerciseCount,
                    heartRateImported = heartRateCount
                )

                _sideEffect.emit(MigrationSideEffect.MigrationComplete)
            } catch (e: Exception) {
                _state.value = MigrationUiState.Error(e.message ?: "Migration failed")
                _sideEffect.emit(MigrationSideEffect.ShowError(e.message ?: "Migration failed"))
            }
        }
    }

    private fun cancelMigration() {
        viewModelScope.launch {
            appPreferencesManager.skipMigration()
            _sideEffect.emit(MigrationSideEffect.NavigateToHome)
        }
    }

    private fun skipMigration() {
        viewModelScope.launch {
            appPreferencesManager.skipMigration()
            _sideEffect.emit(MigrationSideEffect.NavigateToHome)
        }
    }
}