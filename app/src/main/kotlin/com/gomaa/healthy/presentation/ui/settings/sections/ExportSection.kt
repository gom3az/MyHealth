package com.gomaa.healthy.presentation.ui.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gomaa.healthy.presentation.ui.theme.Dimensions

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