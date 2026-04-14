package com.gomaa.healthy.presentation.ui.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gomaa.healthy.presentation.ui.theme.Dimensions

@Composable
fun HealthConnectSection(
    isAvailable: Boolean,
    isConnected: Boolean,
    stepCount: Int,
    exerciseSessionCount: Int,
    heartRateCount: Int,
    lastSyncTime: Long?,
    isSyncing: Boolean,
    onConnect: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏥",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { }
                )
                Spacer(modifier = Modifier.width(Dimensions.spacingLarge))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Connect",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            if (!isAvailable) {
                Text(
                    text = "Health Connect app is not installed. Install it to sync your health data.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
                Button(onClick = onConnect) {
                    Text("Install Health Connect")
                }
            } else if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$stepCount steps imported",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$exerciseSessionCount exercises imported",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$heartRateCount heart rate readings imported",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (lastSyncTime != null) {
                        val timeSinceMinutes = (System.currentTimeMillis() - lastSyncTime) / 60_000
                        Text(
                            text = if (timeSinceMinutes < 1) "Just now" else "${timeSinceMinutes}m ago",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(Dimensions.spacing))
                        Text("Syncing...")
                    } else {
                        Text("Sync Now")
                    }
                }
            } else {
                Text(
                    text = "Grant permissions to sync your health data from other apps.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect to Health Connect")
                }
            }
        }
    }
}