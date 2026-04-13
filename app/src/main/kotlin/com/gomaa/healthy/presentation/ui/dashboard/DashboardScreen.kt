package com.gomaa.healthy.presentation.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.ConnectionState
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTheme
import com.gomaa.healthy.presentation.ui.theme.HeartRateZoneHigh
import com.gomaa.healthy.presentation.ui.theme.HeartRateZoneLow
import com.gomaa.healthy.presentation.ui.theme.HeartRateZoneMedium
import com.gomaa.healthy.presentation.ui.theme.HeartRateZoneVeryHigh
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(), onNavigateToAnalytics: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DashboardEffect.ShowSaveSuccess -> {
                    snackbarHostState.showSnackbar("Session saved successfully!")
                }

                is DashboardEffect.NavigateToAnalytics -> {
                    onNavigateToAnalytics()
                }

                is DashboardEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Active Pulse") }, actions = {
            uiState.deviceBrand?.let { brand ->
                ConnectionBadge(brand = brand)
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        DashboardContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onStartTracking = { viewModel.processIntent(DashboardIntent.OnStartTracking) },
            onStopTracking = { viewModel.processIntent(DashboardIntent.OnStopTracking) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    uiState: DashboardUiState,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    val zoneColor = getZoneColor(uiState.heartRateZone)
    val animatedColor by animateColorAsState(
        targetValue = zoneColor, animationSpec = tween(300), label = "zoneColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isTracking && uiState.heartRate > 0) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Dimensions.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        HeartRateDisplay(
            heartRate = uiState.heartRate,
            zone = uiState.heartRateZone,
            color = animatedColor,
            scale = pulseScale
        )

        if (uiState.isTracking) {
            TrackingStats(
                elapsedTime = uiState.elapsedTime,
                avgHeartRate = uiState.avgHeartRate,
                maxHeartRate = uiState.maxHeartRate,
                minHeartRate = uiState.minHeartRate
            )
        }

        ConnectionStatusRow(
            connectionState = uiState.connectionState, deviceBrand = uiState.deviceBrand
        )

        TrackingButton(
            isTracking = uiState.isTracking,
            isConnected = uiState.connectionState == ConnectionState.Connected,
            onStart = onStartTracking,
            onStop = onStopTracking
        )
    }
}

@Composable
private fun HeartRateDisplay(
    heartRate: Int, zone: HeartRateZone, color: Color, scale: Float
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (heartRate > 0) "$heartRate" else "--",
                    style = MaterialTheme.typography.displayLarge,
                    color = color
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = zone.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun TrackingStats(
    elapsedTime: Long, avgHeartRate: Int, maxHeartRate: Int, minHeartRate: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spacingLarge)
        ) {
            Text(
                text = "Session Stats", style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Duration", value = formatTime(elapsedTime))
                StatItem(label = "Avg", value = "$avgHeartRate BPM")
                StatItem(label = "Max", value = "$maxHeartRate BPM")
                StatItem(label = "Min", value = "$minHeartRate BPM")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value, style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionStatusRow(
    connectionState: ConnectionState, deviceBrand: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                Text(text = "●", color = MaterialTheme.colorScheme.primary)
                Text(text = "Connected", color = MaterialTheme.colorScheme.primary)
            }

            is ConnectionState.Connecting -> {
                Text(text = "○", color = MaterialTheme.colorScheme.tertiary)
                Text(text = "Connecting...", color = MaterialTheme.colorScheme.tertiary)
            }

            is ConnectionState.Disconnected -> {
                Text(text = "○", color = MaterialTheme.colorScheme.outline)
                Text(text = "Disconnected", color = MaterialTheme.colorScheme.outline)
            }

            is ConnectionState.Error -> {
                Text(text = "!", color = MaterialTheme.colorScheme.error)
                Text(text = "Error", color = MaterialTheme.colorScheme.error)
            }
        }
        deviceBrand?.let { brand ->
            Text(
                text = "($brand)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackingButton(
    isTracking: Boolean, isConnected: Boolean, onStart: () -> Unit, onStop: () -> Unit
) {
    Button(
        onClick = if (isTracking) onStop else onStart,
        enabled = isConnected || isTracking,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = if (isTracking) "Stop Session" else "Start Session",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ConnectionBadge(brand: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = brand,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Dimensions.spacing, vertical = 4.dp)
        )
    }
}

private fun getZoneColor(zone: HeartRateZone): Color {
    return when (zone) {
        HeartRateZone.REST -> HeartRateZoneLow
        HeartRateZone.LOW -> HeartRateZoneMedium
        HeartRateZone.MODERATE -> HeartRateZoneHigh
        HeartRateZone.HIGH -> HeartRateZoneVeryHigh
        HeartRateZone.VERY_HIGH -> Color(0xFFB71C1C)
    }
}

private fun formatTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

// ========== Compose Previews ==========

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dashboard - Idle", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun DashboardScreenIdlePreview() {
    HealthTheme {
        DashboardContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.dashboardIdleState,
            onStartTracking = {},
            onStopTracking = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dashboard - Tracking", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun DashboardScreenTrackingPreview() {
    HealthTheme {
        DashboardContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.dashboardTrackingState,
            onStartTracking = {},
            onStopTracking = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dashboard - Error", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun DashboardScreenErrorPreview() {
    HealthTheme {
        DashboardContent(
            paddingValues = androidx.compose.foundation.layout.PaddingValues(Dimensions.spacingLarge),
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.dashboardErrorState,
            onStartTracking = {},
            onStopTracking = {},
        )
    }
}