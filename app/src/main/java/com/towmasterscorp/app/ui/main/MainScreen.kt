package com.towmasterscorp.app.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.Call
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.ui.dashboard.DashboardScreen

fun getStatusColor(status: String): Color = when (status) {
    "pending" -> Color(0xFFFF9500)
    "scheduled" -> Color(0xFFFF9500)
    "dispatched" -> Color(0xFF007AFF)
    "en_route" -> Color(0xFFAF52DE)
    "on_scene" -> Color(0xFF5856D6)
    "hooked", "in_transit" -> Color(0xFF5AC8FA)
    "delivered", "destination_arrival" -> Color(0xFF34C759)
    "completed" -> Color(0xFF8E8E93)
    "canceled" -> Color(0xFFFF3B30)
    else -> Color(0xFF8E8E93)
}

fun getStatusDisplayName(status: String): String = when (status) {
    "pending" -> "Pending"
    "scheduled" -> "Scheduled"
    "dispatched" -> "Dispatched"
    "en_route" -> "En Route"
    "on_scene" -> "On Scene"
    "hooked" -> "Hooked"
    "in_transit" -> "In Transit"
    "delivered" -> "Delivered"
    "destination_arrival" -> "Dest. Arrival"
    "completed" -> "Completed"
    "canceled" -> "Canceled"
    else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
}

@Composable
fun MainScreen(
    user: User,
    authPreferences: AuthPreferences,
    onLogout: () -> Unit
) {
    var selectedCallId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { fcmToken ->
                    Thread {
                        try {
                            val url = java.net.URL("https://app.towmasterscorp.com/api/auth.php?action=update-device-token")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.doOutput = true
                            val body = org.json.JSONObject()
                            body.put("device_token", fcmToken)
                            body.put("device_platform", "android")
                            conn.outputStream.write(body.toString().toByteArray())
                            val code = conn.responseCode
                            Log.d("MainScreen", "FCM token registered: $code")
                            conn.disconnect()
                        } catch (e: Exception) {
                            Log.e("MainScreen", "FCM register failed", e)
                        }
                    }.start()
                }
        } catch (e: Exception) {
            Log.e("MainScreen", "FCM token error", e)
        }
    }

    if (selectedCallId != null) {
        CallDetailContent(
            callId = selectedCallId!!,
            user = user,
            onBack = { selectedCallId = null }
        )
    } else {
        MainTabContent(user = user, onLogout = onLogout, onCallClick = { selectedCallId = it })
    }
}

@Composable
private fun MainTabContent(
    user: User,
    onLogout: () -> Unit,
    onCallClick: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val isAdmin = user.role == "admin" || user.role == "dispatcher"

    data class TabItem(val title: String, val icon: ImageVector)
    val tabs = if (isAdmin) {
        listOf(
            TabItem("Dashboard", Icons.Default.Home),
            TabItem("Dispatch", Icons.Default.List),
            TabItem("Calls", Icons.Default.Phone),
            TabItem("More", Icons.Default.MoreVert)
        )
    } else {
        listOf(
            TabItem("My Jobs", Icons.Default.Work),
            TabItem("Calls", Icons.Default.Phone),
            TabItem("More", Icons.Default.MoreVert)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).background(Color(0xFFF2F2F7))) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> DashboardScreen(user = user)
                    1 -> CallsScreen("Dispatch Board", activeOnly = true, user = user, onCallClick = onCallClick)
                    2 -> CallsScreen("All Calls", activeOnly = false, user = user, onCallClick = onCallClick)
                    3 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                }
            } else {
                when (selectedTab) {
                    0 -> CallsScreen("My Jobs", activeOnly = true, user = user, driverOnly = true, onCallClick = onCallClick)
                    1 -> CallsScreen("All Calls", activeOnly = false, user = user, onCallClick = onCallClick)
                    2 -> SimpleMoreScreen(user = user, onLogout = onLogout)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFD1D1D6)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF9F9F9))
                .padding(top = 6.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(22.dp),
                        tint = if (selectedTab == index) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        color = if (selectedTab == index) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
}

