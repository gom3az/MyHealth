package com.gomaa.healthy.healthconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsRationaleScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Privacy & Permissions",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MyHealth needs access to your health data to:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Track your daily steps and exercise sessions\n• Sync health data from Health Connect\n• Provide accurate fitness analytics",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "We value your privacy. Your health data is stored locally on your device and is never shared with third parties without your consent.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data Types Accessed:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Steps (read/write)\n• Exercise Sessions (read/write)\n• Heart Rate (read/write)\n• Sleep (read)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Continue")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Cancel")
            }
        }
    }
}