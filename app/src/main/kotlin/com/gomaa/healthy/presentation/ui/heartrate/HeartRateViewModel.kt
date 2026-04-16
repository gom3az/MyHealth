package com.gomaa.healthy.presentation.ui.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gomaa.healthy.data.repository.HealthConnectRepositoryInterface
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.usecase.GetAvailableSourcesUseCase
import com.gomaa.healthy.domain.usecase.GetHeartRateSummaryUseCase
import com.gomaa.healthy.domain.usecase.GetRecentHeartRateReadingsUseCase
import com.gomaa.healthy.domain.usecase.HourHeader
import com.gomaa.healthy.domain.usecase.SourceFilterOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HeartRateUiState {
    data object Loading : HeartRateUiState()
    data class Loaded(
        val overallSummary: HeartRateSummary?,
        val sourceFilter: String? = null,
        val availableFilters: List<SourceFilterOption> = emptyList(),
        val isSyncing: Boolean = false,
    ) : HeartRateUiState()

    data object Empty : HeartRateUiState()
    data class Error(val message: String) : HeartRateUiState()
}

sealed class HeartRateIntent {
    data object OnLoadData : HeartRateIntent()
    data object OnRefresh : HeartRateIntent()
    data object OnSync : HeartRateIntent()
    data class OnSourceFilterChanged(val filter: String?) : HeartRateIntent()
}

sealed class HeartRateEffect {
    data class ShowError(val message: String) : HeartRateEffect()
    data class ShowSuccess(val message: String) : HeartRateEffect()
}

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val getHeartRateSummaryUseCase: GetHeartRateSummaryUseCase,
    private val getRecentHeartRateReadingsUseCase: GetRecentHeartRateReadingsUseCase,
    private val getAvailableSourcesUseCase: GetAvailableSourcesUseCase,
    private val healthConnectRepository: HealthConnectRepositoryInterface
) : ViewModel() {

    private val _internalState = MutableStateFlow(InternalState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<HourHeader>> = _internalState
        .map { it.selectedSource }
        .distinctUntilChanged()
        .flatMapLatest { source ->
            getRecentHeartRateReadingsUseCase(source = source?.let { parseSource(it) })
        }
        .cachedIn(viewModelScope) // Caches the raw data from the DB

    // The rest of your UI state (including expandedHours)
    val uiState = _internalState.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeartRateUiState.Loading)

    private val _effect = MutableSharedFlow<HeartRateEffect>()
    val effect: SharedFlow<HeartRateEffect> = _effect.asSharedFlow()

    fun processIntent(intent: HeartRateIntent) {
        _internalState.update { current ->
            when (intent) {
                is HeartRateIntent.OnLoadData -> {
                    loadData()
                    current
                }

                is HeartRateIntent.OnRefresh -> {
                    loadData()
                    current
                }

                is HeartRateIntent.OnSync -> {
                    syncData()
                    current
                }

                is HeartRateIntent.OnSourceFilterChanged -> {
                    current.copy(selectedSource = intent.filter)
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _internalState.update { it.copy(loadingState = LoadingState.Loading) }
            try {
                val summary = getHeartRateSummaryUseCase()
                val availableFilters = getAvailableSourcesUseCase()

                if (summary != null || availableFilters.isNotEmpty()) {
                    _internalState.update {
                        it.copy(
                            loadingState = LoadingState.Loaded,
                            overallSummary = summary,
                            availableFilters = availableFilters
                        )
                    }
                } else {
                    _internalState.update { it.copy(loadingState = LoadingState.Empty) }
                }
            } catch (e: Exception) {
                _internalState.update {
                    it.copy(loadingState = LoadingState.Error(e.message ?: "Unknown error"))
                }
                _effect.emit(
                    HeartRateEffect.ShowError(
                        e.message ?: "Failed to load heart rate data"
                    )
                )
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            val currentState = _internalState.value.loadingState
            if (currentState == LoadingState.Loaded) {
                _internalState.update { it.copy(isSyncing = true) }
                try {
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
                    loadData()
                } catch (e: Exception) {
                    _effect.emit(HeartRateEffect.ShowError(e.message ?: "Sync failed"))
                } finally {
                    _internalState.update { it.copy(isSyncing = false) }
                }
            }
        }
    }

    private fun parseSource(sourceString: String): HeartRateSource? {
        return when (sourceString.lowercase()) {
            "health_connect" -> HeartRateSource.HEALTH_CONNECT
            "wearable_huawei_cloud" -> HeartRateSource.WEARABLE_HUAWEI_CLOUD
            else -> null
        }
    }

    private sealed class LoadingState {
        data object Loading : LoadingState()
        data object Loaded : LoadingState()
        data object Empty : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    private data class InternalState(
        val selectedSource: String? = null,
        val overallSummary: HeartRateSummary? = null,
        val availableFilters: List<SourceFilterOption> = emptyList(),
        val isSyncing: Boolean = false,
        val loadingState: LoadingState = LoadingState.Loading
    )

    private fun InternalState.toUiState(): HeartRateUiState {
        return when (val state = loadingState) {
            is LoadingState.Loading -> HeartRateUiState.Loading
            is LoadingState.Loaded -> HeartRateUiState.Loaded(
                overallSummary = overallSummary,
                sourceFilter = selectedSource,
                availableFilters = availableFilters,
                isSyncing = isSyncing,
            )

            is LoadingState.Empty -> HeartRateUiState.Empty
            is LoadingState.Error -> HeartRateUiState.Error(state.message)
        }
    }
}