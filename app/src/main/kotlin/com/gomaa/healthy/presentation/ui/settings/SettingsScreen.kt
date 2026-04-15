@file:OptIn(ExperimentalMaterial3Api::class)

package com.gomaa.healthy.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.gomaa.healthy.presentation.ui.theme.HealthTheme
import com.gomaa.healthy.presentation.ui.theme.HealthTopAppBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToGoals: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        viewModel.processIntent(SettingsIntent.HealthConnectPermissionsRequested)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is SettingsSideEffect.RequestHealthConnectPermissions -> {
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
                                })
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
        viewModel.processIntent(SettingsIntent.Initialize)
    }

    val idleState = state as? SettingsUiState.Idle

    SettingsContent(
        onNavigateToGoals = onNavigateToGoals,
        healthConnectAvailable = idleState?.healthConnectAvailable ?: false,
        healthConnectConnected = idleState?.healthConnectConnected ?: false,
        healthConnectStepCount = idleState?.healthConnectStepCount ?: 0,
        healthConnectExerciseSessionCount = idleState?.healthConnectExerciseSessionCount ?: 0,
        healthConnectHeartRateCount = idleState?.healthConnectHeartRateCount ?: 0,
        healthConnectLastSyncTime = idleState?.healthConnectLastSyncTime,
        healthConnectSyncing = idleState?.healthConnectSyncing ?: false,
        healthKitSignedIn = idleState?.healthKitSignedIn ?: false,
        healthKitAuthState = idleState?.healthKitAuthState ?: AuthState.NOT_SIGNED_IN,
        healthKitSyncWindowDays = idleState?.healthKitSyncWindowDays ?: 1,
        healthKitLastSyncTime = idleState?.healthKitLastSyncTime,
        healthKitSyncing = idleState?.healthKitSyncing ?: false,
        syncPreferences = idleState?.syncPreferences ?: SyncPreferences(),
        onRequestHealthConnectPermissions = { viewModel.processIntent(SettingsIntent.RequestHealthConnectPermissions) },
        onSyncHealthConnectNow = { viewModel.processIntent(SettingsIntent.SyncHealthConnectNow) },
        onConnectHealthKit = { viewModel.processIntent(SettingsIntent.ConnectHealthKit) },
        onDisconnectHealthKit = { viewModel.processIntent(SettingsIntent.DisconnectHealthKit) },
        onSyncHealthKitNow = { viewModel.processIntent(SettingsIntent.SyncHealthKitNow) },
        onHealthKitSyncWindowChanged = {
            viewModel.processIntent(
                SettingsIntent.SetHealthKitSyncWindow(
                    it
                )
            )
        },
        onMasterSyncChanged = { viewModel.processIntent(SettingsIntent.SetMasterSync(it)) },
        onStepsSyncChanged = { viewModel.processIntent(SettingsIntent.SetStepsSync(it)) },
        onExerciseSyncChanged = { viewModel.processIntent(SettingsIntent.SetExerciseSync(it)) },
        onHeartRateSyncChanged = { viewModel.processIntent(SettingsIntent.SetHeartRateSync(it)) },
        onExport = { includeHealthConnect ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Export started - including Health Connect: $includeHealthConnect"
                )
            }
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onNavigateToGoals: () -> Unit,
    healthConnectAvailable: Boolean,
    healthConnectConnected: Boolean,
    healthConnectStepCount: Int,
    healthConnectExerciseSessionCount: Int,
    healthConnectHeartRateCount: Int,
    healthConnectLastSyncTime: Long?,
    healthConnectSyncing: Boolean,
    healthKitSignedIn: Boolean,
    healthKitAuthState: AuthState,
    healthKitSyncWindowDays: Int,
    healthKitLastSyncTime: Long?,
    healthKitSyncing: Boolean,
    syncPreferences: SyncPreferences,
    onRequestHealthConnectPermissions: () -> Unit,
    onSyncHealthConnectNow: () -> Unit,
    onConnectHealthKit: () -> Unit,
    onDisconnectHealthKit: () -> Unit,
    onSyncHealthKitNow: () -> Unit,
    onHealthKitSyncWindowChanged: (Int) -> Unit,
    onMasterSyncChanged: (Boolean) -> Unit,
    onStepsSyncChanged: (Boolean) -> Unit,
    onExerciseSyncChanged: (Boolean) -> Unit,
    onHeartRateSyncChanged: (Boolean) -> Unit,
    onExport: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(topBar = {
        HealthTopAppBar(title = "Settings")
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
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

            item { Spacer(modifier = Modifier.height(Dimensions.spacingExtraLarge)) }

            item {
                HealthConnectSection(
                    isAvailable = healthConnectAvailable,
                    isConnected = healthConnectConnected,
                    stepCount = healthConnectStepCount,
                    exerciseSessionCount = healthConnectExerciseSessionCount,
                    heartRateCount = healthConnectHeartRateCount,
                    lastSyncTime = healthConnectLastSyncTime,
                    isSyncing = healthConnectSyncing,
                    onConnect = onRequestHealthConnectPermissions,
                    onSyncNow = onSyncHealthConnectNow
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingExtraLarge)) }

            item {
                HealthKitSection(
                    isSignedIn = healthKitSignedIn,
                    authState = healthKitAuthState,
                    syncWindowDays = healthKitSyncWindowDays,
                    lastSyncTime = healthKitLastSyncTime,
                    isSyncing = healthKitSyncing,
                    onConnect = onConnectHealthKit,
                    onDisconnect = onDisconnectHealthKit,
                    onSyncNow = onSyncHealthKitNow,
                    onSyncWindowChanged = onHealthKitSyncWindowChanged
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingExtraLarge)) }

            item {
                SyncPreferencesSection(
                    preferences = syncPreferences,
                    healthConnectConnected = healthConnectConnected,
                    onMasterSyncChanged = onMasterSyncChanged,
                    onStepsSyncChanged = onStepsSyncChanged,
                    onExerciseSyncChanged = onExerciseSyncChanged,
                    onHeartRateSyncChanged = onHeartRateSyncChanged
                )
            }

            item { Spacer(modifier = Modifier.height(Dimensions.spacingExtraLarge)) }

            item {
                ExportSection(
                    onExport = onExport
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String, description: String, icon: String, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.cardRadius))
            .clickable { onClick() }
            .semantics { contentDescription = "$title: $description" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(Dimensions.cardRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { })
            Spacer(modifier = Modifier.width(Dimensions.spacingLarge))
            Column {
                Text(
                    text = title, style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    HealthTheme {
        SettingsContent(
            onNavigateToGoals = {},
            healthConnectAvailable = true,
            healthConnectConnected = true,
            healthConnectStepCount = 5420,
            healthConnectExerciseSessionCount = 3,
            healthConnectHeartRateCount = 156,
            healthConnectLastSyncTime = System.currentTimeMillis(),
            healthConnectSyncing = false,
            healthKitSignedIn = true,
            healthKitAuthState = AuthState.SIGNED_IN,
            healthKitSyncWindowDays = 7,
            healthKitLastSyncTime = System.currentTimeMillis(),
            healthKitSyncing = false,
            syncPreferences = SyncPreferences(
                masterSyncEnabled = true,
                syncStepsEnabled = true,
                syncExerciseEnabled = true,
                syncHeartRateEnabled = true
            ),
            onRequestHealthConnectPermissions = {},
            onSyncHealthConnectNow = {},
            onConnectHealthKit = {},
            onDisconnectHealthKit = {},
            onSyncHealthKitNow = {},
            onHealthKitSyncWindowChanged = {},
            onMasterSyncChanged = {},
            onStepsSyncChanged = {},
            onExerciseSyncChanged = {},
            onHeartRateSyncChanged = {},
            onExport = {})
    }
}
