package com.gomaa.healthy.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.ConnectionState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(), 
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is HomeEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is HomeEffect.NavigateToSettings -> {
                    // Handle navigation
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyHealth") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HomeContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onProviderSelected = { viewModel.processIntent(HomeIntent.OnSelectProvider(it)) },
            onSwitchProvider = { viewModel.processIntent(HomeIntent.OnSwitchProvider(it)) },
            onConnect = { viewModel.processIntent(HomeIntent.OnConnect) },
            onDisconnect = { viewModel.processIntent(HomeIntent.OnDisconnect) },
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateToGoals = onNavigateToGoals,
            onRefresh = { viewModel.processIntent(HomeIntent.OnRefresh) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    uiState: HomeUiState,
    onProviderSelected: (String) -> Unit,
    onSwitchProvider: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onRefresh: () -> Unit
) {
    var showProviderSwitchDialog by remember { mutableStateOf(false) }
    var selectedNewProvider by remember { mutableStateOf<String?>(null) }
    var showProviderSelectionDialog by remember { mutableStateOf(false) }

    if (showProviderSelectionDialog) {
        ProviderSelectionDialog(
            currentProvider = uiState.connectedDeviceBrand,
            availableProviders = uiState.availableProviders,
            onProviderSelected = { newProvider ->
                showProviderSelectionDialog = false
                showProviderSwitchDialog = true
                selectedNewProvider = newProvider
            },
            onDismiss = { showProviderSelectionDialog = false }
        )
    }

    if (showProviderSwitchDialog && selectedNewProvider != null) {
        SwitchProviderConfirmationDialog(
            currentProvider = uiState.connectedDeviceBrand ?: "",
            newProvider = selectedNewProvider ?: "",
            onConfirm = {
                onSwitchProvider(selectedNewProvider!!)
                showProviderSwitchDialog = false
                selectedNewProvider = null
            },
            onDismiss = {
                showProviderSwitchDialog = false
                selectedNewProvider = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyHealth") }
            )
        }
    ) { parentPaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepsProgressCard(
                steps = uiState.todaySteps?.totalSteps ?: 0,
                goalProgress = uiState.stepGoalProgress,
                onClick = onNavigateToGoals
            )

            ConnectionStatusCard(uiState = uiState)

            if (uiState.connectedDeviceBrand == null) {
                ProviderSelectionCard(
                    availableProviders = uiState.availableProviders,
                    onProviderSelected = onProviderSelected
                )
            } else {
                ActionButtonRow(
                    isConnected = uiState.connectionState == ConnectionState.Connected,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onNavigateToDashboard = onNavigateToDashboard,
                    onChangeProvider = { showProviderSelectionDialog = true }
                )
            }

            RecentSessionsCard(sessions = uiState.recentSessions)
        }
    }
}

@Composable
private fun StepsProgressCard(
    steps: Int,
    goalProgress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Today's Steps",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%,d".format(steps),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(goalProgress * 100).toInt()}% of daily goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Heart Rate", style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (uiState.heartRate > 0) "${uiState.heartRate} BPM" else "-- BPM",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val (icon, text, color) = when (uiState.connectionState) {
                    ConnectionState.Connected -> Triple(
                        "●", "Connected", MaterialTheme.colorScheme.primary
                    )

                    ConnectionState.Connecting -> Triple(
                        "○", "Connecting...", MaterialTheme.colorScheme.tertiary
                    )

                    ConnectionState.Disconnected -> Triple(
                        "○", "Disconnected", MaterialTheme.colorScheme.outline
                    )

                    is ConnectionState.Error -> Triple(
                        "!",
                        "Error: ${uiState.connectionState.message}",
                        MaterialTheme.colorScheme.error
                    )
                }
                Text(text = icon, color = color)
                Text(text = text, color = color)
            }
            uiState.connectedDeviceBrand?.let { brand ->
                Text(
                    text = "Device: $brand",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProviderSelectionCard(
    availableProviders: List<String>, onProviderSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Wearable Provider", style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableProviders.forEach { provider ->
                    FilterChip(
                        selected = false,
                        onClick = { onProviderSelected(provider) },
                        label = { Text(provider) })
                }
            }
        }
    }
}

@Composable
private fun ActionButtonRow(
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onChangeProvider: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = if (isConnected) onDisconnect else onConnect,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
            Button(
                onClick = onNavigateToDashboard, modifier = Modifier.weight(1f)
            ) {
                Text("Start Run")
            }
        }
        Button(
            onClick = onChangeProvider,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Provider")
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<com.gomaa.healthy.domain.model.ExerciseSession>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Sessions", style = MaterialTheme.typography.titleMedium
            )
            if (sessions.isEmpty()) {
                Text(
                    text = "No sessions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = java.text.SimpleDateFormat(
                                "MMM dd, HH:mm", java.util.Locale.getDefault()
                            ).format(java.util.Date(session.startTime))
                        )
                        Text(text = "${session.avgHeartRate} BPM avg")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSelectionDialog(
    currentProvider: String?,
    availableProviders: List<String>,
    onProviderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val otherProviders = availableProviders.filter { it != currentProvider }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Healthcare Provider") },
        text = {
            if (otherProviders.isEmpty()) {
                Text("No other providers available for switching")
            } else {
                Column {
                    Text(
                        text = "Choose a new provider",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    otherProviders.forEach { provider ->
                        androidx.compose.material3.TextButton(
                            onClick = { onProviderSelected(provider) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(provider)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SwitchProviderConfirmationDialog(
    currentProvider: String,
    newProvider: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Provider?") },
        text = {
            Text("You are switching from $currentProvider to $newProvider")
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}