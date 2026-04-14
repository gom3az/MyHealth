package com.gomaa.healthy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gomaa.healthy.data.preferences.AppPreferencesManager
import com.gomaa.healthy.data.repository.HealthConnectRepository
import com.gomaa.healthy.data.repository.HealthConnectResult
import com.gomaa.healthy.presentation.navigation.AppNavHost
import com.gomaa.healthy.presentation.navigation.Screen
import com.gomaa.healthy.presentation.ui.theme.HealthTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager

    @Inject
    lateinit var healthConnectRepository: HealthConnectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                try {
                    val prefs = appPreferencesManager.getPreferences()

                    // Check if Health Connect is available and has permissions
                    val hcAvailable = when (val result = healthConnectRepository.isAvailable()) {
                        is HealthConnectResult.Success -> result.data
                        else -> false
                    }

                    val hasPermissions =
                        when (val result = healthConnectRepository.hasPermissions()) {
                            is HealthConnectResult.Success -> result.data
                            else -> false
                        }

                    val shouldMigrate =
                        prefs.showMigrationPrompt && !prefs.hasRunMigration && hcAvailable && hasPermissions

                    startDestination = if (shouldMigrate) {
                        Screen.Migration.route
                    } else {
                        Screen.Home.route
                    }
                } catch (_: Exception) {
                    startDestination = Screen.Home.route
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                HealthTheme {
                    if (startDestination != null) {
                        AppNavHost(
                            navController = navController, startDestination = startDestination!!
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}