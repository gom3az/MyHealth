package com.gomaa.healthy.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gomaa.healthy.data.preferences.SyncPreferences
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsIntent {
    data object CheckHealthConnect : SettingsIntent()
    data object SyncNow : SettingsIntent()
    data object RequestHealthConnectPermissions : SettingsIntent()
    data object PermissionsRequested : SettingsIntent()
    data class SetMasterSync(val enabled: Boolean) : SettingsIntent()
    data class SetStepsSync(val enabled: Boolean) : SettingsIntent()
    data class SetExerciseSync(val enabled: Boolean) : SettingsIntent()
    data class SetHeartRateSync(val enabled: Boolean) : SettingsIntent()
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
        val heartRateCount: Int = 0,
        val lastSyncTime: Long? = null,
        val isSyncing: Boolean = false,
        val syncPreferences: SyncPreferences = SyncPreferences()
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val healthConnectRepository: HealthConnectRepository,
    private val syncPreferencesManager: SyncPreferencesManager,
    private val syncScheduler: HealthConnectSyncScheduler
) : ViewModel() {

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<SettingsSideEffect>()
    val sideEffect: SharedFlow<SettingsSideEffect> = _sideEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                _state, syncPreferencesManager.preferencesFlow
            ) { uiState, prefs ->
                uiState to prefs
            }.collect { (uiState, prefs) ->
                if (uiState is SettingsUiState.Idle) {
                    _state.value = uiState.copy(syncPreferences = prefs)
                }
            }
        }
    }

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.CheckHealthConnect -> checkHealthConnectStatus()
            is SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.RequestHealthConnectPermissions -> requestHealthConnectPermissions()
            is SettingsIntent.PermissionsRequested -> checkHealthConnectStatus()
            is SettingsIntent.SetMasterSync -> viewModelScope.launch { setMasterSync(intent.enabled) }
            is SettingsIntent.SetStepsSync -> viewModelScope.launch { setStepsSync(intent.enabled) }
            is SettingsIntent.SetExerciseSync -> viewModelScope.launch { setExerciseSync(intent.enabled) }
            is SettingsIntent.SetHeartRateSync -> viewModelScope.launch { setHeartRateSync(intent.enabled) }
        }
    }HiltWorkerFactory

    private suspend fun setMasterSync(enabled: Boolean) {
        syncPreferencesManager.setMasterSyncEnabled(enabled)
        updateScheduler(enabled = enabled)
    }

    private suspend fun setStepsSync(enabled: Boolean) {
        syncPreferencesManager.setStepsSyncEnabled(enabled)
        updateScheduler()
    }

    private suspend fun setExerciseSync(enabled: Boolean) {
        syncPreferencesManager.setExerciseSyncEnabled(enabled)
        updateScheduler()
    }

    private suspend fun setHeartRateSync(enabled: Boolean) {
        syncPreferencesManager.setHeartRateSyncEnabled(enabled)
        updateScheduler()
    }

    private suspend fun updateScheduler(enabled: Boolean? = null) {
        val prefs = syncPreferencesManager.getPreferences()
        syncScheduler.schedulePeriodicSync(
            masterSyncEnabled = enabled ?: prefs.masterSyncEnabled,
            syncStepsEnabled = prefs.syncStepsEnabled,
            syncExerciseEnabled = prefs.syncExerciseEnabled,
            syncHeartRateEnabled = prefs.syncHeartRateEnabled
        )
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

            // Only launch async calls if connected
            val stepCount: Int
            val exerciseCount: Int
            val heartRateCount: Int
            val lastSyncTime: Long?

            if (isConnected) {
                val stepDeferred = async { healthConnectRepository.getStepCount() }
                val exerciseDeferred = async { healthConnectRepository.getExerciseSessionCount() }
                val heartRateDeferred = async { healthConnectRepository.getHeartRateCount() }
                val lastSyncDeferred = async { healthConnectRepository.getLastSyncTime() }

                stepCount = stepDeferred.await()
                exerciseCount = exerciseDeferred.await()
                heartRateCount = heartRateDeferred.await()
                lastSyncTime = lastSyncDeferred.await()
            } else {
                stepCount = 0
                exerciseCount = 0
                heartRateCount = 0
                lastSyncTime = null
            }

            _state.value = SettingsUiState.Idle(
                isAvailable = isAvailable,
                isConnected = isConnected,
                stepCount = stepCount,
                exerciseSessionCount = exerciseCount,
                heartRateCount = heartRateCount,
                lastSyncTime = lastSyncTime
            )
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is SettingsUiState.Idle) {
                _state.value = currentState.copy(isSyncing = true)
            }

            val prefs = syncPreferencesManager.getPreferences()

            val workId = syncScheduler.enqueueImmediateSync(
                masterSyncEnabled = prefs.masterSyncEnabled,
                syncStepsEnabled = prefs.syncStepsEnabled,
                syncExerciseEnabled = prefs.syncExerciseEnabled,
                syncHeartRateEnabled = prefs.syncHeartRateEnabled
            )

            workManager.getWorkInfoByIdFlow(workId).first { it?.state?.isFinished == true }
                ?.let { finishedWorkInfo ->
                    val current = _state.value
                    when (finishedWorkInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val stepCount = healthConnectRepository.getStepCount()
                            val exerciseCount = healthConnectRepository.getExerciseSessionCount()
                            val heartRateCount = healthConnectRepository.getHeartRateCount()
                            val lastSyncTime = healthConnectRepository.getLastSyncTime()

                            _state.value = SettingsUiState.Idle(
                                isAvailable = true,
                                isConnected = true,
                                stepCount = stepCount,
                                exerciseSessionCount = exerciseCount,
                                heartRateCount = heartRateCount,
                                lastSyncTime = lastSyncTime,
                                isSyncing = false,
                                syncPreferences = prefs
                            )
                        }

                        WorkInfo.State.FAILED -> {
                            _state.value = current.let {
                                if (it is SettingsUiState.Idle) it.copy(isSyncing = false)
                                else it
                            }
                            _sideEffect.emit(
                                SettingsSideEffect.ShowError(
                                    "Sync failed. Please check Health Connect permissions and try again."
                                )
                            )
                        }

                        WorkInfo.State.CANCELLED -> {
                            _state.value = current.let {
                                if (it is SettingsUiState.Idle) it.copy(isSyncing = false)
                                else it
                            }
                            _sideEffect.emit(SettingsSideEffect.ShowError("Sync was cancelled"))
                        }

                        else -> { /* Handle other states */
                        }
                    }
                }
        }
    }
}