package com.gomaa.healthy.presentation.ui.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.preferences.SyncPreferencesManager
import com.gomaa.healthy.presentation.ui.theme.Dimensions

@Composable
fun HealthKitSection(
    isSignedIn: Boolean,
    authState: AuthState,
    syncWindowDays: Int,
    lastSyncTime: Long?,
    isSyncing: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncNow: () -> Unit,
    onSyncWindowChanged: (Int) -> Unit
) {
    var showSyncWindowDialog by remember { mutableStateOf(false) }

    if (showSyncWindowDialog) {
        AlertDialog(
            onDismissRequest = { showSyncWindowDialog = false },
            title = { Text("Sync Window") },
            text = {
                Column {
                    Text(
                        text = "Select how much historical data to sync:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
                    SyncPreferencesManager.SYNC_WINDOW_OPTIONS.forEach { days ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSyncWindowChanged(days)
                                    showSyncWindowDialog = false
                                }
                                .padding(vertical = Dimensions.spacing)
                        ) {
                            RadioButton(
                                selected = syncWindowDays == days,
                                onClick = {
                                    onSyncWindowChanged(days)
                                    showSyncWindowDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimensions.spacing))
                            Text(SyncPreferencesManager.getSyncWindowLabel(days))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncWindowDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "📱",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { }
                )
                Spacer(modifier = Modifier.width(Dimensions.spacingLarge))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Huawei Health Kit",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            !isSignedIn -> "Not connected"
                            authState == AuthState.TOKEN_EXPIRED -> "Token expired"
                            else -> "Connected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSignedIn && authState == AuthState.SIGNED_IN)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            if (!isSignedIn) {
                Text(
                    text = "Connect to Huawei Health Kit to sync your health data from Huawei devices and Health app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimensions.spacingMedium))
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to Huawei Health Kit")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSyncWindowDialog = true }
                        .padding(vertical = Dimensions.spacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sync Window",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = SyncPreferencesManager.getSyncWindowLabel(syncWindowDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (lastSyncTime != null) {
                    val timeSinceMinutes = (System.currentTimeMillis() - lastSyncTime) / 60_000
                    Text(
                        text = if (timeSinceMinutes < 1) "Last sync: Just now"
                        else "Last sync: ${timeSinceMinutes}m ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacing)
                ) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
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

                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}