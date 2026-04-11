package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalType
import com.gomaa.healthy.domain.repository.GoalRepository
import java.util.UUID
import javax.inject.Inject

class GetActiveGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(): List<FitnessGoal> {
        return goalRepository.getActiveGoals()
    }
}

class GetAllGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(): List<FitnessGoal> {
        return goalRepository.getAllGoals()
    }
}

class CreateGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(
        name: String,
        type: GoalType,
        period: com.gomaa.healthy.domain.model.GoalPeriod
    ): FitnessGoal {
        val goal = FitnessGoal(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            period = period,
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        goalRepository.saveGoal(goal)
        return goal
    }
}

class UpdateGoalStatusUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(id: String, isActive: Boolean) {
        goalRepository.updateGoalStatus(id, isActive)
    }
}

class DeleteGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(id: String) {
        goalRepository.deleteGoal(id)
    }
}

class CalculateGoalProgressUseCase @Inject constructor() {
    operator fun invoke(current: Int, target: Int): Float {
        return if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    }
}