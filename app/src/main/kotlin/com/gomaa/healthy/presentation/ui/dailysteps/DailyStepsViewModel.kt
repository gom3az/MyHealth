package com.gomaa.healthy.presentation.ui.dailysteps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.usecase.GetPaginatedBySourceDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.SourceFilterOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DailyStepsState {
    object Loading : DailyStepsState
    data class Loaded(
        val sourceFilter: String? = null,
        val availableFilters: List<SourceFilterOption> = emptyList(),
        val isSyncing: Boolean = false,
    ) : DailyStepsState

    data class Error(val message: String) : DailyStepsState
}

sealed interface DailyStepsIntent {
    data object LoadData : DailyStepsIntent
    data object Refresh : DailyStepsIntent
}

sealed interface DailyStepsEffect {
    data class ShowError(val message: String) : DailyStepsEffect
}

@HiltViewModel
class DailyStepsViewModel @Inject constructor(
    private val dailyStepsUseCase: GetPaginatedBySourceDailyStepsUseCase
) : ViewModel() {

    private val _effect = MutableSharedFlow<DailyStepsEffect>()
    val effect: SharedFlow<DailyStepsEffect> = _effect.asSharedFlow()

    private val _uiState = MutableStateFlow<DailyStepsState>(DailyStepsState.Loading)
    val uiState: StateFlow<DailyStepsState> = _uiState.asStateFlow()

    fun handleIntent(intent: DailyStepsIntent) {
        when (intent) {
            DailyStepsIntent.LoadData -> {
                viewModelScope.launch {
                    val data = dailyStepsUseCase() // Todo add source later

                    _uiState.value = DailyStepsState.Loaded(
                        sourceFilter = null,
                        availableFilters = emptyList(),
                        isSyncing = false,
                    )
                }
            }

            DailyStepsIntent.Refresh -> {

            }
        }
    }


}