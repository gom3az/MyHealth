package com.gomaa.healthy.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gomaa.healthy.presentation.ui.analytics.AnalyticsScreen
import com.gomaa.healthy.presentation.ui.dashboard.DashboardScreen
import com.gomaa.healthy.presentation.ui.goals.GoalsScreen
import com.gomaa.healthy.presentation.ui.home.HomeScreen
import com.gomaa.healthy.presentation.ui.session.SessionDetailScreen
import com.gomaa.healthy.presentation.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Home : Screen("home", "Home", "🏠")
    object Dashboard : Screen("dashboard", "Dashboard", "📊")
    object Goals : Screen("goals", "Goals", "🎯")
    object Analytics : Screen("analytics", "Analytics", "📈")
    object Settings : Screen("settings", "Settings", "⚙️")
    object SessionDetail : Screen("session/{sessionId}", "Session", "🏃") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Dashboard, Screen.Analytics, Screen.Settings,
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = {
        BottomNavigationBar(navController = navController)
    }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(bottom = paddingValues.calculateBottomPadding())
                .fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                }, onNavigateToGoals = {
                    navController.navigate(Screen.Goals.route)
                })
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToGoals = {
                    navController.navigate(Screen.Goals.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                    }
                })
            }

            composable(Screen.Goals.route) {
                GoalsScreen()
            }

            composable(
                route = Screen.SessionDetail.route, arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                SessionDetailScreen(sessionId = sessionId)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Text(
                        text = screen.icon,
                        modifier = Modifier.semantics { contentDescription = screen.title })
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                    }
                })
        }
    }
}