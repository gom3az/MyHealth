package com.gomaa.healthy.presentation.ui.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gomaa.healthy.data.preferences.SyncPreferences
import com.gomaa.healthy.presentation.ui.theme.Dimensions

@Composable
fun SyncPreferencesSection(
    preferences: SyncPreferences,
    healthConnectConnected: Boolean,
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
                enabled = healthConnectConnected,
                onCheckedChange = onMasterSyncChanged
            )

            SyncToggleRow(
                title = "Steps",
                description = "Sync daily step count",
                checked = preferences.syncStepsEnabled && preferences.masterSyncEnabled,
                enabled = healthConnectConnected && preferences.masterSyncEnabled,
                onCheckedChange = onStepsSyncChanged
            )

            SyncToggleRow(
                title = "Exercise Sessions",
                description = "Sync workout sessions",
                checked = preferences.syncExerciseEnabled && preferences.masterSyncEnabled,
                enabled = healthConnectConnected && preferences.masterSyncEnabled,
                onCheckedChange = onExerciseSyncChanged
            )

            SyncToggleRow(
                title = "Heart Rate",
                description = "Sync heart rate readings",
                checked = preferences.syncHeartRateEnabled && preferences.masterSyncEnabled,
                enabled = healthConnectConnected && preferences.masterSyncEnabled,
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