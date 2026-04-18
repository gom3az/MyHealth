package com.gomaa.healthy.presentation.ui.heartrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gomaa.healthy.data.repository.HealthConnectRepositoryInterface
import com.gomaa.healthy.domain.model.DateRangeFilter
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.model.ReadingSource
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.domain.usecase.GetAvailableSourcesUseCase
import com.gomaa.healthy.domain.usecase.GetHeartRateSummaryUseCase
import com.gomaa.healthy.domain.usecase.GetRecentHeartRateReadingsUseCase
import com.gomaa.healthy.domain.usecase.HourHeader
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
        val dateFilter: DateRangeFilter = DateRangeFilter.All,
        val availableFilters: List<SourceFilterOption> = emptyList(),
        val isSyncing: Boolean = false,
    ) : HeartRateUiState()

    data object Empty : HeartRateUiState()
    data class Error(val message: String) : HeartRateUiState()
}

sealed class HeartRateIntent {
    data object OnLoadData : HeartRateIntent()
    data object OnRefresh : HeartRateIntent()
    data class OnSourceFilterChanged(val filter: String?) : HeartRateIntent()
    data class OnDateFilterChanged(val filter: DateRangeFilter) : HeartRateIntent()
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
        .map { it.selectedSource to it.selectedDateRange }
        .distinctUntilChanged()
        .flatMapLatest { (source, dateRange) ->
            getRecentHeartRateReadingsUseCase(
                source = source?.let { parseSource(it) },
                dateRange = dateRange
            )
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

                is HeartRateIntent.OnSourceFilterChanged -> {
                    current.copy(selectedSource = intent.filter)
                }

                is HeartRateIntent.OnDateFilterChanged -> {
                    current.copy(selectedDateRange = intent.filter)
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

    private fun parseSource(sourceString: String): ReadingSource? {
        return ReadingSource.fromDbString(sourceString)
    }

    private sealed class LoadingState {
        data object Loading : LoadingState()
        data object Loaded : LoadingState()
        data object Empty : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    private data class InternalState(
        val selectedSource: String? = null,
        val selectedDateRange: DateRangeFilter = DateRangeFilter.All,
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
                dateFilter = selectedDateRange,
                availableFilters = availableFilters,
                isSyncing = isSyncing,
            )

            is LoadingState.Empty -> HeartRateUiState.Empty
            is LoadingState.Error -> HeartRateUiState.Error(state.message)
        }
    }
}