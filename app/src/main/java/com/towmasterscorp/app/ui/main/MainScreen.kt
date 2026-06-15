package com.towmasterscorp.app.ui.main

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.Call
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
        Box(modifier = Modifier.weight(1f)) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> DashboardScreen(user = user)
                    1 -> CallsScreen(title = "Dispatch Board", activeOnly = true, user = user)
                    2 -> CallsScreen(title = "All Calls", activeOnly = false, user = user)
                    3 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                }
            } else {
                when (selectedTab) {
                    0 -> CallsScreen(title = "My Jobs", activeOnly = true, user = user, driverOnly = true)
                    1 -> CallsScreen(title = "All Calls", activeOnly = false, user = user)
                    2 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                }
            }
        }

        // Bottom tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) Color(0xFF1A237E) else Color.Gray
                    )
                    if (selectedTab == index) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(3.dp)
                                .background(Color(0xFF1A237E), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallsScreen(title: String, activeOnly: Boolean, user: User, driverOnly: Boolean = false) {
    var calls by remember { mutableStateOf<List<Call>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadCalls() {
        isLoading = true
        error = null
        Thread {
            try {
                val endpoint = if (activeOnly) "calls.php?action=active" else "calls.php?action=list"
                val url = java.net.URL("https://app.towmasterscorp.com/api/$endpoint")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val responseText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = org.json.JSONObject(responseText)
                if (json.optBoolean("success")) {
                    val callsArray = json.optJSONArray("calls") ?: org.json.JSONArray()
                    val parsed = mutableListOf<Call>()
                    for (i in 0 until callsArray.length()) {
                        val c = callsArray.getJSONObject(i)
                        val call = Call(
                            id = c.optInt("id"),
                            callNumber = c.optString("call_number", ""),
                            status = c.optString("status", "pending"),
                            priority = c.optString("priority", "normal"),
                            callType = c.optString("call_type", "tow"),
                            assignedDriverId = if (c.isNull("assigned_driver_id")) null else c.optInt("assigned_driver_id"),
                            driverName = c.optString("driver_name", null),
                            truckNumber = c.optString("truck_number", null),
                            customerName = c.optString("customer_name", null),
                            callerName = c.optString("caller_name", null),
                            callerPhone = c.optString("caller_phone", null),
                            vehicleMake = c.optString("vehicle_make", null),
                            vehicleModel = c.optString("vehicle_model", null),
                            pickupAddress = c.optString("pickup_address", null),
                            pickupCity = c.optString("pickup_city", null),
                            pickupState = c.optString("pickup_state", null),
                            dropoffAddress = c.optString("dropoff_address", null),
                            createdAt = c.optString("created_at", null)
                        )
                        if (driverOnly && call.assignedDriverId != user.id) continue
                        parsed.add(call)
                    }
                    handler.post { calls = parsed
                        isLoading = false
                    }
                } else {
                    handler.post { error = "Failed to load"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Log.e("CallsScreen", "Load failed", e)
                handler.post { error = e.message
                    isLoading = false
                }
            }
        }.start()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { loadCalls() },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Loading..." else "Load")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Loading calls...", color = Color.Gray)
            }
        } else if (error != null) {
            Text(text = "Error: $error", color = Color.Red, modifier = Modifier.padding(16.dp))
        } else if (calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tap Load to fetch calls", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                calls.forEach { call ->
                    CallCard(call = call, user = user, onStatusUpdated = { loadCalls() })
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CallCard(call: Call, user: User, onStatusUpdated: () -> Unit) {
    var isUpdating by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(call.callNumber ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = call.statusDisplayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (call.status) {
                        "pending" -> Color(0xFFFF9800)
                        "dispatched" -> Color(0xFF2196F3)
                        "en_route" -> Color(0xFF9C27B0)
                        "on_scene" -> Color(0xFF009688)
                        "hooked", "in_transit" -> Color(0xFF00BCD4)
                        "delivered", "destination_arrival" -> Color(0xFF4CAF50)
                        "completed" -> Color.Gray
                        "canceled" -> Color.Red
                        else -> Color.Gray
                    }
                )
            }

            if (!call.vehicleDescription.isNullOrEmpty()) {
                Text(call.vehicleDescription, fontSize = 13.sp, color = Color.DarkGray)
            }
            Text(call.pickupAddress ?: "No address", fontSize = 13.sp, color = Color.Gray, maxLines = 1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(call.callTypeDisplay, fontSize = 12.sp, color = Color(0xFF1A237E))
                Text(call.driverName ?: "Unassigned", fontSize = 12.sp, color = Color.Gray)
            }

            // Status update button for drivers
            if (user.isDriver && call.nextDriverStatus != null && !isUpdating) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isUpdating = true
                        val newStatus = call.nextDriverStatus!!
                        Thread {
                            try {
                                val url = java.net.URL("https://app.towmasterscorp.com/api/calls.php?action=update&id=${call.id}")
                                val conn = url.openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "PUT"
                                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                                conn.setRequestProperty("Content-Type", "application/json")
                                conn.doOutput = true
                                conn.outputStream.bufferedWriter().use {
                                    it.write("""{"status":"$newStatus"}""")
                                }
                                conn.responseCode
                                conn.disconnect()
                                handler.post {
                                    isUpdating = false
                                    onStatusUpdated()
                                }
                            } catch (e: Exception) {
                                Log.e("CallCard", "Update failed", e)
                                handler.post {
                                    isUpdating = false
                                }
                            }
                        }.start()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update to: ${call.nextStatusDisplayName}")
                }
            }
            if (isUpdating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SimpleMoreScreen(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "More", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = user.fullName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text = user.email, fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = user.role.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                ApiClient.token = null
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Logout", color = Color.White)
        }
    }
}