@Composable
fun CallsScreen(
    title: String,
    activeOnly: Boolean,
    user: User,
    driverOnly: Boolean = false,
    onCallClick: (Int) -> Unit
) {
    var calls by remember { mutableStateOf<List<Call>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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
                            vehicleYear = c.opt("vehicle_year"),
                            vehicleMake = c.optString("vehicle_make", null),
                            vehicleModel = c.optString("vehicle_model", null),
                            vehicleColor = c.optString("vehicle_color", null),
                            vehicleLicense = c.optString("vehicle_license", null),
                            pickupAddress = c.optString("pickup_address", null),
                            pickupCity = c.optString("pickup_city", null),
                            pickupState = c.optString("pickup_state", null),
                            dropoffAddress = c.optString("dropoff_address", null),
                            dropoffCity = c.optString("dropoff_city", null),
                            dropoffState = c.optString("dropoff_state", null),
                            dispatchNotes = c.optString("dispatch_notes", null),
                            driverNotes = c.optString("driver_notes", null),
                            reasonForTow = c.optString("reason_for_tow", null),
                            totalAmount = c.opt("total_amount"),
                            paymentStatus = c.optString("payment_status", null),
                            createdAt = c.optString("created_at", null)
                        )
                        if (driverOnly && call.assignedDriverId != user.id) continue
                        parsed.add(call)
                    }
                    handler.post {
                        calls = parsed
                        isLoading = false
                    }
                } else {
                    handler.post {
                        error = "Failed to load"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Log.e("CallsScreen", "Load failed", e)
                handler.post {
                    error = e.message
                    isLoading = false
                }
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        loadCalls()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (isLoading) "Loading..." else "Refresh",
                fontSize = 14.sp,
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable(enabled = !isLoading) { loadCalls() }
            )
        }

        if (isLoading && calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.Gray, fontSize = 16.sp)
            }
        } else if (error != null && calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tap to retry",
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable { loadCalls() }
                    )
                }
            }
        } else if (calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No calls found", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                calls.forEach { call ->
                    CallCard(
                        call = call,
                        onClick = { onCallClick(call.id) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = getStatusColor(status)
    val name = getStatusDisplayName(status)
    Text(
        text = name,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun CallCard(call: Call, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = call.callNumber ?: "#${call.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                StatusBadge(call.status)
            }

            if (call.vehicleDescription.isNotEmpty()) {
                Text(
                    text = call.vehicleDescription,
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
            }

            Text(
                text = call.pickupAddress ?: "No address",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val cityState = listOfNotNull(
                call.pickupCity?.ifEmpty { null },
                call.pickupState?.ifEmpty { null }
            ).joinToString(", ")
            if (cityState.isNotEmpty()) {
                Text(text = cityState, fontSize = 11.sp, color = Color(0xFFAEAEB2))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = call.driverName ?: "Unassigned",
                    fontSize = 12.sp,
                    color = Color(0xFF007AFF)
                )
                Text(
                    text = call.callTypeDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF),
                    modifier = Modifier
                        .background(Color(0xFF007AFF).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CallDetailContent(callId: Int, user: User, onBack: () -> Unit) {
    var call by remember { mutableStateOf<Call?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadCall() {
        isLoading = true
        error = null
        Thread {
            try {
                val url = java.net.URL("https://app.towmasterscorp.com/api/calls.php?action=get&id=$callId")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val responseText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(responseText)
                if (json.optBoolean("success")) {
                    val c = json.getJSONObject("call")
                    val parsed = Call(
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
                        contactName = c.optString("contact_name", null),
                        contactPhone = c.optString("contact_phone", null),
                        vehicleYear = c.opt("vehicle_year"),
                        vehicleMake = c.optString("vehicle_make", null),
                        vehicleModel = c.optString("vehicle_model", null),
                        vehicleColor = c.optString("vehicle_color", null),
                        vehicleVin = c.optString("vehicle_vin", null),
                        vehicleLicense = c.optString("vehicle_license", null),
                        plateState = c.optString("plate_state", null),
                        pickupAddress = c.optString("pickup_address", null),
                        pickupCity = c.optString("pickup_city", null),
                        pickupState = c.optString("pickup_state", null),
                        pickupZip = c.optString("pickup_zip", null),
                        dropoffAddress = c.optString("dropoff_address", null),
                        dropoffCity = c.optString("dropoff_city", null),
                        dropoffState = c.optString("dropoff_state", null),
                        dropoffZip = c.optString("dropoff_zip", null),
                        baseRate = c.opt("base_rate"),
                        mileageRate = c.opt("mileage_rate"),
                        mileage = c.opt("mileage"),
                        additionalCharges = c.opt("additional_charges"),
                        totalAmount = c.opt("total_amount"),
                        paymentStatus = c.optString("payment_status", null),
                        paymentMethod = c.optString("payment_method", null),
                        dispatchNotes = c.optString("dispatch_notes", null),
                        driverNotes = c.optString("driver_notes", null),
                        reasonForTow = c.optString("reason_for_tow", null),
                        poNumber = c.optString("po_number", null),
                        dispatcherName = c.optString("dispatcher_name", null),
                        dispatchedAt = c.optString("dispatched_at", null),
                        enRouteAt = c.optString("en_route_at", null),
                        onSceneAt = c.optString("on_scene_at", null),
                        hookedAt = c.optString("hooked_at", null),
                        deliveredAt = c.optString("delivered_at", null),
                        completedAt = c.optString("completed_at", null),
                        createdAt = c.optString("created_at", null)
                    )
                    handler.post {
                        call = parsed
                        isLoading = false
                    }
                } else {
                    handler.post {
                        error = json.optString("error", "Failed to load call")
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Log.e("CallDetail", "Load failed", e)
                handler.post {
                    error = e.message
                    isLoading = false
                }
            }
        }.start()
    }

    fun updateStatus(newStatus: String) {
        isUpdating = true
        updateMsg = null
        Thread {
            try {
                val url = java.net.URL("https://app.towmasterscorp.com/api/calls.php?action=update&id=$callId")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("""{"status":"$newStatus"}""".toByteArray())
                val code = conn.responseCode
                conn.disconnect()
                handler.post {
                    isUpdating = false
                    if (code in 200..299) {
                        updateMsg = "Status updated!"
                        loadCall()
                    } else {
                        updateMsg = "Update failed (HTTP $code)"
                    }
                }
            } catch (e: Throwable) {
                Log.e("CallDetail", "Update failed", e)
                handler.post {
                    isUpdating = false
                    updateMsg = "Error: ${e.message}"
                }
            }
        }.start()
    }

    LaunchedEffect(callId) {
        kotlinx.coroutines.delay(100)
        loadCall()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< Back",
                color = Color(0xFF007AFF),
                fontSize = 16.sp,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = call?.callNumber ?: "Call #$callId",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Refresh",
                color = Color(0xFF007AFF),
                fontSize = 14.sp,
                modifier = Modifier.clickable { loadCall() }.padding(8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFD1D1D6)))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.Gray, fontSize = 16.sp)
            }
        } else if (error != null && call == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = Color.Red, fontSize = 14.sp)
            }
        } else if (call != null) {
            val c = call!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                DetailSection {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(c.callNumber ?: "#${c.id}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = c.callTypeDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF),
                                    modifier = Modifier
                                        .background(Color(0xFF007AFF).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                if (c.priority != null && c.priority != "normal") {
                                    val priColor = when (c.priority) {
                                        "high" -> Color(0xFFFF9500)
                                        "urgent", "emergency" -> Color(0xFFFF3B30)
                                        else -> Color(0xFF007AFF)
                                    }
                                    Text(
                                        text = c.priority.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = priColor,
                                        modifier = Modifier
                                            .background(priColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        StatusBadge(c.status)
                    }
                    if (!c.reasonForTow.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reason: ${c.reasonForTow}", fontSize = 13.sp, color = Color(0xFF8E8E93))
                    }
                    if (!c.createdAt.isNullOrEmpty()) {
                        Text("Created: ${c.createdAt}", fontSize = 11.sp, color = Color(0xFFAEAEB2))
                    }
                }

                // Status Update Section
                if (c.status !in listOf("completed", "canceled")) {
                    val nextStatuses = mutableListOf<Pair<String, String>>()
                    when (c.status) {
                        "pending" -> nextStatuses.add("dispatched" to "Dispatch")
                        "dispatched" -> nextStatuses.add("en_route" to "En Route")
                        "en_route" -> nextStatuses.add("on_scene" to "On Scene")
                        "on_scene" -> nextStatuses.add("hooked" to "Hooked")
                        "hooked" -> nextStatuses.add("in_transit" to "In Transit")
                        "in_transit" -> nextStatuses.add("delivered" to "Delivered")
                        "delivered" -> nextStatuses.add("completed" to "Complete")
                    }
                    if (nextStatuses.isNotEmpty()) {
                        DetailSection {
                            Text("Update Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            nextStatuses.forEach { (status, label) ->
                                val btnColor = getStatusColor(status)
                                Button(
                                    onClick = { if (!isUpdating) updateStatus(status) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUpdating,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = btnColor)
                                ) {
                                    Text(
                                        if (isUpdating) "Updating..." else "Mark as $label",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (user.isAdmin || user.isDispatcher) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { if (!isUpdating) updateStatus("canceled") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isUpdating,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                                ) {
                                    Text("Cancel Call", color = Color.White)
                                }
                            }
                            if (updateMsg != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    updateMsg!!,
                                    fontSize = 13.sp,
                                    color = if (updateMsg!!.startsWith("Error") || updateMsg!!.startsWith("Update failed")) Color.Red else Color(0xFF34C759)
                                )
                            }
                        }
                    }
                }

                // Vehicle Section
                if (c.vehicleDescription.isNotEmpty() || !c.vehicleLicense.isNullOrEmpty()) {
                    DetailSection {
                        Text("Vehicle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (c.vehicleDescription.isNotEmpty()) DetailRow("Vehicle", c.vehicleDescription)
                        if (!c.vehicleColor.isNullOrEmpty()) DetailRow("Color", c.vehicleColor!!)
                        if (!c.vehicleLicense.isNullOrEmpty()) {
                            DetailRow("Plate", "${c.vehicleLicense} ${c.plateState ?: ""}".trim())
                        }
                        if (!c.vehicleVin.isNullOrEmpty()) DetailRow("VIN", c.vehicleVin!!)
                    }
                }

                // People Section
                val hasPeople = !c.callerName.isNullOrEmpty() || !c.customerName.isNullOrEmpty() || !c.contactName.isNullOrEmpty()
                if (hasPeople) {
                    DetailSection {
                        Text("People", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!c.callerName.isNullOrEmpty()) {
                            DetailRow("Caller", c.callerName!!)
                            if (!c.callerPhone.isNullOrEmpty()) DetailRow("Phone", c.callerPhone!!)
                        }
                        if (!c.contactName.isNullOrEmpty()) {
                            DetailRow("Contact", c.contactName!!)
                            if (!c.contactPhone.isNullOrEmpty()) DetailRow("Phone", c.contactPhone!!)
                        }
                        if (!c.customerName.isNullOrEmpty()) DetailRow("Account", c.customerName!!)
                    }
                }

                // Locations Section
                DetailSection {
                    Text("Locations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PICKUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                    Text(c.fullPickupAddress, fontSize = 14.sp)
                    if (c.fullDropoffAddress.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DROP-OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                        Text(c.fullDropoffAddress, fontSize = 14.sp)
                    }
                }

                // Assignment Section
                val hasAssignment = !c.driverName.isNullOrEmpty() || !c.truckNumber.isNullOrEmpty()
                if (hasAssignment) {
                    DetailSection {
                        Text("Assignment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!c.driverName.isNullOrEmpty()) DetailRow("Driver", c.driverName!!)
                        if (!c.truckNumber.isNullOrEmpty()) DetailRow("Truck", c.truckNumber!!)
                        if (!c.dispatcherName.isNullOrEmpty()) DetailRow("Dispatcher", c.dispatcherName!!)
                    }
                }

                // Billing Section
                val hasAmounts = c.totalAmount != null || c.baseRate != null
                if (hasAmounts) {
                    DetailSection {
                        Text("Billing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (c.baseRate != null) DetailRow("Base Rate", "$${formatAmount(c.baseRate)}")
                        if (c.mileageRate != null) DetailRow("Mileage Rate", "$${formatAmount(c.mileageRate)}")
                        if (c.mileage != null) DetailRow("Mileage", formatAmount(c.mileage))
                        if (c.additionalCharges != null) DetailRow("Additional", "$${formatAmount(c.additionalCharges)}")
                        if (c.totalAmount != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$${formatAmount(c.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF34C759))
                            }
                        }
                        if (!c.paymentStatus.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val psColor = when (c.paymentStatus) {
                                "paid" -> Color(0xFF34C759)
                                "partial" -> Color(0xFFFF9500)
                                else -> Color(0xFF8E8E93)
                            }
                            Text(
                                text = c.paymentStatus.replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = psColor,
                                modifier = Modifier
                                    .background(psColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Notes Section
                val hasNotes = !c.dispatchNotes.isNullOrEmpty() || !c.driverNotes.isNullOrEmpty()
                if (hasNotes) {
                    DetailSection {
                        Text("Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!c.dispatchNotes.isNullOrEmpty()) {
                            Text("Dispatch Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                            Text(c.dispatchNotes!!, fontSize = 14.sp)
                        }
                        if (!c.driverNotes.isNullOrEmpty()) {
                            if (!c.dispatchNotes.isNullOrEmpty()) Spacer(modifier = Modifier.height(6.dp))
                            Text("Driver Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                            Text(c.driverNotes!!, fontSize = 14.sp)
                        }
                    }
                }

                // PO Number
                if (!c.poNumber.isNullOrEmpty()) {
                    DetailSection {
                        DetailRow("PO Number", c.poNumber!!)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

private fun formatAmount(value: Any?): String {
    return when (value) {
        is Number -> String.format("%.2f", value.toDouble())
        is String -> {
            val d = value.toDoubleOrNull()
            if (d != null) String.format("%.2f", d) else value
        }
        else -> "0.00"
    }
}

@Composable
fun DetailSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF8E8E93), modifier = Modifier.width(90.dp))
        Text(value, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SimpleMoreScreen(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "More", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = user.fullName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text = user.email, fontSize = 14.sp, color = Color(0xFF8E8E93))
                Text(
                    text = user.role.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    color = Color(0xFFFF9500),
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Logout", color = Color.White)
        }
    }
}
