package com.gomaa.healthy.presentation.ui.analytics

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.ExerciseSession
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToSessionDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.processIntent(AnalyticsIntent.OnLoadSessions)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AnalyticsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is AnalyticsEffect.NavigateToSessionDetail -> {
                    onNavigateToSessionDetail(effect.sessionId)
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "Analytics", style = MaterialTheme.typography.displaySmall
                )
            }, colors = TopAppBarDefaults.topAppBarColors(
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        AnalyticsContent(
            uiState = uiState,
            onSessionClick = viewModel::processIntent,
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onSessionClick: (AnalyticsIntent) -> Unit,
    paddingValues: PaddingValues = PaddingValues(Dimensions.spacingLarge),

    ) {
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.contentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
    ) {
        if (uiState.sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimensions.cardRadius)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(Dimensions.cardRadius)
                ) {
                    Text(
                        text = "No sessions recorded yet",
                        modifier = Modifier.padding(Dimensions.cardPadding),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            uiState.sessions.forEach { session ->
                item {
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(AnalyticsIntent.OnLoadSessions) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ExerciseSession, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.cardPadding)
        ) {
            Text(
                text = formatDate(session.startTime),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    label = "Duration", value = formatDuration(session.endTime - session.startTime)
                )
                StatColumn(label = "Avg HR", value = "${session.avgHeartRate} BPM")
                StatColumn(label = "Max HR", value = "${session.maxHeartRate} BPM")
                StatColumn(label = "Device", value = session.deviceBrand)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}m ${secs}s"
}

// ========== Compose Previews ==========

@androidx.compose.ui.tooling.preview.Preview(
    name = "Analytics - Loaded", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun AnalyticsScreenLoadedPreview() {
    HealthTheme {
        AnalyticsContent(
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.analyticsLoadedState,
            onSessionClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Analytics - Empty", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun AnalyticsScreenEmptyPreview() {
    HealthTheme {
        AnalyticsContent(
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.analyticsEmptyState,
            onSessionClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Analytics - Loading", showBackground = true, widthDp = 360, heightDp = 640
)
@Composable
private fun AnalyticsScreenLoadingPreview() {
    HealthTheme {
        AnalyticsContent(
            uiState = com.gomaa.healthy.presentation.ui.PreviewData.analyticsLoadingState,
            onSessionClick = {},
        )
    }
}
