package com.towmasterscorp.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.ui.calls.CallDetailScreen
import com.towmasterscorp.app.ui.calls.CallsListScreen
import com.towmasterscorp.app.ui.chat.ChatListScreen
import com.towmasterscorp.app.ui.chat.ChatScreen
import com.towmasterscorp.app.ui.dashboard.DashboardScreen
import com.towmasterscorp.app.ui.dispatch.DispatchBoardScreen
import com.towmasterscorp.app.ui.driver.DriverJobsScreen
import com.towmasterscorp.app.ui.more.MoreMenuScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Dispatch : BottomNavItem("dispatch", "Dispatch", Icons.Default.Assignment)
    data object Calls : BottomNavItem("calls", "Calls", Icons.Default.Phone)
    data object MyJobs : BottomNavItem("my_jobs", "My Jobs", Icons.Default.Work)
    data object More : BottomNavItem("more", "More", Icons.Default.Menu)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: User,
    authPreferences: AuthPreferences,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val adminTabs = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Dispatch,
        BottomNavItem.Calls,
        BottomNavItem.More
    )

    val driverTabs = listOf(
        BottomNavItem.MyJobs,
        BottomNavItem.Calls,
        BottomNavItem.More
    )

    val tabs = if (user.isDispatcher) adminTabs else driverTabs

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                tabs.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (user.isDispatcher) "dashboard" else "my_jobs",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(user = user)
            }
            composable("dispatch") {
                DispatchBoardScreen(
                    user = user,
                    onCallClick = { callId ->
                        navController.navigate("call_detail/$callId")
                    }
                )
            }
            composable("calls") {
                CallsListScreen(
                    user = user,
                    onCallClick = { callId ->
                        navController.navigate("call_detail/$callId")
                    }
                )
            }
            composable("my_jobs") {
                DriverJobsScreen(
                    user = user,
                    onCallClick = { callId ->
                        navController.navigate("call_detail/$callId")
                    }
                )
            }
            composable("more") {
                MoreMenuScreen(
                    user = user,
                    authPreferences = authPreferences,
                    onLogout = onLogout,
                    onNavigateToChat = {
                        navController.navigate("chat_list")
                    }
                )
            }
            composable("call_detail/{callId}") { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId")?.toIntOrNull() ?: 0
                CallDetailScreen(
                    callId = callId,
                    user = user,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("chat_list") {
                ChatListScreen(
                    user = user,
                    onConversationClick = { userId ->
                        navController.navigate("chat/$userId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("chat/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                ChatScreen(
                    currentUser = user,
                    otherUserId = userId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
