@file:OptIn(ExperimentalMaterial3Api::class)

package com.gomaa.healthy.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.preferences.SyncPreferences
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectRepository.Companion.HEALTH_CONNECT_PACKAGE
import com.gomaa.healthy.presentation.ui.settings.sections.ExportSection
import com.gomaa.healthy.presentation.ui.settings.sections.HealthConnectSection
import com.gomaa.healthy.presentation.ui.settings.sections.HealthKitSection
import com.gomaa.healthy.presentation.ui.settings.sections.SyncPreferencesSection
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

                is SettingsSideEffect.RequestHealthKitSignIn -> {
                    viewModel.processIntent(SettingsIntent.ConnectHealthKit)
                }

                is SettingsSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }

                is SettingsSideEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.processIntent(SettingsIntent.CheckHealthConnect)
        viewModel.processIntent(SettingsIntent.CheckHealthKitStatus)
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
                HealthKitSection(
                    isSignedIn = idleState?.healthKitSignedIn ?: false,
                    authState = idleState?.healthKitAuthState ?: AuthState.NOT_SIGNED_IN,
                    syncWindowDays = idleState?.healthKitSyncWindowDays ?: 1,
                    lastSyncTime = idleState?.lastHealthKitSyncTime,
                    isSyncing = idleState?.isHealthKitSyncing ?: false,
                    onConnect = { viewModel.processIntent(SettingsIntent.ConnectHealthKit) },
                    onDisconnect = { viewModel.processIntent(SettingsIntent.DisconnectHealthKit) },
                    onSyncNow = { viewModel.processIntent(SettingsIntent.SyncHealthKitNow) },
                    onSyncWindowChanged = { viewModel.processIntent(SettingsIntent.SetSyncWindow(it)) }
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
private fun SettingsItem(
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