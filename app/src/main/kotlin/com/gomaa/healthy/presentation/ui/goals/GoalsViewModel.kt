package com.gomaa.healthy.presentation.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalPeriod
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.usecase.CalculateGoalProgressUseCase
import com.gomaa.healthy.domain.usecase.CreateGoalUseCase
import com.gomaa.healthy.domain.usecase.DeleteGoalUseCase
import com.gomaa.healthy.domain.usecase.GetAllGoalsUseCase
import com.gomaa.healthy.domain.usecase.GetDailyStepsUseCase
import com.gomaa.healthy.domain.usecase.UpdateGoalStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalsUiState(
    val isLoading: Boolean = false,
    val goals: List<FitnessGoal> = emptyList(),
    val goalProgress: Map<String, Float> = emptyMap(),
    val showCreateDialog: Boolean = false
)

sealed class GoalsIntent {
    data object LoadGoals : GoalsIntent()
    data class CreateGoal(val name: String, val type: GoalType, val period: GoalPeriod) :
        GoalsIntent()

    data class DeleteGoal(val id: String) : GoalsIntent()
    data class ToggleGoal(val id: String, val isActive: Boolean) : GoalsIntent()
    data object ShowCreateDialog : GoalsIntent()
    data object HideCreateDialog : GoalsIntent()
}

sealed class GoalsEffect {
    data object ShowCreateSuccess : GoalsEffect()
    data object ShowDeleteConfirmation : GoalsEffect()
    data class ShowError(val message: String) : GoalsEffect()
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getAllGoalsUseCase: GetAllGoalsUseCase,
    private val createGoalUseCase: CreateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val updateGoalStatusUseCase: UpdateGoalStatusUseCase,
    private val getDailyStepsUseCase: GetDailyStepsUseCase,
    private val calculateGoalProgressUseCase: CalculateGoalProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<GoalsEffect>()
    val effect: SharedFlow<GoalsEffect> = _effect.asSharedFlow()

    fun processIntent(intent: GoalsIntent) {
        when (intent) {
            is GoalsIntent.LoadGoals -> loadGoals()
            is GoalsIntent.CreateGoal -> createGoal(intent.name, intent.type, intent.period)
            is GoalsIntent.DeleteGoal -> deleteGoal(intent.id)
            is GoalsIntent.ToggleGoal -> toggleGoal(intent.id, intent.isActive)
            is GoalsIntent.ShowCreateDialog -> _uiState.value =
                _uiState.value.copy(showCreateDialog = true)

            is GoalsIntent.HideCreateDialog -> _uiState.value =
                _uiState.value.copy(showCreateDialog = false)
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val goals = getAllGoalsUseCase()
                val todaySteps = getDailyStepsUseCase(LocalDate.now())?.totalSteps ?: 0

                val progressMap = mutableMapOf<String, Float>()
                goals.forEach { goal ->
                    val progress = when (val type = goal.type) {
                        is GoalType.Steps -> calculateGoalProgressUseCase(todaySteps, type.target)
                        is GoalType.ActivityMinutes -> {
                            val minutes = getDailyStepsUseCase(LocalDate.now())?.activeMinutes ?: 0
                            calculateGoalProgressUseCase(minutes, type.targetMinutes)
                        }

                        else -> 0f
                    }
                    progressMap[goal.id] = progress
                }

                _uiState.value = _uiState.value.copy(
                    goals = goals, goalProgress = progressMap, isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _effect.emit(GoalsEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun createGoal(name: String, type: GoalType, period: GoalPeriod) {
        viewModelScope.launch {
            try {
                createGoalUseCase(name, type, period)
                _uiState.value = _uiState.value.copy(showCreateDialog = false)
                _effect.emit(GoalsEffect.ShowCreateSuccess)
                loadGoals()
            } catch (e: Exception) {
                _effect.emit(GoalsEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun deleteGoal(id: String) {
        viewModelScope.launch {
            try {
                deleteGoalUseCase(id)
                _effect.emit(GoalsEffect.ShowDeleteConfirmation)
                loadGoals()
            } catch (e: Exception) {
                _effect.emit(GoalsEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun toggleGoal(id: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                updateGoalStatusUseCase(id, isActive)
                loadGoals()
            } catch (e: Exception) {
                _effect.emit(GoalsEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }
}