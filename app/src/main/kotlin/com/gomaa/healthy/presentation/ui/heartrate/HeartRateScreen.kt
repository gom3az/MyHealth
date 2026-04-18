package com.gomaa.healthy.presentation.ui.heartrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.gomaa.healthy.domain.model.DateRangeFilter
import com.gomaa.healthy.domain.model.HeartRateSummary
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.domain.usecase.HourHeader
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTopAppBarWithBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    viewModel: HeartRateViewModel = hiltViewModel(), onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.processIntent(HeartRateIntent.OnLoadData)
    }

    Scaffold(topBar = {
        HealthTopAppBarWithBack(
            title = "Heart Rate",
            onBack = onNavigateBack,
            titleStyle = MaterialTheme.typography.displaySmall,
            actions = {
                IconButton(
                    onClick = { viewModel.processIntent(HeartRateIntent.OnSync) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            })
    }) { paddingValues ->
        HeartRateContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onIntent = { intent -> viewModel.processIntent(intent) },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeartRateContent(
    paddingValues: PaddingValues,
    uiState: HeartRateUiState,
    onIntent: (HeartRateIntent) -> Unit,
    viewModel: HeartRateViewModel
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    text = "No heart rate data yet", style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(Dimensions.spacing))
                Text(
                    text = "Sync from Health Connect to see your heart rate readings",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                Button(
                    onClick = { onIntent(HeartRateIntent.OnSync) },
                    shape = RoundedCornerShape(Dimensions.buttonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = Dimensions.buttonPaddingVertical,
                        horizontal = Dimensions.buttonPaddingHorizontal
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.padding(Dimensions.spacingSmall))
                    Text("Sync from Health Connect")
                }
            }
        }

        is HeartRateUiState.Loaded -> {
            val lazyPagingItems = viewModel.pagingData.collectAsLazyPagingItems()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.contentPadding),
                verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
            ) {
                uiState.overallSummary?.let { summary ->
                    HeartRateSummaryCards(summary = summary)
                }

                Text(
                    text = "Readings by Hour", style = MaterialTheme.typography.headlineMedium
                )

                SourceFilterChips(
                    selectedFilter = uiState.sourceFilter,
                    availableFilters = uiState.availableFilters,
                    onFilterChanged = { onIntent(HeartRateIntent.OnSourceFilterChanged(it)) })

                DateFilterChip(
                    selectedFilter = uiState.dateFilter,
                    onFilterChanged = { onIntent(HeartRateIntent.OnDateFilterChanged(it)) })

                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        count = lazyPagingItems.itemCount, key = { index ->
                            when (val item = lazyPagingItems.peek(index)) {
                                is HourHeader -> "header_${item.date}_${item.hour}_$index"
                                null -> "placeholder_$index"
                            }
                        }) { index ->
                        val item = lazyPagingItems[index] ?: return@items

                        HourHeaderCard(
                            hour = item.hour,
                            date = item.date,
                            minBpm = item.minBpm,
                            avgBpm = item.avgBpm,
                            maxBpm = item.maxBpm,
                            modifier = Modifier.padding(bottom = Dimensions.verticalSpacing)
                        )
                    }
                    item {
                        val appendState = lazyPagingItems.loadState.append
                        when {
                            appendState is LoadState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            }

                            appendState is LoadState.Error -> {
                                ErrorItem(
                                    message = appendState.error.message ?: "Error",
                                    retry = { lazyPagingItems.retry() })
                            }

                            appendState.endOfPaginationReached && lazyPagingItems.itemCount > 0 -> {
                                Text("End of readings")
                            }
                        }
                    }
                }

                // --- INITIAL REFRESH ---
                val refreshState = lazyPagingItems.loadState.refresh
                if (refreshState is LoadState.Loading && lazyPagingItems.itemCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                Button(
                    onClick = { onIntent(HeartRateIntent.OnRefresh) },
                    shape = RoundedCornerShape(Dimensions.buttonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SourceFilterChips(
    selectedFilter: String?,
    availableFilters: List<SourceFilterOption>,
    onFilterChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
        // ALL option - shows combined data from all sources
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterChanged(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(Dimensions.chipRadius)
        )

        // Dynamic filters from database
        availableFilters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter.id,
                onClick = { onFilterChanged(filter.id) },
                label = { Text(filter.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(Dimensions.chipRadius)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterChip(
    selectedFilter: DateRangeFilter, onFilterChanged: (DateRangeFilter) -> Unit
) {
    // For simplicity, we'll show a text button that cycles through preset date ranges
    TextButton(
        onClick = {
            // Cycle through preset date ranges
            val newFilter = when (selectedFilter) {
                DateRangeFilter.Today -> DateRangeFilter.Last7Days
                DateRangeFilter.Last7Days -> DateRangeFilter.Last30Days
                DateRangeFilter.Last30Days -> DateRangeFilter.All
                DateRangeFilter.All -> DateRangeFilter.Today
                // For Custom, we'll just go to Today for simplicity
                is DateRangeFilter.Custom -> DateRangeFilter.Today
            }
            onFilterChanged(newFilter)
        }, modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = selectedFilter.displayName(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Simple indicator that this is clickable
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select date range",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
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
            title = "Max", value = "${summary.maxBpm}", unit = "BPM", modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Min", value = "${summary.minBpm}", unit = "BPM", modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String, value: String, unit: String, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(Dimensions.cardRadius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Dimensions.cardRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun HourHeaderCard(
    hour: Int, date: String, minBpm: Int, avgBpm: Int, maxBpm: Int, modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(Dimensions.cardRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding)
        ) {
            // Date and time header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$date, ${formatHour(hour)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "$avgBpm BPM",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            // Min/Avg/Max stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Min", value = "$minBpm")
                StatItem(label = "Avg", value = "$avgBpm")
                StatItem(label = "Max", value = "$maxBpm")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun formatHour(hour: Int): String {
    return "${if (hour == 0) 12 else if (hour > 12) hour - 12 else hour}:00 ${if (hour >= 12) "PM" else "AM"}"
}

@Composable
private fun ErrorItem(message: String, retry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = retry) {
            Text("Retry")
        }
    }
}
