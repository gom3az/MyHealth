package com.gomaa.healthy.presentation.ui.dailysteps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gomaa.healthy.data.worker.HealthConnectSyncScheduler
import com.gomaa.healthy.domain.model.DailySteps
import com.gomaa.healthy.domain.model.ReadingSource
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.domain.usecase.GetPaginatedBySourceDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.GetStepsAvailableFiltersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DailyStepsState {
    object Loading : DailyStepsState
    data class Loaded(
        val sourceFilter: String? = null,
        val isSyncing: Boolean = false,
        val availableFilters: List<SourceFilterOption> = emptyList(),
    ) : DailyStepsState

    data object Empty : DailyStepsState
    data class Error(val message: String) : DailyStepsState
}

sealed interface DailyStepsIntent {
    data object LoadData : DailyStepsIntent
    data object Refresh : DailyStepsIntent
    data class SourceFilterChanged(val filter: String?) : DailyStepsIntent
}

sealed interface DailyStepsEffect {
    data class ShowError(val message: String) : DailyStepsEffect
    data class ShowSuccess(val message: String) : DailyStepsEffect
}

private data class DailyStepsFilter(
    val sourceFilter: String? = null
)

@HiltViewModel
class DailyStepsViewModel @Inject constructor(
    private val getPaginatedBySourceDailyStepsUseCase: GetPaginatedBySourceDailyStepsUseCase,
    private val getStepsAvailableFiltersUseCase: GetStepsAvailableFiltersUseCase,
    private val healthConnectSyncScheduler: HealthConnectSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyStepsState>(DailyStepsState.Loading)
    val uiState: StateFlow<DailyStepsState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DailyStepsEffect>()
    val effect: SharedFlow<DailyStepsEffect> = _effect.asSharedFlow()

    private val _filter = MutableStateFlow(DailyStepsFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<DailySteps>> =
        _filter.map { it.sourceFilter } // Convert filter to a Flow
            .flatMapLatest { source ->
                getPaginatedBySourceDailyStepsUseCase(source?.let {
                    ReadingSource.fromDbString(it) // Convert String to ReadingSource
                })
            }.cachedIn(viewModelScope)

    fun handleIntent(intent: DailyStepsIntent) {
        when (intent) {
            is DailyStepsIntent.LoadData -> loadData()
            is DailyStepsIntent.Refresh -> refreshData()
            is DailyStepsIntent.SourceFilterChanged -> {
                _filter.value = _filter.value.copy(sourceFilter = intent.filter)
                updateStateWithFilter(intent.filter)
            }
        }
    }

    private fun updateStateWithFilter(filter: String?) {
        _uiState.update { current ->
            if (current is DailyStepsState.Loaded) {
                current.copy(sourceFilter = filter)
            } else {
                DailyStepsState.Loaded(sourceFilter = filter)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = DailyStepsState.Loading
            try {
                val availableFilters = getStepsAvailableFiltersUseCase()

                _uiState.value = DailyStepsState.Loaded(
                    sourceFilter = _filter.value.sourceFilter,
                    availableFilters = availableFilters,
                    isSyncing = false
                )
            } catch (e: Exception) {
                _uiState.value = DailyStepsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { current ->
                if (current is DailyStepsState.Loaded) current.copy(isSyncing = true)
                else DailyStepsState.Loaded(isSyncing = true)
            }
            try {
                healthConnectSyncScheduler.enqueueImmediateSync(
                    masterSyncEnabled = true,
                    syncStepsEnabled = true,
                    syncExerciseEnabled = false,
                    syncHeartRateEnabled = false
                )
                _effect.emit(DailyStepsEffect.ShowSuccess("Sync started"))
            } catch (e: Exception) {
                _effect.emit(DailyStepsEffect.ShowError(e.message ?: "Failed to start sync"))
            } finally {
                _uiState.update { current ->
                    if (current is DailyStepsState.Loaded) current.copy(isSyncing = false)
                    else DailyStepsState.Loaded(isSyncing = false)
                }
            }
        }
    }
}