package com.gomaa.healthy.presentation.ui.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.data.repository.HealthConnectRepositoryInterface
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.usecase.GetHeartRateSummaryUseCase
import com.gomaa.healthy.domain.usecase.GetLatestHeartRateUseCase
import com.gomaa.healthy.domain.usecase.GetRecentHeartRateReadingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

typealias HourlyReadings = Map<Int, List<HeartRateReading>>

enum class HeartRateSourceFilter {
    ALL, MY_HEALTH, HEALTH_CONNECT
}

sealed class HeartRateUiState {
    data object Loading : HeartRateUiState()
    data class Loaded(
        val todaySummary: HeartRateSummary?,
        val recentReadings: List<HeartRateReading>,
        val hourlyReadings: HourlyReadings,
        val sourceFilter: HeartRateSourceFilter = HeartRateSourceFilter.ALL,
        val isSyncing: Boolean = false
    ) : HeartRateUiState()

    data object Empty : HeartRateUiState()
    data class Error(val message: String) : HeartRateUiState()
}

sealed class HeartRateIntent {
    data object OnLoadData : HeartRateIntent()
    data object OnRefresh : HeartRateIntent()
    data object OnSync : HeartRateIntent()
    data class OnSourceFilterChanged(val filter: HeartRateSourceFilter) : HeartRateIntent()
}

sealed class HeartRateEffect {
    data class ShowError(val message: String) : HeartRateEffect()
    data class ShowSuccess(val message: String) : HeartRateEffect()
}

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val getLatestHeartRateUseCase: GetLatestHeartRateUseCase,
    private val getHeartRateSummaryUseCase: GetHeartRateSummaryUseCase,
    private val getRecentHeartRateReadingsUseCase: GetRecentHeartRateReadingsUseCase,
    private val healthConnectRepository: HealthConnectRepositoryInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow<HeartRateUiState>(HeartRateUiState.Loading)
    val uiState: StateFlow<HeartRateUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HeartRateEffect>()
    val effect: SharedFlow<HeartRateEffect> = _effect.asSharedFlow()

    init {
        processIntent(HeartRateIntent.OnLoadData)
    }

    fun processIntent(intent: HeartRateIntent) {
        when (intent) {
            is HeartRateIntent.OnLoadData -> loadData()
            is HeartRateIntent.OnRefresh -> loadData()
            is HeartRateIntent.OnSync -> syncData()
            is HeartRateIntent.OnSourceFilterChanged -> updateFilter(intent.filter)
        }
    }

    private fun loadData(filter: HeartRateSourceFilter = HeartRateSourceFilter.ALL) {
        viewModelScope.launch {
            _uiState.value = HeartRateUiState.Loading
            try {
                val startOfDay =
                    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay =
                    LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()

                val summary = getHeartRateSummaryUseCase(startOfDay, endOfDay)

                val source = when (filter) {
                    HeartRateSourceFilter.ALL -> null
                    HeartRateSourceFilter.MY_HEALTH -> HeartRateSource.MY_HEALTH
                    HeartRateSourceFilter.HEALTH_CONNECT -> HeartRateSource.HEALTH_CONNECT
                }
                val recentReadings = getRecentHeartRateReadingsUseCase(limit = 20, source = source)

                if (recentReadings.isNotEmpty() || summary != null) {
                    _uiState.value = HeartRateUiState.Loaded(
                        todaySummary = summary,
                        recentReadings = recentReadings,
                        hourlyReadings = groupReadingsByHour(recentReadings),
                        sourceFilter = filter
                    )
                } else {
                    _uiState.value = HeartRateUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = HeartRateUiState.Error(e.message ?: "Unknown error")
                _effect.emit(
                    HeartRateEffect.ShowError(
                        e.message ?: "Failed to load heart rate data"
                    )
                )
            }
        }
    }

    // HC-063: Implement sync button to call syncHeartRates()
    private fun syncData() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HeartRateUiState.Loaded) {
                _uiState.value = currentState.copy(isSyncing = true)
                try {
                    // Call Health Connect sync
                    when (val result = healthConnectRepository.syncHeartRates()) {
                        is HealthConnectResult.Success -> {
                            _effect.emit(HeartRateEffect.ShowSuccess("Synced ${result.data} heart rate records"))
                        }

                        is HealthConnectResult.Error -> {
                            _effect.emit(
                                HeartRateEffect.ShowError(
                                    result.exception.message ?: "Sync failed"
                                )
                            )
                        }
                    }
                    // Reload data after sync
                    loadData()
                } catch (e: Exception) {
                    _effect.emit(HeartRateEffect.ShowError(e.message ?: "Sync failed"))
                } finally {
                    val updatedState = _uiState.value
                    if (updatedState is HeartRateUiState.Loaded) {
                        _uiState.value = updatedState.copy(isSyncing = false)
                    }
                }
            }
        }
    }

    private fun updateFilter(filter: HeartRateSourceFilter) {
        loadData(filter)
    }

    private fun groupReadingsByHour(readings: List<HeartRateReading>): HourlyReadings {
        return readings
            .map { reading ->
                val hour = Instant.ofEpochMilli(reading.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .hour
                hour to reading
            }
            .groupBy(
                keySelector = { (hour, _) -> hour },
                valueTransform = { (_, reading) -> reading }
            )
            .toSortedMap()
    }
}
