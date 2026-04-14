@file:OptIn(ExperimentalMaterial3Api::class)

package com.gomaa.healthy.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gomaa.healthy.data.preferences.SyncPreferences
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectRepository.Companion.HEALTH_CONNECT_PACKAGE
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToGoals: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.processIntent(SettingsIntent.PermissionsRequested)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is SettingsSideEffect.RequestPermissions -> {
                    try {
                        permissionLauncher.launch(HealthConnectRepository.PERMISSIONS)
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error: ${e.message}", e)
                        try {
                            val uriString =
                                "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setPackage("com.android.vending")
                                    data = Uri.parse(uriString)
                                    putExtra("overlay", true)
                                    putExtra("callerId", context.packageName)
                                }
                            )
                        } catch (e2: Exception) {
                            Log.e("SettingsScreen", "Error starting activity: ${e2.message}", e2)
                        }
                    }
                }

                is SettingsSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.processIntent(SettingsIntent.CheckHealthConnect)
    }

    val idleState = state as? SettingsUiState.Idle

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.contentPadding)
        ) {
            item {
                SettingsItem(
                    title = "Goals",
                    description = "Set daily step goals and targets",
                    icon = "🎯",
                    onClick = onNavigateToGoals
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingLarge)) }

            item {
                HealthConnectSection(
                    isAvailable = idleState?.isAvailable ?: false,
                    isConnected = idleState?.isConnected ?: false,
                    stepCount = idleState?.stepCount ?: 0,
                    exerciseSessionCount = idleState?.exerciseSessionCount ?: 0,
                    heartRateCount = idleState?.heartRateCount ?: 0,
                    lastSyncTime = idleState?.lastSyncTime,
                    isSyncing = idleState?.isSyncing ?: false,
                    onConnect = {
                        viewModel.processIntent(SettingsIntent.RequestHealthConnectPermissions)
                    },
                    onSyncNow = { viewModel.processIntent(SettingsIntent.SyncNow) }
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingLarge)) }

            item {
                SyncPreferencesSection(
                    preferences = idleState?.syncPreferences ?: SyncPreferences(),
                    isConnected = idleState?.isConnected ?: false,
                    onMasterSyncChanged = { viewModel.processIntent(SettingsIntent.SetMasterSync(it)) },
                    onStepsSyncChanged = { viewModel.processIntent(SettingsIntent.SetStepsSync(it)) },
                    onExerciseSyncChanged = {
                        viewModel.processIntent(
                            SettingsIntent.SetExerciseSync(
                                it
                            )
                        )
                    },
                    onHeartRateSyncChanged = {
                        viewModel.processIntent(
                            SettingsIntent.SetHeartRateSync(
                                it
                            )
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingLarge)) }

            item {
                ExportSection(
                    onExport = { includeHealthConnect ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Export started - including Health Connect: $includeHealthConnect"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExportSection(
    onExport: (Boolean) -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var includeHealthConnect by remember { mutableStateOf(true) }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = {
                Column {
                    Text("Select data to include in export:")
                    Spacer(modifier = Modifier.height(Dimensions.spacingLarge))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeHealthConnect = true }
                            .padding(vertical = Dimensions.spacing)
                    ) {
                        RadioButton(
                            selected = includeHealthConnect,
                            onClick = { includeHealthConnect = true })
                        Spacer(modifier = Modifier.width(Dimensions.spacing))
                        Text("All data (MyHealth + Health Connect)")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeHealthConnect = false }
                            .padding(vertical = Dimensions.spacing)
                    ) {
                        RadioButton(
                            selected = !includeHealthConnect,
                            onClick = { includeHealthConnect = false })
                        Spacer(modifier = Modifier.width(Dimensions.spacing))
                        Text("MyHealth data only")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onExport(includeHealthConnect)
                    showExportDialog = false
                }) {
                    Text("Export CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showExportDialog = true }
            .semantics { contentDescription = "Export data to CSV" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📤",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { }
            )
            Spacer(modifier = Modifier.width(Dimensions.spacingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Export Data",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Export your health data to CSV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
    onSyncNow: () -> Unit
) {
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
                // Connection details
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

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "$title: $description" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { }
            )
            Spacer(modifier = Modifier.width(Dimensions.spacingLarge))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SyncPreferencesSection(
    preferences: SyncPreferences,
    isConnected: Boolean,
    onMasterSyncChanged: (Boolean) -> Unit,
    onStepsSyncChanged: (Boolean) -> Unit,
    onExerciseSyncChanged: (Boolean) -> Unit,
    onHeartRateSyncChanged: (Boolean) -> Unit
) {
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
            Text(
                text = "Sync Settings",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Control what data syncs to Health Connect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            SyncToggleRow(
                title = "Master Sync",
                description = "Enable/disable all background sync",
                checked = preferences.masterSyncEnabled,
                enabled = isConnected,
                onCheckedChange = onMasterSyncChanged
            )

            SyncToggleRow(
                title = "Steps",
                description = "Sync daily step count",
                checked = preferences.syncStepsEnabled && preferences.masterSyncEnabled,
                enabled = isConnected && preferences.masterSyncEnabled,
                onCheckedChange = onStepsSyncChanged
            )

            SyncToggleRow(
                title = "Exercise Sessions",
                description = "Sync workout sessions",
                checked = preferences.syncExerciseEnabled && preferences.masterSyncEnabled,
                enabled = isConnected && preferences.masterSyncEnabled,
                onCheckedChange = onExerciseSyncChanged
            )

            SyncToggleRow(
                title = "Heart Rate",
                description = "Sync heart rate readings",
                checked = preferences.syncHeartRateEnabled && preferences.masterSyncEnabled,
                enabled = isConnected && preferences.masterSyncEnabled,
                onCheckedChange = onHeartRateSyncChanged
            )
        }
    }
}

@Composable
private fun SyncToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
