package com.gomaa.healthy.presentation.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.usecase.GetSessionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val sessions: List<ExerciseSession> = emptyList(),
    val error: String? = null
)

class AnalyticsViewModel(
    private val getSessionsUseCase: GetSessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val sessions = getSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sessions = sessions.sortedByDescending { it.startTime }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() {
        loadSessions()
    }
}