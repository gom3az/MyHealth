package com.gomaa.healthy.presentation.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitAuthManager
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitScheduler
import com.gomaa.healthy.data.preferences.SyncPreferences
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed class SettingsIntent {
    // Health Connect intents
    data object CheckHealthConnect : SettingsIntent()
    data object SyncNow : SettingsIntent()
    data object RequestHealthConnectPermissions : SettingsIntent()
    data object PermissionsRequested : SettingsIntent()
    data class SetMasterSync(val enabled: Boolean) : SettingsIntent()
    data class SetStepsSync(val enabled: Boolean) : SettingsIntent()
    data class SetExerciseSync(val enabled: Boolean) : SettingsIntent()
    data class SetHeartRateSync(val enabled: Boolean) : SettingsIntent()

    // Health Kit intents (Phase 4)
    data object CheckHealthKitStatus : SettingsIntent()
    data object ConnectHealthKit : SettingsIntent()
    data object DisconnectHealthKit : SettingsIntent()
    data class SetSyncWindow(val days: Int) : SettingsIntent()
    data object SyncHealthKitNow : SettingsIntent()
}

sealed class SettingsSideEffect {
    data object RequestPermissions : SettingsSideEffect()
    data object RequestHealthKitSignIn : SettingsSideEffect()
    data class ShowError(val message: String) : SettingsSideEffect()
    data class ShowSuccess(val message: String) : SettingsSideEffect()
}

sealed interface SettingsUiState {
    data class Idle(
        // Health Connect state
        val isAvailable: Boolean = false,
        val isConnected: Boolean = false,
        val stepCount: Int = 0,
        val exerciseSessionCount: Int = 0,
        val heartRateCount: Int = 0,
        val lastSyncTime: Long? = null,
        val isSyncing: Boolean = false,
        val syncPreferences: SyncPreferences = SyncPreferences(),
        // Health Kit state
        val healthKitSignedIn: Boolean = false,
        val healthKitAuthState: AuthState = AuthState.NOT_SIGNED_IN,
        val healthKitSyncWindowDays: Int = 1,
        val lastHealthKitSyncTime: Long? = null,
        val isHealthKitSyncing: Boolean = false
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val healthConnectRepository: HealthConnectRepository,
    private val syncPreferencesManager: SyncPreferencesManager,
    private val syncScheduler: HealthConnectSyncScheduler,
    private val healthKitAuthManager: HuaweiHealthKitAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<SettingsSideEffect>()
    val sideEffect: SharedFlow<SettingsSideEffect> = _sideEffect.asSharedFlow()

    // Mutex for thread-safe state updates
    private val stateMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(
                _state, syncPreferencesManager.preferencesFlow
            ) { uiState, prefs ->
                uiState to prefs
            }.collect { (uiState, prefs) ->
                stateMutex.withLock {
                    if (uiState is SettingsUiState.Idle) {
                        _state.value = uiState.copy(syncPreferences = prefs)
                    }
                }
            }
        }

