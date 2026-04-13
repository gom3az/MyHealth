package com.gomaa.healthy.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.model.HeartRateRecord
import com.gomaa.healthy.domain.usecase.ConnectWearableUseCase
import com.gomaa.healthy.domain.usecase.DisconnectWearableUseCase
import com.gomaa.healthy.domain.usecase.SaveSessionUseCase
import com.gomaa.healthy.domain.usecase.SelectWearableProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class HeartRateZone {
    REST, LOW, MODERATE, HIGH, VERY_HIGH
}

data class DashboardUiState(
    val isTracking: Boolean = false,
    val heartRate: Int = 0,
    val heartRateZone: HeartRateZone = HeartRateZone.REST,
    val elapsedTime: Long = 0,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val deviceBrand: String? = null,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val minHeartRate: Int = 0
)

sealed class DashboardIntent {
    data object OnStartTracking : DashboardIntent()
    data object OnStopTracking : DashboardIntent()
    data object OnTick : DashboardIntent()
}

sealed class DashboardEffect {
    data object ShowSaveSuccess : DashboardEffect()
    data object NavigateToAnalytics : DashboardEffect()
    data class ShowError(val message: String) : DashboardEffect()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val selectWearableProviderUseCase: SelectWearableProviderUseCase,
    private val connectWearableUseCase: ConnectWearableUseCase,
    private val disconnectWearableUseCase: DisconnectWearableUseCase,
    private val saveSessionUseCase: SaveSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DashboardEffect>()
    val effect: SharedFlow<DashboardEffect> = _effect.asSharedFlow()

    private val heartRateSamples = mutableListOf<Int>()
    private var sessionStartTime: Long = 0

    private var heartRateJob: Job? = null
    private var connectionJob: Job? = null
    private var timerJob: Job? = null

    init {
        observeWearableData()
    }

    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.OnStartTracking -> startTracking()
            is DashboardIntent.OnStopTracking -> stopTracking()
            is DashboardIntent.OnTick -> { /* Timer now handled internally */
            }
        }
    }

    private fun observeWearableData() {
        viewModelScope.launch {
            selectWearableProviderUseCase.selectedProvider.collectLatest { provider ->
                _uiState.value = _uiState.value.copy(deviceBrand = provider?.brand)

                heartRateJob?.cancel()
                connectionJob?.cancel()

                provider?.let { p ->
                    heartRateJob = viewModelScope.launch {
                        p.heartRateFlow().collectLatest { hr ->
                            if (_uiState.value.isTracking) {
                                heartRateSamples.add(hr)
                                updateHeartRateStats(hr)
                            }
                            _uiState.value = _uiState.value.copy(
                                heartRate = hr,
                                heartRateZone = calculateZone(hr)
                            )
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

    private fun calculateZone(hr: Int): HeartRateZone {
        return when {
            hr < 60 -> HeartRateZone.REST
            hr < 100 -> HeartRateZone.LOW
            hr < 130 -> HeartRateZone.MODERATE
            hr < 160 -> HeartRateZone.HIGH
            else -> HeartRateZone.VERY_HIGH
        }
    }

    private fun updateHeartRateStats(hr: Int) {
        val samples = heartRateSamples
        val max = samples.maxOrNull() ?: hr
        val min = samples.minOrNull() ?: hr
        val avg = if (samples.isNotEmpty()) samples.average().toInt() else hr

        _uiState.value = _uiState.value.copy(
            maxHeartRate = max,
            minHeartRate = min,
            avgHeartRate = avg
        )
    }

    private fun startTracking() {
        sessionStartTime = System.currentTimeMillis()
        heartRateSamples.clear()
        _uiState.value = _uiState.value.copy(
            isTracking = true,
            elapsedTime = 0,
            avgHeartRate = 0,
            maxHeartRate = 0,
            minHeartRate = 0
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTracking) {
                delay(1000)
                _uiState.value = _uiState.value.copy(elapsedTime = _uiState.value.elapsedTime + 1)
            }
        }

        viewModelScope.launch {
            connectWearableUseCase("device-1")
        }
    }

    private fun stopTracking() {
        timerJob?.cancel()
        viewModelScope.launch {
            disconnectWearableUseCase()

            val endTime = System.currentTimeMillis()
            val session = ExerciseSession(
                id = UUID.randomUUID().toString(),
                startTime = sessionStartTime,
                endTime = endTime,
                avgHeartRate = _uiState.value.avgHeartRate,
                maxHeartRate = _uiState.value.maxHeartRate,
                minHeartRate = _uiState.value.minHeartRate,
                deviceBrand = _uiState.value.deviceBrand ?: "Unknown",
                heartRates = heartRateSamples.mapIndexed { index, hr ->
                    HeartRateRecord(
                        timestamp = sessionStartTime + (index * 1000L),
                        bpm = hr
                    )
                }
            )

            val result = saveSessionUseCase(session)
            if (result.isSuccess) {
                _effect.emit(DashboardEffect.ShowSaveSuccess)
                _effect.emit(DashboardEffect.NavigateToAnalytics)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to save session"
                _effect.emit(DashboardEffect.ShowError(errorMsg))
            }

            _uiState.value = _uiState.value.copy(isTracking = false)
            heartRateSamples.clear()
        }
    }
}