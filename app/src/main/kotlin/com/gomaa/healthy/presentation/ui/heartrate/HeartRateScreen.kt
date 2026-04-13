package com.gomaa.healthy.presentation.ui.heartrate

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    viewModel: HeartRateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Heart Rate") },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                androidx.compose.material3.IconButton(
                    onClick = { viewModel.processIntent(HeartRateIntent.OnSync) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync"
                    )
                }
            }
        )
    }) { paddingValues ->
        HeartRateContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onIntent = { intent -> viewModel.processIntent(intent) }
        )
    }
}

@Composable
private fun HeartRateContent(
    paddingValues: PaddingValues,
    uiState: HeartRateUiState,
    onIntent: (HeartRateIntent) -> Unit
) {
    when (uiState) {
        is HeartRateUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is HeartRateUiState.Empty -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No heart rate data yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sync from Health Connect to see your heart rate readings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onIntent(HeartRateIntent.OnSync) }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Sync from Health Connect")
                }
            }
        }

        is HeartRateUiState.Loaded -> {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimensions.contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Source Filter
                item {
                    SourceFilterChips(
                        selectedFilter = uiState.sourceFilter,
                        onFilterChanged = { onIntent(HeartRateIntent.OnSourceFilterChanged(it)) }
                    )
                }

                // Summary Cards
                uiState.todaySummary?.let { summary ->
                    item {
                        HeartRateSummaryCards(summary = summary)
                    }
                }

                // Recent Readings - HC-064: Show all readings by source, not just latest
                if (uiState.recentReadings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Readings",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(uiState.recentReadings) { reading ->
                        HeartRateReadingItem(reading = reading)
                    }
                }

                // Sync Button
                item {
                    Button(
                        onClick = { onIntent(HeartRateIntent.OnSync) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Sync from Health Connect")
                    }
                }
            }
        }

        is HeartRateUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error: ${uiState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onIntent(HeartRateIntent.OnRefresh) }) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SourceFilterChips(
    selectedFilter: HeartRateSourceFilter,
    onFilterChanged: (HeartRateSourceFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == HeartRateSourceFilter.ALL,
            onClick = { onFilterChanged(HeartRateSourceFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedFilter == HeartRateSourceFilter.MY_HEALTH,
            onClick = { onFilterChanged(HeartRateSourceFilter.MY_HEALTH) },
            label = { Text("MyHealth") }
        )
        FilterChip(
            selected = selectedFilter == HeartRateSourceFilter.HEALTH_CONNECT,
            onClick = { onFilterChanged(HeartRateSourceFilter.HEALTH_CONNECT) },
            label = { Text("Health Connect") }
        )
    }
}

@Composable
private fun HeartRateSummaryCards(summary: HeartRateSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "Average",
            value = "${summary.averageBpm}",
            unit = "BPM",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Max",
            value = "${summary.maxBpm}",
            unit = "BPM",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Min",
            value = "${summary.minBpm}",
            unit = "BPM",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// HC-067: Use remember for SimpleDateFormat to avoid recreation on every composition
@Composable
private fun HeartRateReadingItem(reading: HeartRateReading) {
    // HC-067: Use remember to cache SimpleDateFormat instance
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = timeFormat.format(Date(reading.timestamp)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when (reading.source) {
                        HeartRateSource.MY_HEALTH -> "MyHealth"
                        HeartRateSource.HEALTH_CONNECT -> "Health Connect"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "${reading.bpm} BPM",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
