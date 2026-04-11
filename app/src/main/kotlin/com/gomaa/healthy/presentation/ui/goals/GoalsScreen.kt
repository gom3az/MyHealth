package com.gomaa.healthy.presentation.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.FitnessGoal
import com.gomaa.healthy.domain.model.GoalPeriod
import com.gomaa.healthy.domain.model.GoalType

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GoalsContent(
        goals = uiState.goals,
        goalProgress = uiState.goalProgress,
        isLoading = uiState.isLoading,
        showCreateDialog = uiState.showCreateDialog,
        onCreateGoal = { name, type, period ->
            viewModel.processIntent(GoalsIntent.CreateGoal(name, type, period))
        },
        onDeleteGoal = { id ->
            viewModel.processIntent(GoalsIntent.DeleteGoal(id))
        },
        onShowCreateDialog = {
            viewModel.processIntent(GoalsIntent.ShowCreateDialog)
        },
        onHideCreateDialog = {
            viewModel.processIntent(GoalsIntent.HideCreateDialog)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsContent(
    goals: List<FitnessGoal>,
    goalProgress: Map<String, Float>,
    isLoading: Boolean,
    showCreateDialog: Boolean,
    onCreateGoal: (String, GoalType, GoalPeriod) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onShowCreateDialog: () -> Unit,
    onHideCreateDialog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Goals") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShowCreateDialog) {
                Text("+")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Loading...")
            }
        } else if (goals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No goals yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to create your first goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        progress = goalProgress[goal.id] ?: 0f,
                        onDelete = { onDeleteGoal(goal.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = onHideCreateDialog,
            onCreate = onCreateGoal
        )
    }
}

@Composable
private fun GoalCard(
    goal: FitnessGoal,
    progress: Float,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val targetText = when (goal.type) {
                is GoalType.Steps -> "%,d steps".format(goal.type.target)
                is GoalType.ActivityMinutes -> "${goal.type.targetMinutes} min"
                is GoalType.Distance -> "%.1f km".format(goal.type.targetMeters / 1000)
                is GoalType.HeartRateZone -> "${goal.type.zone} zone"
            }

            Text(
                text = targetText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(progress * 100).toInt()}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (String, GoalType, GoalPeriod) -> Unit
) {
    var name by remember { mutableStateOf("") }
                var selectedType by remember { mutableStateOf<GoalType>(GoalType.Steps(10000)) }
                var selectedPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Goal Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedType = GoalType.Steps(10000) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Steps")
                    }
                    Button(
                        onClick = { selectedType = GoalType.ActivityMinutes(30) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mins")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedPeriod = GoalPeriod.DAILY },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Daily")
                    }
                    Button(
                        onClick = { selectedPeriod = GoalPeriod.WEEKLY },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Weekly")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedType, selectedPeriod) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}