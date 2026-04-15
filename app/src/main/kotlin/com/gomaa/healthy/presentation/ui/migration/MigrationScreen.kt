@file:OptIn(ExperimentalMaterial3Api::class)

package com.gomaa.healthy.presentation.ui.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTopAppBar
import kotlinx.coroutines.flow.first

@Composable
fun MigrationScreen(
    onComplete: () -> Unit, viewModel: MigrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for Health Kit connection dialog (Phase 6)
    var showHealthKitDialog by remember { mutableStateOf(false) }
    var connectHealthKit by remember { mutableStateOf(false) }
    var connectHealthConnect by remember { mutableStateOf(true) }

    // Health Kit connection dialog
    if (showHealthKitDialog) {
        HealthKitConnectionDialog(
            connectHealthKit = connectHealthKit,
            connectHealthConnect = connectHealthConnect,
            onHealthKitChanged = { connectHealthKit = it },
            onHealthConnectChanged = { connectHealthConnect = it },
            onConfirm = {
                showHealthKitDialog = false
                // If user wants to connect to Health Kit, trigger it
                if (connectHealthKit) {
                    // This would be handled by the view model
                    // For now, just proceed
                }
                // Proceed with Health Connect migration
                viewModel.processIntent(MigrationIntent.StartMigration)
            },
            onDismiss = {
                showHealthKitDialog = false
                viewModel.processIntent(MigrationIntent.SkipMigration)
            })
    }

    LaunchedEffect(Unit) {
        val effect = viewModel.sideEffect.first()
        when (effect) {
            is MigrationSideEffect.NavigateToHome -> onComplete()
            is MigrationSideEffect.MigrationComplete -> onComplete()
            is MigrationSideEffect.ShowError -> {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(topBar = {
        HealthTopAppBar(
            title = "Welcome to MyHealth",
            titleStyle = MaterialTheme.typography.displaySmall
        )
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimensions.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)
        ) {
            item {
                when (val currentState = state) {
                    is MigrationUiState.Idle -> {
                        IdleContent(
                            onStartMigration = { showHealthKitDialog = true },
                            onSkip = { viewModel.processIntent(MigrationIntent.SkipMigration) })
                    }

                    is MigrationUiState.InProgress -> {
                        InProgressContent(
                            stepsImported = currentState.stepsImported,
                            exerciseImported = currentState.exerciseImported,
                            heartRateImported = currentState.heartRateImported,
                            onCancel = { viewModel.processIntent(MigrationIntent.CancelMigration) })
                    }

                    is MigrationUiState.Success -> {
                        SuccessContent(
                            stepsImported = currentState.stepsImported,
                            exerciseImported = currentState.exerciseImported,
                            heartRateImported = currentState.heartRateImported,
                            onDone = onComplete
                        )
                    }

                    is MigrationUiState.Error -> {
                        ErrorContent(
                            errorMessage = currentState.message,
                            onRetry = { viewModel.processIntent(MigrationIntent.StartMigration) },
                            onSkip = { viewModel.processIntent(MigrationIntent.SkipMigration) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onStartMigration: () -> Unit, onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📥",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { })

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Import Health Data",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "We've detected Health Connect data that can be imported into MyHealth. This includes your historical steps, exercise sessions, and heart rate readings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            Button(
                onClick = onStartMigration,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Import Now")
            }

            Spacer(modifier = Modifier.height(Dimensions.spacing))

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius)
            ) {
                Text("Skip for Now")
            }
        }
    }
}

@Composable
private fun InProgressContent(
    stepsImported: Int, exerciseImported: Int, heartRateImported: Int, onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            Text(
                text = "Importing Data...", style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImportStat(label = "Steps", count = stepsImported)
                ImportStat(label = "Exercise", count = exerciseImported)
                ImportStat(label = "Heart Rate", count = heartRateImported)
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ImportStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(), style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessContent(
    stepsImported: Int, exerciseImported: Int, heartRateImported: Int, onDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✅",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { })

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Import Complete", style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Successfully imported data from Health Connect:",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImportStat(label = "Steps", count = stepsImported)
                ImportStat(label = "Exercise", count = exerciseImported)
                ImportStat(label = "Heart Rate", count = heartRateImported)
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String, onRetry: () -> Unit, onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { })

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Import Failed",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Retry")
            }

            Spacer(modifier = Modifier.height(Dimensions.spacing))

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius)
            ) {
                Text("Skip for Now")
            }
        }
    }
}

/**
 * Health Kit connection dialog for first-run migration (Phase 6).
 * Allows users to connect to Health Kit, Health Connect, both, or skip.
 */
@Composable
private fun HealthKitConnectionDialog(
    connectHealthKit: Boolean,
    connectHealthConnect: Boolean,
    onHealthKitChanged: (Boolean) -> Unit,
    onHealthConnectChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { })

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Connect Your Health Data",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            Text(
                text = "Choose which services to sync:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            // Health Connect option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.spacing)
            ) {
                Checkbox(
                    checked = connectHealthConnect, onCheckedChange = onHealthConnectChanged
                )
                Spacer(modifier = Modifier.width(Dimensions.spacing))
                Column {
                    Text(
                        text = "Health Connect", style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Sync from Google Health Connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Health Kit option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.spacing)
            ) {
                Checkbox(
                    checked = connectHealthKit, onCheckedChange = onHealthKitChanged
                )
                Spacer(modifier = Modifier.width(Dimensions.spacing))
                Column {
                    Text(
                        text = "Huawei Health Kit", style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Sync from Huawei devices and Health app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            // Confirm button
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = connectHealthKit || connectHealthConnect
            ) {
                Text("Continue")
            }

            Spacer(modifier = Modifier.height(Dimensions.spacing))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.buttonRadius)
            ) {
                Text("Skip All")
            }
        }
    }
}