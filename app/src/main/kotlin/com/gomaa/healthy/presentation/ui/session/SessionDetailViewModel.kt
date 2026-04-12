package com.gomaa.healthy.presentation.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.domain.usecase.GetSessionDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val isLoading: Boolean = false,
    val session: ExerciseSession? = null,
    val error: String? = null
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val getSessionDetailUseCase: GetSessionDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = SessionDetailUiState(isLoading = true)
            try {
                val session = getSessionDetailUseCase(sessionId)
                _uiState.value = SessionDetailUiState(
                    isLoading = false,
                    session = session
                )
            } catch (e: Exception) {
                _uiState.value = SessionDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load session"
                )
            }
        }
    }
}