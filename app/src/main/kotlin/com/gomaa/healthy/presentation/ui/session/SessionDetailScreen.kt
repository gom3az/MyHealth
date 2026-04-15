package com.gomaa.healthy.presentation.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String, viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details", style = MaterialTheme.typography.displaySmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }) { paddingValues ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.session != null -> {
                val session = uiState.session!!
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimensions.contentPadding)
                ) {
                    item {
                        Text(
                            text = "Exercise Session",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                    }

                    item {
                        DetailItem(label = "Session ID", value = session.id)
                    }

                    item {
                        DetailItem(
                            label = "Date", value = formatTimestamp(session.startTime)
                        )
                    }

                    item {
                        val durationMinutes = (session.endTime - session.startTime) / 60000
                        DetailItem(label = "Duration", value = "$durationMinutes minutes")
                    }

                    item {
                        DetailItem(label = "Device", value = session.deviceBrand)
                    }

                    item {
                        DetailItem(
                            label = "Avg Heart Rate", value = "${session.avgHeartRate} bpm"
                        )
                    }

                    item {
                        DetailItem(
                            label = "Max Heart Rate", value = "${session.maxHeartRate} bpm"
                        )
                    }

                    item {
                        DetailItem(
                            label = "Min Heart Rate", value = "${session.minHeartRate} bpm"
                        )
                    }

                    if (session.heartRates.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Dimensions.spacingExtraLarge))
                            Text(
                                text = "Heart Rate Data",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spacing))
                        }

                        session.heartRates.take(10).forEach { hr ->
                            item {
                                DetailItem(
                                    label = formatTimestamp(hr.timestamp), value = "${hr.bpm} bpm"
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Session not found")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String, value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.spacingSmall)
            .clip(RoundedCornerShape(Dimensions.cardRadius)), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimensions.spacingSmall))
            Text(
                text = value, style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}