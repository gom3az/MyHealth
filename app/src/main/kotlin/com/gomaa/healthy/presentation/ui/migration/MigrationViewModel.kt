package com.gomaa.healthy.presentation.ui.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gomaa.healthy.data.preferences.AppPreferencesManager
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        val stepsImported: Int = 0, val exerciseImported: Int = 0, val heartRateImported: Int = 0
    ) : MigrationUiState()

    data class Success(
        val stepsImported: Int, val exerciseImported: Int, val heartRateImported: Int
    ) : MigrationUiState()

    data class Error(val message: String) : MigrationUiState()
}

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val healthConnectRepository: HealthConnectRepository,
    private val appPreferencesManager: AppPreferencesManager,
    private val syncScheduler: HealthConnectSyncScheduler
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

            val workId = syncScheduler.enqueueImmediateSync(
                masterSyncEnabled = true,
                syncStepsEnabled = true,
                syncExerciseEnabled = true,
                syncHeartRateEnabled = true
            )

            workManager.getWorkInfoByIdFlow(workId).first { it?.state?.isFinished == true }
                ?.let { finishedWorkInfo ->
                    when (finishedWorkInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            appPreferencesManager.setMigrationComplete()
                            _state.value = MigrationUiState.Success(
                                stepsImported = healthConnectRepository.getStepCount(),
                                exerciseImported = healthConnectRepository.getExerciseSessionCount(),
                                heartRateImported = healthConnectRepository.getHeartRateCount()
                            )
                            _sideEffect.emit(MigrationSideEffect.MigrationComplete)
                        }

                        WorkInfo.State.FAILED -> {
                            _state.value =
                                MigrationUiState.Error("Migration failed. Please check Health Connect permissions.")
                            _sideEffect.emit(MigrationSideEffect.ShowError("Migration failed. Please check Health Connect permissions."))
                        }

                        WorkInfo.State.CANCELLED -> {
                            _state.value = MigrationUiState.Error("Migration was cancelled")
                            _sideEffect.emit(MigrationSideEffect.ShowError("Migration was cancelled"))
                        }

                        else -> { /* Handle other states */
                        }
                    }
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