package com.gomaa.healthy.presentation.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToHeartRate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showProviderSwitchDialog by remember { mutableStateOf(false) }
    var selectedNewProvider by remember { mutableStateOf<String?>(null) }
    var showProviderSelectionDialog by remember { mutableStateOf(false) }

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

    if (showProviderSelectionDialog) {
        ProviderSelectionDialog(
            currentProvider = uiState.connectedDeviceBrand,
            availableProviders = uiState.availableProviders,
            onProviderSelected = { newProvider ->
                showProviderSelectionDialog = false
                showProviderSwitchDialog = true
                selectedNewProvider = newProvider
            },
            onDismiss = { showProviderSelectionDialog = false })
    }

    if (showProviderSwitchDialog && selectedNewProvider != null) {
        SwitchProviderConfirmationDialog(
            currentProvider = uiState.connectedDeviceBrand ?: "",
            newProvider = selectedNewProvider ?: "",
            onConfirm = {
                viewModel.processIntent(HomeIntent.OnSwitchProvider(selectedNewProvider!!))
                showProviderSwitchDialog = false
                selectedNewProvider = null
            },
            onDismiss = {
                showProviderSwitchDialog = false
                selectedNewProvider = null
            })
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("MyHealth") })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        HomeContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onProviderSelected = { viewModel.processIntent(HomeIntent.OnSelectProvider(it)) },
            onConnect = { viewModel.processIntent(HomeIntent.OnConnect) },
            onDisconnect = { viewModel.processIntent(HomeIntent.OnDisconnect) },
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateToGoals = onNavigateToGoals,
            onNavigateToHeartRate = onNavigateToHeartRate,
            onFilterChanged = { viewModel.processIntent(HomeIntent.OnFilterChanged(it)) },
            onChangeProvider = { showProviderSelectionDialog = true })
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    uiState: HomeUiState,
    onProviderSelected: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToHeartRate: () -> Unit,
    onFilterChanged: (StepSourceFilter) -> Unit,
    onChangeProvider: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.contentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
    ) {
        item {
            StepsFilterChips(
                selectedFilter = uiState.stepSourceFilter,
                onFilterChanged = onFilterChanged,
                healthConnectAvailable = uiState.healthConnectAvailable
            )
        }

        item {
            StepsProgressCard(
                steps = getFilteredSteps(uiState),
                goalProgress = uiState.stepGoalProgress,
                onClick = onNavigateToGoals,
                combinedSteps = uiState.combinedSteps,
                selectedFilter = uiState.stepSourceFilter
            )
        }

        if (uiState.healthConnectAvailable && uiState.stepSourceFilter != StepSourceFilter.MY_HEALTH) {
            item {
                ExpandableStepsSection(
                    combinedSteps = uiState.combinedSteps, selectedFilter = uiState.stepSourceFilter
                )
            }
        }

        item {
            HeartRateCard(
                latestHeartRate = uiState.latestHeartRate,
                lastUpdate = uiState.lastHeartRateUpdate,
                isLoading = uiState.isLoadingHeartRate,
                onClick = onNavigateToHeartRate
            )
        }

        item {
            ConnectionStatusCard(uiState = uiState)
        }

        if (uiState.connectedDeviceBrand == null) {
            item {
                ProviderSelectionCard(
                    availableProviders = uiState.availableProviders,
                    onProviderSelected = onProviderSelected
                )
            }
        } else {
            item {
                ActionButtonRow(
                    isConnected = uiState.connectionState == ConnectionState.Connected,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onNavigateToDashboard = onNavigateToDashboard,
                    onChangeProvider = onChangeProvider
                )
            }
        }

        item {
            RecentSessionsCard(sessions = uiState.recentSessions)
        }
    }
}

@Composable
private fun StepsFilterChips(
    selectedFilter: StepSourceFilter,
    onFilterChanged: (StepSourceFilter) -> Unit,
    healthConnectAvailable: Boolean
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
        FilterChip(
            selected = selectedFilter == StepSourceFilter.ALL,
            onClick = { onFilterChanged(StepSourceFilter.ALL) },
            label = { Text("All") })
        FilterChip(
            selected = selectedFilter == StepSourceFilter.MY_HEALTH,
            onClick = { onFilterChanged(StepSourceFilter.MY_HEALTH) },
            label = { Text("MyHealth") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
            })
        if (healthConnectAvailable) {
            FilterChip(
                selected = selectedFilter == StepSourceFilter.HEALTH_CONNECT,
                onClick = { onFilterChanged(StepSourceFilter.HEALTH_CONNECT) },
                label = { Text("Health Connect") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                })
        }
    }
}

