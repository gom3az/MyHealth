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
import kotlinx.coroutines.coroutineScope
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
    // Initialization
    data object Initialize : SettingsIntent()

    // Health Connect intents
    data object SyncNow : SettingsIntent()
    data object RequestHealthConnectPermissions : SettingsIntent()
    data object PermissionsRequested : SettingsIntent()
    data class SetMasterSync(val enabled: Boolean) : SettingsIntent()
    data class SetStepsSync(val enabled: Boolean) : SettingsIntent()
    data class SetExerciseSync(val enabled: Boolean) : SettingsIntent()
    data class SetHeartRateSync(val enabled: Boolean) : SettingsIntent()

    // Health Kit intents (Phase 4)
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

private enum class SyncType {
    MASTER_SYNC, STEPS_SYNC, EXERCISE_SYNC, HEART_RATE_SYNC
}

private data class HealthConnectState(
    val isAvailable: Boolean,
    val isConnected: Boolean,
    val stepCount: Int,
    val exerciseCount: Int,
    val heartRateCount: Int,
    val lastSyncTime: Long?
)

private data class HealthKitState(
    val isSignedIn: Boolean,
    val authState: AuthState,
    val syncWindowDays: Int,
    val lastSyncTime: Long?
)

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

    private companion object {
        private const val HEALTH_KIT_SYNC_CHECK_DELAY_MS = 2000L
    }

    init {
        viewModelScope.launch {
            combine(
                syncPreferencesManager.preferencesFlow, healthKitAuthManager.authState
            ) { prefs, authState -> prefs to authState }.collect { (prefs, authState) ->
                updateIfIdle {
                    copy(
                        syncPreferences = prefs,
                        healthKitAuthState = authState,
                        healthKitSignedIn = authState == AuthState.SIGNED_IN
                    )
                }
            }
        }
    }

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            // Initialization
            is SettingsIntent.Initialize -> initialize()

            // Health Connect intents
            is SettingsIntent.SyncNow -> syncNow()
            is SettingsIntent.RequestHealthConnectPermissions -> requestHealthConnectPermissions()
            is SettingsIntent.PermissionsRequested -> checkHealthConnectStatus()
            is SettingsIntent.SetMasterSync -> viewModelScope.launch {
                setSyncEnabled(
                    SyncType.MASTER_SYNC, intent.enabled
                )
            }

            is SettingsIntent.SetStepsSync -> viewModelScope.launch {
                setSyncEnabled(
                    SyncType.STEPS_SYNC, intent.enabled
                )
            }

            is SettingsIntent.SetExerciseSync -> viewModelScope.launch {
                setSyncEnabled(
                    SyncType.EXERCISE_SYNC, intent.enabled
                )
            }

            is SettingsIntent.SetHeartRateSync -> viewModelScope.launch {
                setSyncEnabled(
                    SyncType.HEART_RATE_SYNC, intent.enabled
                )
            }

            // Health Kit intents (Phase 4)
            is SettingsIntent.ConnectHealthKit -> connectHealthKit()
            is SettingsIntent.DisconnectHealthKit -> disconnectHealthKit()
            is SettingsIntent.SetSyncWindow -> viewModelScope.launch { setSyncWindow(intent.days) }
            is SettingsIntent.SyncHealthKitNow -> syncHealthKitNow()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val healthConnectDeferred = async { checkHealthConnectInternal() }
                    val healthKitDeferred = async { checkHealthKitInternal() }

                    val healthConnectState = healthConnectDeferred.await()
                    val healthKitState = healthKitDeferred.await()

                    _state.value = SettingsUiState.Idle(
                        isAvailable = healthConnectState.isAvailable,
                        isConnected = healthConnectState.isConnected,
                        stepCount = healthConnectState.stepCount,
                        exerciseSessionCount = healthConnectState.exerciseCount,
                        heartRateCount = healthConnectState.heartRateCount,
                        lastSyncTime = healthConnectState.lastSyncTime,
                        healthKitSignedIn = healthKitState.isSignedIn,
                        healthKitAuthState = healthKitState.authState,
                        healthKitSyncWindowDays = healthKitState.syncWindowDays,
                        lastHealthKitSyncTime = healthKitState.lastSyncTime
                    )
                }
            } catch (e: Exception) {
                _sideEffect.emit(SettingsSideEffect.ShowError("Failed to initialize: ${e.message}"))
            }
        }
    }

    private suspend fun checkHealthConnectInternal(): HealthConnectState {
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

        val stepCount: Int
        val exerciseCount: Int
        val heartRateCount: Int
        val lastSyncTime: Long?

        if (isConnected) {
            val stepDeferred = viewModelScope.async { healthConnectRepository.getStepCount() }
            val exerciseDeferred =
                viewModelScope.async { healthConnectRepository.getExerciseSessionCount() }
            val heartRateDeferred =
                viewModelScope.async { healthConnectRepository.getHeartRateCount() }
            val lastSyncDeferred =
                viewModelScope.async { healthConnectRepository.getLastSyncTime() }

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

        return HealthConnectState(
            isAvailable = isAvailable,
            isConnected = isConnected,
            stepCount = stepCount,
            exerciseCount = exerciseCount,
            heartRateCount = heartRateCount,
            lastSyncTime = lastSyncTime
        )
    }

    private suspend fun checkHealthKitInternal(): HealthKitState {
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

        return HealthKitState(
            isSignedIn = isSignedIn,
            authState = authState,
            syncWindowDays = syncWindowDays,
            lastSyncTime = lastSyncTime
        )
    }

    private fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val healthConnectState = checkHealthConnectInternal()

            updateIfIdle {
                copy(
                    isAvailable = healthConnectState.isAvailable,
                    isConnected = healthConnectState.isConnected,
                    stepCount = healthConnectState.stepCount,
                    exerciseSessionCount = healthConnectState.exerciseCount,
                    heartRateCount = healthConnectState.heartRateCount,
                    lastSyncTime = healthConnectState.lastSyncTime
                )
            }
        }
    }

    private suspend fun setSyncEnabled(type: SyncType, enabled: Boolean) {
        when (type) {
            SyncType.MASTER_SYNC -> {
                syncPreferencesManager.setMasterSyncEnabled(enabled)
                updateScheduler(enabled = enabled)
            }

            SyncType.STEPS_SYNC -> {
                syncPreferencesManager.setStepsSyncEnabled(enabled)
                updateScheduler()
            }

            SyncType.EXERCISE_SYNC -> {
                syncPreferencesManager.setExerciseSyncEnabled(enabled)
                updateScheduler()
            }

            SyncType.HEART_RATE_SYNC -> {
                syncPreferencesManager.setHeartRateSyncEnabled(enabled)
                updateScheduler()
            }
        }
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

    // Connect to Health Kit (initiate sign-in)
    private fun connectHealthKit() {
        viewModelScope.launch {
            setHealthKitSyncing(true)

            when (val result = healthKitAuthManager.signIn()) {
                is com.gomaa.healthy.data.healthkit.HealthKitAuthResult.Success -> {
                    updateIfIdle {
                        copy(
                            healthKitSignedIn = true,
                            healthKitAuthState = AuthState.SIGNED_IN,
                            isHealthKitSyncing = false
                        )
                    }
                    _sideEffect.emit(SettingsSideEffect.ShowSuccess("Connected to Huawei Health Kit"))
                    HuaweiHealthKitScheduler.schedule(context)
                }

                is com.gomaa.healthy.data.healthkit.HealthKitAuthResult.Error -> {
                    setHealthKitSyncing(false)
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

            updateIfIdle {
                copy(
                    healthKitSignedIn = false, healthKitAuthState = AuthState.NOT_SIGNED_IN
                )
            }
            _sideEffect.emit(SettingsSideEffect.ShowSuccess("Disconnected from Huawei Health Kit"))
        }
    }

    // Trigger immediate Health Kit sync
    private fun syncHealthKitNow() {
        viewModelScope.launch {
            setHealthKitSyncing(true)
            HuaweiHealthKitScheduler.runImmediate(context)
            kotlinx.coroutines.delay(HEALTH_KIT_SYNC_CHECK_DELAY_MS)
            val lastSyncTime = syncPreferencesManager.getLastHealthKitSyncTime()
            updateIfIdle {
                copy(
                    isHealthKitSyncing = false, lastHealthKitSyncTime = lastSyncTime
                )
            }
            _sideEffect.emit(SettingsSideEffect.ShowSuccess("Health Kit sync started"))
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            setSyncing(true)

            val prefs = syncPreferencesManager.getPreferences()

            val workId = syncScheduler.enqueueImmediateSync(
                masterSyncEnabled = prefs.masterSyncEnabled,
                syncStepsEnabled = prefs.syncStepsEnabled,
                syncExerciseEnabled = prefs.syncExerciseEnabled,
                syncHeartRateEnabled = prefs.syncHeartRateEnabled
            )

            workManager.getWorkInfoByIdFlow(workId).first { it?.state?.isFinished == true }
                ?.let { finishedWorkInfo ->
                    when (finishedWorkInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val stepCount = healthConnectRepository.getStepCount()
                            val exerciseCount = healthConnectRepository.getExerciseSessionCount()
                            val heartRateCount = healthConnectRepository.getHeartRateCount()
                            val lastSyncTime = healthConnectRepository.getLastSyncTime()

                            updateIfIdle {
                                copy(
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
                        }

                        WorkInfo.State.FAILED -> {
                            setSyncing(false)
                            _sideEffect.emit(
                                SettingsSideEffect.ShowError(
                                    "Sync failed. Please check Health Connect permissions and try again."
                                )
                            )
                        }

                        WorkInfo.State.CANCELLED -> {
                            setSyncing(false)
                            _sideEffect.emit(SettingsSideEffect.ShowError("Sync was cancelled"))
                        }

                        else -> { /* Handle other states */
                        }
                    }
                }
        }
    }

    private fun setSyncing(syncing: Boolean) {
        updateIfIdle { copy(isSyncing = syncing) }
    }

    private fun setHealthKitSyncing(syncing: Boolean) {
        updateIfIdle { copy(isHealthKitSyncing = syncing) }
    }

    private fun updateIfIdle(transform: SettingsUiState.Idle.() -> SettingsUiState) {
        val current = _state.value
        if (current is SettingsUiState.Idle) {
            _state.value = current.transform()
        }
    }
}