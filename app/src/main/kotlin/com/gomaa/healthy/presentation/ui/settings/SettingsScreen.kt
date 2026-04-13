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
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectRepository.Companion.HEALTH_CONNECT_PACKAGE
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                SettingsItem(
                    title = "Goals",
                    description = "Set daily step goals and targets",
                    icon = "🎯",
                    onClick = onNavigateToGoals
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                HealthConnectSection(
                    isAvailable = idleState?.isAvailable ?: false,
                    isConnected = idleState?.isConnected ?: false,
                    stepCount = idleState?.stepCount ?: 0,
                    exerciseSessionCount = idleState?.exerciseSessionCount ?: 0,
                    lastSyncTime = idleState?.lastSyncTime,
                    isSyncing = idleState?.isSyncing ?: false,
                    onConnect = {
                        viewModel.processIntent(SettingsIntent.RequestHealthConnectPermissions)
                    },
                    onSyncNow = { viewModel.processIntent(SettingsIntent.SyncNow) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeHealthConnect = true }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = includeHealthConnect,
                            onClick = { includeHealthConnect = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All data (MyHealth + Health Connect)")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeHealthConnect = false }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = !includeHealthConnect,
                            onClick = { includeHealthConnect = false })
                        Spacer(modifier = Modifier.width(8.dp))
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📤",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { }
            )
            Spacer(modifier = Modifier.width(16.dp))
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
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏥",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { }
                )
                Spacer(modifier = Modifier.width(16.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAvailable) {
                Text(
                    text = "Health Connect app is not installed. Install it to sync your health data.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    }
                    if (lastSyncTime != null) {
                        val timeSinceMinutes = (System.currentTimeMillis() - lastSyncTime) / 60_000
                        Text(
                            text = if (timeSinceMinutes < 1) "Just now" else "${timeSinceMinutes}m ago",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        Spacer(modifier = Modifier.width(8.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { }
            )
            Spacer(modifier = Modifier.width(16.dp))
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
