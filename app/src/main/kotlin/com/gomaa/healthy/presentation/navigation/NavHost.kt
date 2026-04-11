package com.gomaa.healthy.presentation.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gomaa.healthy.presentation.ui.analytics.AnalyticsScreen
import com.gomaa.healthy.presentation.ui.dashboard.DashboardScreen
import com.gomaa.healthy.presentation.ui.goals.GoalsScreen
import com.gomaa.healthy.presentation.ui.home.HomeScreen

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Goals : Screen("goals", "Goals")
    object Analytics : Screen("analytics", "Analytics")
    object SessionDetail : Screen("session/{sessionId}", "Session") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Dashboard, Screen.Goals, Screen.Analytics
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .padding(bottom = paddingValues.calculateBottomPadding())
                .navigationBarsPadding()
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

            composable(Screen.Goals.route) {
                GoalsScreen()
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.SessionDetail.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                AnalyticsScreen()
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
                icon = { Text(getIconForScreen(screen)) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route)
                })
        }
    }
}

private fun getIconForScreen(screen: Screen): String {
    return when (screen) {
        is Screen.Home -> "🏠"
        is Screen.Dashboard -> "📊"
        is Screen.Goals -> "🎯"
        is Screen.Analytics -> "📈"
        is Screen.SessionDetail -> "🏃"
    }
}