package com.towmasterscorp.app.ui.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.User

@Composable
fun DashboardScreen(
    user: User,
    onNewCall: () -> Unit = {},
    onDriverMap: () -> Unit = {},
    onReports: () -> Unit = {}
) {
    var todayCalls by remember { mutableStateOf(0) }
    var activeCalls by remember { mutableStateOf(0) }
    var completedToday by remember { mutableStateOf(0) }
    var todayRevenue by remember { mutableStateOf(0.0) }
    var driversOnline by remember { mutableStateOf(0) }
    var pendingCalls by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadDashboard() {
        isLoading = true
        error = null
        Thread {
            try {
                val url = java.net.URL("https://app.towmasterscorp.com/api/reports.php?action=dashboard")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val responseText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(responseText)
                if (json.optBoolean("success")) {
                    val today = json.optJSONObject("today")
                    val tc = today?.optInt("total_calls", 0) ?: 0
                    val ac = today?.optString("active", "0")?.toIntOrNull() ?: 0
                    val ct = today?.optString("completed", "0")?.toIntOrNull() ?: 0
                    val tr = today?.optString("total_revenue", "0")?.toDoubleOrNull() ?: 0.0
                    val dr = json.optInt("drivers_active", 0)
                    val pc = today?.optString("pending", "0")?.toIntOrNull() ?: 0
                    handler.post {
                        todayCalls = tc
                        activeCalls = ac
                        completedToday = ct
                        todayRevenue = tr
                        driversOnline = dr
                        pendingCalls = pc
                        isLoading = false
                    }
                } else {
                    handler.post { isLoading = false }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Load failed", e)
                handler.post {
                    error = e.message
                    isLoading = false
                }
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        loadDashboard()
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000)
            loadDashboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Welcome, ${user.firstName}", fontSize = 14.sp, color = Color(0xFF8E8E93))
            }
            Text(
                text = if (isLoading) "Loading..." else "Refresh",
                fontSize = 14.sp,
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable(enabled = !isLoading) { loadDashboard() }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (error != null) {
            Text(text = "$error", color = Color.Red, fontSize = 13.sp)
        }

        if (isLoading && todayCalls == 0) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            Text("Today's Overview", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Today's Calls", "$todayCalls", Color(0xFF007AFF))
                StatCard(Modifier.weight(1f), "Completed", "$completedToday", Color(0xFF34C759))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Active", "$activeCalls", Color(0xFFFF9500))
                StatCard(Modifier.weight(1f), "Pending", "$pendingCalls", Color(0xFFFFCC00))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Drivers On", "$driversOnline", Color(0xFFAF52DE))
                StatCard(Modifier.weight(1f), "Revenue", "$${String.format("%.2f", todayRevenue)}", Color(0xFF34C759))
            }
        }

        // Quick Actions
        Text("Quick Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = "New Call",
                icon = Icons.Default.Add,
                color = Color(0xFF007AFF),
                onClick = onNewCall
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = "Active Drivers",
                icon = Icons.Default.People,
                color = Color(0xFF34C759),
                onClick = onDriverMap
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = "Reports",
                icon = Icons.Default.BarChart,
                color = Color(0xFFAF52DE),
                onClick = onReports
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 12.sp, color = Color(0xFF8E8E93))
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
