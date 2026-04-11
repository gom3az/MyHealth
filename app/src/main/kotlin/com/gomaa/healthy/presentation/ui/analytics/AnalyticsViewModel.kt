package com.gomaa.healthy.presentation.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val sessions: List<ExerciseSession> = emptyList()
)

sealed class AnalyticsIntent {
    data object OnLoadSessions : AnalyticsIntent()
    data object OnRefresh : AnalyticsIntent()
}

sealed class AnalyticsEffect {
    data class ShowError(val message: String) : AnalyticsEffect()
    data class NavigateToSessionDetail(val sessionId: String) : AnalyticsEffect()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSessionsUseCase: GetSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AnalyticsEffect>()
    val effect: SharedFlow<AnalyticsEffect> = _effect.asSharedFlow()

    fun processIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.OnLoadSessions -> loadSessions()
            is AnalyticsIntent.OnRefresh -> loadSessions()
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val sessions = getSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, sessions = sessions.sortedByDescending { it.startTime })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _effect.emit(AnalyticsEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }
}