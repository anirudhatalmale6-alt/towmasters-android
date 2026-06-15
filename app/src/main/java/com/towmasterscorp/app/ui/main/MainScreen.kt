package com.towmasterscorp.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.ui.dashboard.DashboardScreen
import com.towmasterscorp.app.ui.calls.CallsListScreen
import com.towmasterscorp.app.ui.dispatch.DispatchBoardScreen
import com.towmasterscorp.app.ui.driver.DriverJobsScreen
import com.towmasterscorp.app.ui.more.MoreMenuScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: User,
    authPreferences: AuthPreferences,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val isAdmin = user.role == "admin" || user.role == "dispatcher"

    Scaffold(
        bottomBar = {
            NavigationBar {
                if (isAdmin) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Dispatch") },
                        label = { Text("Dispatch") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Phone, contentDescription = "Calls") },
                        label = { Text("Calls") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                        label = { Text("More") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                } else {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Work, contentDescription = "My Jobs") },
                        label = { Text("My Jobs") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Phone, contentDescription = "Calls") },
                        label = { Text("Calls") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                        label = { Text("More") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> DashboardScreen(user = user)
                    1 -> DispatchBoardScreen(user = user, onCallClick = {})
                    2 -> CallsListScreen(user = user, onCallClick = {})
                    3 -> MoreMenuScreen(user = user, authPreferences = authPreferences, onLogout = onLogout, onNavigateToChat = {})
                }
            } else {
                when (selectedTab) {
                    0 -> DriverJobsScreen(user = user, onCallClick = {})
                    1 -> CallsListScreen(user = user, onCallClick = {})
                    2 -> MoreMenuScreen(user = user, authPreferences = authPreferences, onLogout = onLogout, onNavigateToChat = {})
                }
            }
        }
    }
}
