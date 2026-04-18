package com.gomaa.healthy.presentation.ui.dailysteps

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.gomaa.healthy.domain.model.SourceFilterOption
import com.gomaa.healthy.presentation.ui.shared.DateFilterChip
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import com.gomaa.healthy.presentation.ui.theme.HealthTopAppBarWithBack

private val StepsPrimary = Color(0xFF00C853)
private val StepsPrimaryLight = Color(0xFF69F0AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStepsScreen(
    viewModel: DailyStepsViewModel = hiltViewModel(), onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.handleIntent(DailyStepsIntent.LoadData)
    }

    Scaffold(
        topBar = {
            HealthTopAppBarWithBack(
                title = "Daily Steps",
                onBack = onNavigateBack,
                titleStyle = MaterialTheme.typography.displaySmall,
            )
        }) { paddingValues ->
        DailyStepsContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onIntent = { intent -> viewModel.handleIntent(intent) },
            viewModel = viewModel
        )
    }
}

@Composable
private fun DailyStepsContent(
    paddingValues: PaddingValues,
    uiState: DailyStepsState,
    onIntent: (DailyStepsIntent) -> Unit,
    viewModel: DailyStepsViewModel
) {
    val pagingItems = viewModel.pagingData.collectAsLazyPagingItems()

    when (uiState) {
        is DailyStepsState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = StepsPrimary)
            }
        }

        is DailyStepsState.Empty -> {
            DailyStepsEmptyContent(paddingValues = paddingValues)
        }

        is DailyStepsState.Loaded -> {
            DailyStepsLoadedContent(
                paddingValues = paddingValues,
                uiState = uiState,
                onIntent = onIntent,
                pagingItems = pagingItems
            )
        }

        is DailyStepsState.Error -> {
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
                    onClick = { onIntent(DailyStepsIntent.LoadData) },
                    shape = RoundedCornerShape(Dimensions.buttonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StepsPrimary, contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        vertical = Dimensions.buttonPaddingVertical,
                        horizontal = Dimensions.buttonPaddingHorizontal
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun DailyStepsEmptyContent(
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Dimensions.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsWalk,
            contentDescription = null,
            modifier = Modifier.padding(bottom = Dimensions.spacingLarge),
            tint = StepsPrimaryLight
        )
        Text(
            text = "No steps data yet", style = MaterialTheme.typography.headlineLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyStepsLoadedContent(
    paddingValues: PaddingValues,
    uiState: DailyStepsState.Loaded,
    onIntent: (DailyStepsIntent) -> Unit,
    pagingItems: androidx.paging.compose.LazyPagingItems<com.gomaa.healthy.domain.model.DailySteps>
) {
    if (pagingItems.itemCount == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimensions.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.padding(bottom = Dimensions.spacingLarge),
                tint = StepsPrimaryLight
            )
            Text(
                text = "No steps data yet", style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(Dimensions.spacing))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimensions.contentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.verticalSpacing)
        ) {
            Text(
                text = "Steps by Day", style = MaterialTheme.typography.headlineMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceFilterChips(
                    selectedFilter = uiState.sourceFilter,
                    availableFilters = uiState.availableFilters,
                    onFilterChanged = { onIntent(DailyStepsIntent.SourceFilterChanged(it)) },
                    modifier = Modifier.weight(1f)
                )

                DateFilterChip(
                    selectedFilter = uiState.dateFilter,
                    onFilterChanged = { onIntent(DailyStepsIntent.DateFilterChanged(it)) })
            }

            if (uiState.isSyncing) {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = StepsPrimary, modifier = Modifier.padding(Dimensions.spacing)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    count = pagingItems.itemCount,

                    ) { index ->
                    val item = pagingItems[index] ?: return@items
                    DailyStepsCard(
                        date = item.date.toString(),
                        totalSteps = item.totalSteps,
                        source = item.source.name,
                        goalSteps = 10000,
                        modifier = Modifier.padding(bottom = Dimensions.verticalSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceFilterChips(
    selectedFilter: String?,
    availableFilters: List<SourceFilterOption>,
    onFilterChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableFilters.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.horizontalSpacing)
    ) {
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

@Composable
private fun DailyStepsCard(
    date: String, totalSteps: Int, goalSteps: Int, source: String, modifier: Modifier = Modifier
) {
    val goalProgress =
        if (goalSteps > 0) (totalSteps.toFloat() / goalSteps).coerceIn(0f, 1f) else 0f
    val goalMet = totalSteps >= goalSteps

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius)),
        colors = CardDefaults.cardColors(
            containerColor = if (goalMet) Color(0xFFE8F5E9) else Color.White
        ),
        shape = RoundedCornerShape(Dimensions.cardRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF0D3311)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = if (goalMet) StepsPrimary else StepsPrimaryLight,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "$totalSteps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (goalMet) StepsPrimary else Color(0xFF0D3311)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.spacing))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(goalProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.spacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Goal: $goalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (goalMet) StepsPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}