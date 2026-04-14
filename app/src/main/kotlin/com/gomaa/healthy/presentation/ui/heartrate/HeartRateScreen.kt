package com.gomaa.healthy.presentation.ui.heartrate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.domain.model.HeartRateReading
import com.gomaa.healthy.domain.model.HeartRateSource
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    modifier = Modifier.padding(bottom = Dimensions.spacingLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No heart rate data yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(Dimensions.spacing))
                Text(
                    text = "Sync from Health Connect to see your heart rate readings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                Button(onClick = { onIntent(HeartRateIntent.OnSync) }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(Dimensions.spacingSmall))
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
                verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
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
                if (uiState.hourlyReadings.isNotEmpty()) {
                    item {
                        Text(
                            text = "Readings by Hour",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    uiState.hourlyReadings.forEach { (hour, readings) ->
                        item {
                            HourlyReadingSummary(hour = hour, readings = readings)
                        }
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
                Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
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
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
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
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
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
                .padding(Dimensions.spacingMedium),
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

// HC-067: Use remember for DateTimeFormatter to avoid recreation on every composition
@Composable
private fun HeartRateReadingItem(reading: HeartRateReading) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(reading.timestamp), ZoneId.systemDefault()
                    ).format(timeFormatter),
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

@Composable
private fun HourlyReadingSummary(hour: Int, readings: List<HeartRateReading>) {
    var isExpanded by remember { mutableStateOf(false) }
    val avgBpm = readings.map { it.bpm }.average().toInt()
    val minBpm = readings.minOf { it.bpm }
    val maxBpm = readings.maxOf { it.bpm }
    val count = readings.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }) {
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
                    text = "${if (hour == 0) 12 else if (hour > 12) hour - 12 else hour}:00 ${if (hour >= 12) "PM" else "AM"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "$avgBpm BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
            Text(
                text = "Readings: $count | Min: $minBpm | Max: $maxBpm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = Dimensions.spacingMedium)
                ) {
                    readings.forEach { reading ->
                        HeartRateReadingItem(reading = reading)
                        Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
                    }
                }
            }
        }
    }
}
