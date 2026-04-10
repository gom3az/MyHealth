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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.presentation.viewmodel.HomeUiState
import com.gomaa.healthy.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(), onNavigateToDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onProviderSelected = viewModel::selectProvider,
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onNavigateToDashboard = onNavigateToDashboard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onProviderSelected: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vanguard Dynamics") })
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    onNavigateToDashboard = onNavigateToDashboard
                )
            }

            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            RecentSessionsCard(sessions = uiState.recentSessions)
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
    onNavigateToDashboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = if (isConnected) onDisconnect else onConnect, modifier = Modifier.weight(1f)
        ) {
            Text(if (isConnected) "Disconnect" else "Connect")
        }
        Button(
            onClick = onNavigateToDashboard, modifier = Modifier.weight(1f)
        ) {
            Text("Start Run")
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