@Composable
private fun StepsProgressCard(
    steps: Int,
    goalProgress: Float,
    onClick: () -> Unit,
    combinedSteps: com.gomaa.healthy.domain.model.CombinedSteps,
    selectedFilter: StepSourceFilter
) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Steps", style = MaterialTheme.typography.titleMedium
                )
                if (combinedSteps.healthConnectSteps > 0 && selectedFilter == StepSourceFilter.ALL) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Combined",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = Dimensions.spacing,
                                vertical = 4.dp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Text(
                text = "%,d".format(steps),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(goalProgress * 100).toInt()}% of daily goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedFilter == StepSourceFilter.ALL && combinedSteps.healthConnectSteps > 0) {
                Spacer(modifier = Modifier.height(Dimensions.spacing))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MyHealth: %,d".format(combinedSteps.myHealthSteps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Health Connect: %,d".format(combinedSteps.healthConnectSteps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableStepsSection(
    combinedSteps: com.gomaa.healthy.domain.model.CombinedSteps, selectedFilter: StepSourceFilter
) {
    var myHealthExpanded by remember { mutableStateOf(false) }
    var healthConnectExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
        // MyHealth Section
        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(Dimensions.spacingLarge)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "MyHealth", style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "%,d steps".format(combinedSteps.myHealthSteps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                AnimatedVisibility(
                    visible = myHealthExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = Dimensions.spacing)) {
                        Text(
                            text = "Steps tracked manually in MyHealth app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { myHealthExpanded = !myHealthExpanded },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (myHealthExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (myHealthExpanded) "Collapse" else "Expand"
                    )
                }
            }
        }

        // Health Connect Section
        if (combinedSteps.healthConnectSteps > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(Dimensions.spacingLarge)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Health Connect", style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "%,d steps".format(combinedSteps.healthConnectSteps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    AnimatedVisibility(
                        visible = healthConnectExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = Dimensions.spacing)) {
                            Text(
                                text = "Steps imported from Health Connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { healthConnectExpanded = !healthConnectExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = if (healthConnectExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (healthConnectExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
        }
    }
}

private fun getFilteredSteps(uiState: HomeUiState): Int {
    return when (uiState.stepSourceFilter) {
        StepSourceFilter.ALL -> uiState.combinedSteps.totalSteps
        StepSourceFilter.MY_HEALTH -> uiState.combinedSteps.myHealthSteps
        StepSourceFilter.HEALTH_CONNECT -> uiState.combinedSteps.healthConnectSteps
    }
}

@Composable
private fun HeartRateCard(
    latestHeartRate: Int?, lastUpdate: Long?, isLoading: Boolean, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heart Rate", style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spacing))

            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (latestHeartRate != null) {
                Text(
                    text = "$latestHeartRate BPM",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                lastUpdate?.let { timestamp ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val timeFormat =
                        java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    Text(
                        text = "Last updated: ${timeFormat.format(java.util.Date(timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No heart rate data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sync from Health Connect to see readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                .padding(Dimensions.spacingLarge),
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
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
            ) {
                val (icon, text, color) = when (uiState.connectionState) {
                    ConnectionState.Connected -> Triple(
                        Icons.Default.CheckCircle, "Connected", MaterialTheme.colorScheme.primary
                    )

                    ConnectionState.Connecting -> Triple(
                        Icons.Default.Refresh, "Connecting...", MaterialTheme.colorScheme.tertiary
                    )

                    ConnectionState.Disconnected -> Triple(
                        Icons.Default.Refresh, "Disconnected", MaterialTheme.colorScheme.outline
                    )

                    is ConnectionState.Error -> Triple(
                        Icons.Default.Error,
                        "Error: ${uiState.connectionState.message}",
                        MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = icon, contentDescription = null, tint = color
                )
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
            modifier = Modifier.padding(Dimensions.spacingLarge)
        ) {
            Text(
                text = "Select Wearable Provider", style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
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
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
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
            onClick = onChangeProvider, modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.padding(Dimensions.spacingLarge)
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
                            .padding(vertical = Dimensions.spacing),
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
                    Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
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
        })
}

@Composable
private fun SwitchProviderConfirmationDialog(
    currentProvider: String, newProvider: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Provider?") },
        text = {
            Text("You are switching from $currentProvider to $newProvider")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        })
}

// ========== Compose Previews ==========

@androidx.compose.ui.tooling.preview.Preview(
    name = "Home - Loaded", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun HomeScreenLoadedPreview() {
    HealthTheme {
        HomeContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.homeLoadedState,
            onProviderSelected = {},
            onConnect = {},
            onDisconnect = {},
            onNavigateToDashboard = {},
            onNavigateToGoals = {},
            onNavigateToHeartRate = {},
            onFilterChanged = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Home - Disconnected", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun HomeScreenDisconnectedPreview() {
    HealthTheme {
        HomeContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.homeDisconnectedState,
            onProviderSelected = {},
            onConnect = {},
            onDisconnect = {},
            onNavigateToDashboard = {},
            onNavigateToGoals = {},
            onNavigateToHeartRate = {},
            onFilterChanged = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Home - Empty", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun HomeScreenEmptyPreview() {
    HealthTheme {
        HomeContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.homeEmptyState,
            onProviderSelected = {},
            onConnect = {},
            onDisconnect = {},
            onNavigateToDashboard = {},
            onNavigateToGoals = {},
            onNavigateToHeartRate = {},
            onFilterChanged = {})
    }
}