        // Also collect Health Kit auth state
        viewModelScope.launch {
            healthKitAuthManager.authState.collect { authState ->
                stateMutex.withLock {
                    val current = _state.value
                    if (current is SettingsUiState.Idle) {
                        _state.value = current.copy(
                            healthKitAuthState = authState,
                            healthKitSignedIn = authState == AuthState.SIGNED_IN
                        )
                    }
                }
            }
        }
    }

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            // Health Connect intents
            is SettingsIntent.CheckHealthConnect -> checkHealthConnectStatus()
            is SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.RequestHealthConnectPermissions -> requestHealthConnectPermissions()
            is SettingsIntent.PermissionsRequested -> checkHealthConnectStatus()
            is SettingsIntent.SetMasterSync -> viewModelScope.launch { setMasterSync(intent.enabled) }
            is SettingsIntent.SetStepsSync -> viewModelScope.launch { setStepsSync(intent.enabled) }
            is SettingsIntent.SetExerciseSync -> viewModelScope.launch { setExerciseSync(intent.enabled) }
            is SettingsIntent.SetHeartRateSync -> viewModelScope.launch { setHeartRateSync(intent.enabled) }

            // Health Kit intents (Phase 4)
            is SettingsIntent.CheckHealthKitStatus -> checkHealthKitStatus()
            is SettingsIntent.ConnectHealthKit -> connectHealthKit()
            is SettingsIntent.DisconnectHealthKit -> disconnectHealthKit()
            is SettingsIntent.SetSyncWindow -> viewModelScope.launch { setSyncWindow(intent.days) }
            is SettingsIntent.SyncHealthKitNow -> syncHealthKitNow()
        }
    }

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

    private suspend fun setSyncWindow(days: Int) {
        syncPreferencesManager.setSyncWindowDays(days)
        _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
            healthKitSyncWindowDays = days
        ) ?: _state.value

        _sideEffect.emit(
            SettingsSideEffect.ShowSuccess(
                "Sync window updated to ${
                    SyncPreferencesManager.getSyncWindowLabel(
                        days
                    )
                }"
            )
        )
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

    private fun requestHealthKitSignIn() {
        viewModelScope.launch {
            _sideEffect.emit(SettingsSideEffect.RequestHealthKitSignIn)
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

    // Health Kit status check (Phase 4)
    private fun checkHealthKitStatus() {
        viewModelScope.launch {
            val isSignedIn = healthKitAuthManager.isSignedIn()
            val authState = when {
                !isSignedIn -> AuthState.NOT_SIGNED_IN
                else -> {
                    val expiry = healthKitAuthManager.getTokenExpiry()
                    if (System.currentTimeMillis() >= expiry) AuthState.TOKEN_EXPIRED else AuthState.SIGNED_IN
                }
            }

            val syncWindowDays = syncPreferencesManager.getSyncWindowDays()
            val lastSyncTime = syncPreferencesManager.getLastHealthKitSyncTime()

            _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                healthKitSignedIn = isSignedIn,
                healthKitAuthState = authState,
                healthKitSyncWindowDays = syncWindowDays,
                lastHealthKitSyncTime = lastSyncTime
            ) ?: SettingsUiState.Idle(
                healthKitSignedIn = isSignedIn,
                healthKitAuthState = authState,
                healthKitSyncWindowDays = syncWindowDays,
                lastHealthKitSyncTime = lastSyncTime
            )
        }
    }

    // Connect to Health Kit (initiate sign-in)
    private fun connectHealthKit() {
        viewModelScope.launch {
            _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                isHealthKitSyncing = true
            ) ?: _state.value

            when (val result = healthKitAuthManager.signIn()) {
                is com.gomaa.healthy.data.healthkit.HealthKitAuthResult.Success -> {
                    _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                        healthKitSignedIn = true,
                        healthKitAuthState = AuthState.SIGNED_IN,
                        isHealthKitSyncing = false
                    ) ?: _state.value

                    _sideEffect.emit(SettingsSideEffect.ShowSuccess("Connected to Huawei Health Kit"))

                    // Start the sync worker
                    HuaweiHealthKitScheduler.schedule(context)
                }

                is com.gomaa.healthy.data.healthkit.HealthKitAuthResult.Error -> {
                    _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                        isHealthKitSyncing = false
                    ) ?: _state.value

                    _sideEffect.emit(SettingsSideEffect.ShowError("Failed to connect: ${result.message}"))
                }
            }
        }
    }

    // Disconnect from Health Kit
    private fun disconnectHealthKit() {
        viewModelScope.launch {
            healthKitAuthManager.signOut()
            HuaweiHealthKitScheduler.cancel(context)

            _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                healthKitSignedIn = false,
                healthKitAuthState = AuthState.NOT_SIGNED_IN
            ) ?: _state.value

            _sideEffect.emit(SettingsSideEffect.ShowSuccess("Disconnected from Huawei Health Kit"))
        }
    }

    // Trigger immediate Health Kit sync
    private fun syncHealthKitNow() {
        viewModelScope.launch {
            _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                isHealthKitSyncing = true
            ) ?: _state.value

            HuaweiHealthKitScheduler.runImmediate(context)

            // Give it a moment then update UI
            kotlinx.coroutines.delay(2000)

            val lastSyncTime = syncPreferencesManager.getLastHealthKitSyncTime()

            _state.value = (_state.value as? SettingsUiState.Idle)?.copy(
                isHealthKitSyncing = false,
                lastHealthKitSyncTime = lastSyncTime
            ) ?: _state.value

            _sideEffect.emit(SettingsSideEffect.ShowSuccess("Health Kit sync started"))
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
