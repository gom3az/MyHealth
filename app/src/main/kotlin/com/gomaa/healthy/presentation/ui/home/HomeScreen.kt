package com.gomaa.healthy.presentation.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.presentation.ui.theme.BorderLight
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.EnergyOrange
import com.gomaa.healthy.presentation.ui.theme.HealthGreen
import com.gomaa.healthy.presentation.ui.theme.HealthTopAppBar
import com.gomaa.healthy.presentation.ui.theme.Primary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToSteps: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.processIntent(HomeIntent.OnLoadData)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is HomeEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is HomeEffect.NavigateToGoals -> onNavigateToGoals()
                is HomeEffect.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    Scaffold(topBar = {
        HealthTopAppBar(
            title = "Today's Brief", titleStyle = MaterialTheme.typography.displaySmall
        )
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        HomeContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onNavigateToSteps = onNavigateToSteps,
            onNavigateToHeartRate = onNavigateToHeartRate,
            onNavigateToGoals = onNavigateToGoals
        )
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    onNavigateToSteps: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.contentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
    ) {
        // Today's Steps Card
        item {
            TodayStepsCard(
                steps = uiState.todaySteps,
                goal = uiState.stepGoal,
                progress = uiState.stepGoalProgress,
                onClick = onNavigateToSteps
            )
        }

        // Heart Rate Summary Card
        item {
            HeartRateSummaryCard(
                averageBpm = uiState.averageBpm,
                maxBpm = uiState.maxBpm,
                minBpm = uiState.minBpm,
                readingCount = uiState.readingCount,
                onClick = onNavigateToHeartRate
            )
        }

        // Activity Metrics Row
        item {
            ActivityMetricsRow(
                activeMinutes = uiState.activeMinutes, calories = uiState.caloriesBurned
            )
        }

        // Goals Progress Card
        item {
            if (uiState.activeGoalsCount > 0 && uiState.activeGoal != null) {
                GoalsProgressCard(
                    goal = uiState.activeGoal, goalsCount = uiState.activeGoalsCount,
                    progress = uiState.stepGoalProgress,
                    onClick = onNavigateToGoals
                )
            }
        }

        // Recent Sessions Card
        item {
            if (uiState.recentSessions.isNotEmpty()) {
                RecentSessionsCard(
                    sessions = uiState.recentSessions
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
        }
    }
}

@Composable
private fun TodayStepsCard(
    steps: Int, goal: Int, progress: Float, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),  // NO SHADOW
        border = BorderStroke(1.dp, BorderLight)  // Design.md compliant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = HealthGreen
                )
            }
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Text(
                text = steps.toString().reversed().chunked(3).joinToString(",").reversed(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = HealthGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}% of ${
                    goal.toString().reversed().chunked(3).joinToString(",").reversed()
                }",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeartRateSummaryCard(
    readingCount: Int?,
    minBpm: Int?,
    maxBpm: Int?,
    averageBpm: Int?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heart Rate",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spacing))

            if (averageBpm != null && minBpm != null && maxBpm != null && readingCount != null) {
                Text(
                    text = "$averageBpm BPM",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Min: $minBpm | Max: $maxBpm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (readingCount > 0) {
                    Text(
                        text = "$readingCount readings today",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No data today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sync from Health Connect to see readings",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityMetricsRow(
    activeMinutes: Int, calories: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
        // Active Minutes Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Dimensions.cardRadius)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(Dimensions.cardRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, BorderLight)
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.cardPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = EnergyOrange,
                        modifier = Modifier.height(18.dp)
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$activeMinutes",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calories Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Dimensions.cardRadius)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(Dimensions.cardRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, BorderLight)
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.cardPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = EnergyOrange,
                        modifier = Modifier.height(18.dp)
                    )
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$calories",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "kcal", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalsProgressCard(
    goal: FitnessGoal, goalsCount: Int, progress: Float, onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.cardPadding)
        ) {
            Text(
                text = "Goals Progress",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

            if (goalsCount > 1) {
                Spacer(modifier = Modifier.height(Dimensions.spacing))
                Text(
                    text = "+ ${goalsCount - 1} more goals",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(
    sessions: List<com.gomaa.healthy.domain.model.ExerciseSession>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.cardPadding)
        ) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))

            sessions.forEach { session ->
                val duration = (session.endTime - session.startTime) / 60000  // minutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.spacing),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    @Suppress("NonObservableLocale")
                    Text(
                        text = java.text.SimpleDateFormat(
                            "MMM dd, HH:mm", java.util.Locale.getDefault()
                        ).format(java.util.Date(session.startTime)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${duration}min • ${session.avgHeartRate}bpm",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}