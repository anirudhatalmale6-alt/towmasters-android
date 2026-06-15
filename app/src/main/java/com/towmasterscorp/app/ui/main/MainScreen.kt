package com.towmasterscorp.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.ui.dashboard.DashboardScreen

@Composable
fun MainScreen(
    user: User,
    authPreferences: AuthPreferences,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val isAdmin = user.role == "admin" || user.role == "dispatcher"
    val tabs = if (isAdmin) listOf("Dashboard", "Dispatch", "Calls", "More") else listOf("My Jobs", "Calls", "More")

    Column(modifier = Modifier.fillMaxSize()) {
        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when {
                isAdmin && selectedTab == 0 -> DashboardScreen(user = user)
                isAdmin && selectedTab == 3 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                !isAdmin && selectedTab == tabs.size - 1 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                else -> PlaceholderScreen(tabs[selectedTab])
            }
        }

        // Simple bottom tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) Color(0xFF1A237E) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title - Coming soon", fontSize = 18.sp, color = Color.Gray)
    }
}

@Composable
fun SimpleMoreScreen(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "More", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "${user.fullName}", fontSize = 18.sp)
        Text(text = user.email, fontSize = 14.sp, color = Color.Gray)
        Text(text = "Role: ${user.role}", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.Button(
            onClick = {
                com.towmasterscorp.app.data.api.ApiClient.token = null
                onLogout()
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Text("Logout", color = Color.White)
        }
    }
}
