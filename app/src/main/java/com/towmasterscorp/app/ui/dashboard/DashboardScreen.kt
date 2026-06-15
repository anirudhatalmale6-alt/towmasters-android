package com.towmasterscorp.app.ui.dashboard

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.theme.*

@Composable
fun DashboardScreen(user: User) {
    var todayCalls by remember { mutableStateOf(0) }
    var activeCalls by remember { mutableStateOf(0) }
    var completedToday by remember { mutableStateOf(0) }
    var todayRevenue by remember { mutableStateOf(0.0) }
    var driversOnline by remember { mutableStateOf(0) }
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
                    handler.post {
                        todayCalls = tc
                        activeCalls = ac
                        completedToday = ct
                        todayRevenue = tr
                        driversOnline = dr
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
        kotlinx.coroutines.delay(500)
        loadDashboard()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Welcome, ${user.firstName}",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.Gray, fontSize = 16.sp)
            }
        }

        if (error != null) {
            Text(text = "$error", color = Color.Red, fontSize = 13.sp)
        }

        // Stats cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimpleStatCard(Modifier.weight(1f), "Total Calls", "$todayCalls", Primary)
            SimpleStatCard(Modifier.weight(1f), "Active", "$activeCalls", Secondary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimpleStatCard(Modifier.weight(1f), "Completed", "$completedToday", Success)
            SimpleStatCard(Modifier.weight(1f), "Drivers", "$driversOnline", Info)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Revenue", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "$${String.format("%.2f", todayRevenue)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { loadDashboard() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Refresh")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SimpleStatCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
