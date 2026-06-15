package com.towmasterscorp.app.ui.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.MainActivity
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.Call
import com.towmasterscorp.app.data.models.CallAddress
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
        val pending = MainActivity.consumePendingCallId()
        if (pending != null) selectedCallId = pending
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val pending = MainActivity.consumePendingCallId()
            if (pending != null) selectedCallId = pending
        }
    }

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

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000)
            loadCalls()
        }
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
    val statusColor = getStatusColor(call.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
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

            val vehDesc = call.vehicleDescription
            Text(
                text = if (vehDesc.isNotEmpty()) vehDesc else "No vehicle specified",
                fontSize = 14.sp,
                color = if (vehDesc.isNotEmpty()) Color(0xFF333333) else Color(0xFFAEAEB2)
            )

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

data class ChargeItem(val description: String, val amount: Double, val type: String)

@Composable
fun CallDetailContent(callId: Int, user: User, onBack: () -> Unit) {
    var call by remember { mutableStateOf<Call?>(null) }
    var charges by remember { mutableStateOf<List<ChargeItem>>(emptyList()) }
    var taxRate by remember { mutableStateOf(0.0) }
    var taxAmount by remember { mutableStateOf(0.0) }
    var amountPaid by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
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

                    val addressesList = mutableListOf<CallAddress>()
                    val addrsArr = c.optJSONArray("addresses")
                    if (addrsArr != null) {
                        for (j in 0 until addrsArr.length()) {
                            val a = addrsArr.getJSONObject(j)
                            addressesList.add(CallAddress(
                                id = a.optInt("id"),
                                label = a.optString("label", null),
                                address = a.optString("address", null),
                                city = a.optString("city", null),
                                state = a.optString("state", null),
                                zip = a.optString("zip", null)
                            ))
                        }
                    }

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
                        createdAt = c.optString("created_at", null),
                        addresses = addressesList
                    )
                    val chargesList = mutableListOf<ChargeItem>()
                    val chargesArr = c.optJSONArray("charges")
                    if (chargesArr != null) {
                        for (j in 0 until chargesArr.length()) {
                            val ch = chargesArr.getJSONObject(j)
                            val desc = ch.optString("description", null)
                            val chargeType = ch.optString("charge_type", "Charge")
                            val displayName = if (!desc.isNullOrEmpty() && desc != "null") desc else chargeType
                            val hours = ch.optDouble("hours", 0.0)
                            val rate = ch.optDouble("rate", 0.0)
                            val total = ch.optDouble("total", 0.0)
                            val displayDetail = if (hours > 0) "$displayName (${String.format("%.1f", hours)}h x $${String.format("%.2f", rate)})" else displayName
                            chargesList.add(ChargeItem(
                                description = displayDetail,
                                amount = total,
                                type = chargeType
                            ))
                        }
                    }
                    val tr = c.optDouble("tax_rate", 0.0)
                    val ta = c.optDouble("tax_amount", 0.0)
                    val ap = c.optDouble("amount_paid", 0.0)

                    handler.post {
                        call = parsed
                        charges = chargesList
                        taxRate = tr
                        taxAmount = ta
                        amountPaid = ap
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
                val url = java.net.URL("https://app.towmasterscorp.com/api/calls.php?action=update-status&id=$callId")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("""{"status":"$newStatus"}""".toByteArray())
                val code = conn.responseCode
                val respText = try {
                    if (code in 200..299) conn.inputStream.bufferedReader().readText()
                    else conn.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) { "" }
                conn.disconnect()
                handler.post {
                    isUpdating = false
                    if (code in 200..299) {
                        updateMsg = "Status updated!"
                        loadCall()
                    } else {
                        val errJson = try { org.json.JSONObject(respText).optString("error", "") } catch (_: Exception) { "" }
                        updateMsg = if (errJson.isNotEmpty()) errJson else "Update failed (HTTP $code)"
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Edit",
                    color = Color(0xFF007AFF),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { isEditing = !isEditing }.padding(8.dp)
                )
                Text(
                    text = "Refresh",
                    color = Color(0xFF007AFF),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { loadCall() }.padding(8.dp)
                )
            }
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
                    if (!c.reasonForTow.isNullOrEmpty() && c.reasonForTow != "null") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reason: ${c.reasonForTow}", fontSize = 13.sp, color = Color(0xFF8E8E93))
                    }
                    if (!c.createdAt.isNullOrEmpty()) {
                        Text("Created: ${c.createdAt}", fontSize = 11.sp, color = Color(0xFFAEAEB2))
                    }
                }

                // Edit Call Section
                if (isEditing) {
                    EditCallSection(call = c, callId = callId, onSaved = {
                        isEditing = false
                        loadCall()
                    })
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
                val hasVehicle = c.vehicleDescription.isNotEmpty() || c.vehicleLicenseSafe != null || c.vehicleVinSafe != null || c.vehicleColorSafe != null
                DetailSection {
                    Text("Vehicle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (c.vehicleDescription.isNotEmpty()) {
                        DetailRow("Vehicle", c.vehicleDescription)
                    } else {
                        DetailRow("Vehicle", "No vehicle specified")
                    }
                    if (c.vehicleColorSafe != null) DetailRow("Color", c.vehicleColorSafe!!)
                    if (c.vehicleLicenseSafe != null) {
                        DetailRow("Plate", "${c.vehicleLicenseSafe} ${c.plateStateSafe ?: ""}".trim())
                    }
                    if (c.vehicleVinSafe != null) DetailRow("VIN", c.vehicleVinSafe!!)
                }

                // People Section
                val hasPeople = !c.callerName.isNullOrEmpty() || !c.customerName.isNullOrEmpty() || !c.contactName.isNullOrEmpty()
                if (hasPeople) {
                    DetailSection {
                        Text("People", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!c.callerName.isNullOrEmpty() && c.callerName != "null") {
                            DetailRow("Caller", c.callerName!!)
                            if (!c.callerPhone.isNullOrEmpty() && c.callerPhone != "null") DetailRow("Phone", c.callerPhone!!)
                        }
                        if (!c.contactName.isNullOrEmpty() && c.contactName != "null") {
                            DetailRow("Contact", c.contactName!!)
                            if (!c.contactPhone.isNullOrEmpty() && c.contactPhone != "null") DetailRow("Phone", c.contactPhone!!)
                        }
                        if (!c.customerName.isNullOrEmpty() && c.customerName != "null") DetailRow("Account", c.customerName!!)
                    }
                }

                // Locations Section with additional addresses
                DetailSection {
                    Text("Locations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PICKUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                    Text(c.fullPickupAddress, fontSize = 14.sp)

                    if (c.addresses.isNotEmpty()) {
                        c.addresses.forEach { addr ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                (addr.label ?: "Stop").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9500)
                            )
                            if (!addr.address.isNullOrEmpty()) {
                                Text(addr.address, fontSize = 14.sp)
                            }
                            val csz = addr.cityStateZip
                            if (csz != null) {
                                Text(csz, fontSize = 12.sp, color = Color(0xFF8E8E93))
                            }
                        }
                    }

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
                val hasAmounts = c.totalAmount != null || c.baseRate != null || charges.isNotEmpty()
                if (hasAmounts) {
                    DetailSection {
                        Text("Billing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (c.baseRate != null) DetailRow("Base Rate", "$${formatAmount(c.baseRate)}")
                        if (c.mileageRate != null) DetailRow("Mileage Rate", "$${formatAmount(c.mileageRate)}/mi")
                        if (c.mileage != null) DetailRow("Mileage", "${formatAmount(c.mileage)} mi")
                        if (c.additionalCharges != null) DetailRow("Additional", "$${formatAmount(c.additionalCharges)}")
                        if (charges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Itemized Charges", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                            charges.forEach { ch ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ch.description, fontSize = 13.sp, color = Color(0xFF333333), modifier = Modifier.weight(1f))
                                    Text("$${String.format("%.2f", ch.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (taxRate > 0 || taxAmount > 0) {
                            DetailRow("Tax (${String.format("%.1f", taxRate)}%)", "$${String.format("%.2f", taxAmount)}")
                        }
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
                        if (amountPaid > 0) {
                            DetailRow("Amount Paid", "$${String.format("%.2f", amountPaid)}")
                        }
                        if (!c.paymentStatus.isNullOrEmpty() && c.paymentStatus != "null") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                if (!c.paymentMethod.isNullOrEmpty() && c.paymentMethod != "null") {
                                    Text(c.paymentMethod!!, fontSize = 12.sp, color = Color(0xFF8E8E93))
                                }
                            }
                        }
                    }
                }

                // Notes Section
                val hasNotes = c.dispatchNotesSafe != null || c.driverNotesSafe != null
                if (hasNotes) {
                    DetailSection {
                        Text("Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (c.dispatchNotesSafe != null) {
                            Text("Dispatch Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                            Text(c.dispatchNotesSafe!!, fontSize = 14.sp)
                        }
                        if (c.driverNotesSafe != null) {
                            if (c.dispatchNotesSafe != null) Spacer(modifier = Modifier.height(6.dp))
                            Text("Driver Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                            Text(c.driverNotesSafe!!, fontSize = 14.sp)
                        }
                    }
                }

                // PO Number
                if (!c.poNumber.isNullOrEmpty() && c.poNumber != "null") {
                    DetailSection {
                        DetailRow("PO Number", c.poNumber!!)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun EditCallSection(call: Call, callId: Int, onSaved: () -> Unit) {
    var vehicleYear by remember { mutableStateOf(call.vehicleYear?.toString()?.takeIf { it != "0" && it != "null" } ?: "") }
    var vehicleMake by remember { mutableStateOf(call.vehicleMake?.takeIf { it != "null" } ?: "") }
    var vehicleModel by remember { mutableStateOf(call.vehicleModel?.takeIf { it != "null" } ?: "") }
    var vehicleColor by remember { mutableStateOf(call.vehicleColor?.takeIf { it != "null" } ?: "") }
    var vehicleLicense by remember { mutableStateOf(call.vehicleLicense?.takeIf { it != "null" } ?: "") }
    var plateState by remember { mutableStateOf(call.plateState?.takeIf { it != "null" } ?: "") }
    var vehicleVin by remember { mutableStateOf(call.vehicleVin?.takeIf { it != "null" } ?: "") }
    var pickupAddress by remember { mutableStateOf(call.pickupAddress ?: "") }
    var pickupCity by remember { mutableStateOf(call.pickupCity ?: "") }
    var pickupState by remember { mutableStateOf(call.pickupState ?: "") }
    var pickupZip by remember { mutableStateOf(call.pickupZip ?: "") }
    var dropoffAddress by remember { mutableStateOf(call.dropoffAddress ?: "") }
    var dropoffCity by remember { mutableStateOf(call.dropoffCity ?: "") }
    var dropoffState by remember { mutableStateOf(call.dropoffState ?: "") }
    var dropoffZip by remember { mutableStateOf(call.dropoffZip ?: "") }
    var callerName by remember { mutableStateOf(call.callerName?.takeIf { it != "null" } ?: "") }
    var callerPhone by remember { mutableStateOf(call.callerPhone?.takeIf { it != "null" } ?: "") }
    var dispatchNotes by remember { mutableStateOf(call.dispatchNotes?.takeIf { it != "null" } ?: "") }
    var driverNotes by remember { mutableStateOf(call.driverNotes?.takeIf { it != "null" } ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    var isDecodingVin by remember { mutableStateOf(false) }
    var vinMessage by remember { mutableStateOf<String?>(null) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun decodeVin(vin: String) {
        if (vin.length != 17) return
        isDecodingVin = true
        vinMessage = null
        Thread {
            try {
                val url = java.net.URL("https://vpic.nhtsa.dot.gov/api/vehicles/decodevin/$vin?format=json")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val responseText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = org.json.JSONObject(responseText)
                val results = json.optJSONArray("Results")
                var year = ""
                var make = ""
                var model = ""
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val r = results.getJSONObject(i)
                        val variable = r.optString("Variable", "")
                        val value = r.optString("Value", "")
                        if (value.isNotEmpty() && value != "null") {
                            when (variable) {
                                "Model Year" -> year = value
                                "Make" -> make = value
                                "Model" -> model = value
                            }
                        }
                    }
                }
                handler.post {
                    isDecodingVin = false
                    if (year.isNotEmpty() || make.isNotEmpty() || model.isNotEmpty()) {
                        if (year.isNotEmpty()) vehicleYear = year
                        if (make.isNotEmpty()) vehicleMake = make
                        if (model.isNotEmpty()) vehicleModel = model
                        val found = listOf(year, make, model).filter { it.isNotEmpty() }.joinToString(" ")
                        vinMessage = "Found: $found"
                    } else {
                        vinMessage = "No vehicle data found for this VIN"
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    isDecodingVin = false
                    vinMessage = "VIN decode failed"
                }
            }
        }.start()
    }

    DetailSection {
        Text("Edit Call Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF007AFF))
        Spacer(modifier = Modifier.height(8.dp))

        Text("Vehicle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
        EditField("Year", vehicleYear) { vehicleYear = it }
        EditField("Make", vehicleMake) { vehicleMake = it }
        EditField("Model", vehicleModel) { vehicleModel = it }
        EditField("Color", vehicleColor) { vehicleColor = it }
        EditField("License Plate", vehicleLicense) { vehicleLicense = it }
        EditField("Plate State", plateState) { plateState = it }

        OutlinedTextField(
            value = vehicleVin,
            onValueChange = { newVal ->
                vehicleVin = newVal
                val cleaned = newVal.trim().uppercase()
                if (cleaned.length == 17) decodeVin(cleaned)
            },
            label = { Text("VIN", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFD1D1D6),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            ),
            trailingIcon = {
                if (isDecodingVin) {
                    Text("...", fontSize = 14.sp, color = Color(0xFF8E8E93))
                } else if (vehicleVin.trim().length == 17) {
                    Text(
                        "Decode",
                        fontSize = 13.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { decodeVin(vehicleVin.trim().uppercase()) }
                            .padding(end = 8.dp)
                    )
                }
            }
        )
        if (vinMessage != null) {
            Text(
                vinMessage!!,
                fontSize = 12.sp,
                color = if (vinMessage!!.startsWith("Found")) Color(0xFF34C759) else Color(0xFFFF9500),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Pickup", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
        EditField("Address", pickupAddress) { pickupAddress = it }
        EditField("City", pickupCity) { pickupCity = it }
        EditField("State", pickupState) { pickupState = it }
        EditField("Zip", pickupZip) { pickupZip = it }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Drop-off", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
        EditField("Address", dropoffAddress) { dropoffAddress = it }
        EditField("City", dropoffCity) { dropoffCity = it }
        EditField("State", dropoffState) { dropoffState = it }
        EditField("Zip", dropoffZip) { dropoffZip = it }

        Spacer(modifier = Modifier.height(8.dp))
        Text("People", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
        EditField("Caller Name", callerName) { callerName = it }
        EditField("Caller Phone", callerPhone) { callerPhone = it }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Notes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
        EditField("Dispatch Notes", dispatchNotes) { dispatchNotes = it }
        EditField("Driver Notes", driverNotes) { driverNotes = it }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (isSaving) return@Button
                isSaving = true
                saveMsg = null
                Thread {
                    try {
                        val url = java.net.URL("https://app.towmasterscorp.com/api/calls.php?action=update&id=$callId")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "PUT"
                        conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        val body = org.json.JSONObject()
                        body.put("vehicle_year", vehicleYear)
                        body.put("vehicle_make", vehicleMake)
                        body.put("vehicle_model", vehicleModel)
                        body.put("vehicle_color", vehicleColor)
                        body.put("vehicle_license", vehicleLicense)
                        body.put("plate_state", plateState)
                        body.put("vehicle_vin", vehicleVin)
                        body.put("pickup_address", pickupAddress)
                        body.put("pickup_city", pickupCity)
                        body.put("pickup_state", pickupState)
                        body.put("pickup_zip", pickupZip)
                        body.put("dropoff_address", dropoffAddress)
                        body.put("dropoff_city", dropoffCity)
                        body.put("dropoff_state", dropoffState)
                        body.put("dropoff_zip", dropoffZip)
                        body.put("caller_name", callerName)
                        body.put("caller_phone", callerPhone)
                        body.put("dispatch_notes", dispatchNotes)
                        body.put("driver_notes", driverNotes)
                        conn.outputStream.write(body.toString().toByteArray())
                        val code = conn.responseCode
                        conn.disconnect()
                        handler.post {
                            isSaving = false
                            if (code in 200..299) {
                                saveMsg = "Saved!"
                                onSaved()
                            } else {
                                saveMsg = "Save failed (HTTP $code)"
                            }
                        }
                    } catch (e: Exception) {
                        handler.post {
                            isSaving = false
                            saveMsg = "Error: ${e.message}"
                        }
                    }
                }.start()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(if (isSaving) "Saving..." else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (saveMsg != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(saveMsg!!, fontSize = 13.sp, color = if (saveMsg!!.startsWith("Saved")) Color(0xFF34C759) else Color.Red)
        }
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFD1D1D6),
            unfocusedContainerColor = Color(0xFFF9F9F9)
        )
    )
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
    var selectedScreen by remember { mutableStateOf<String?>(null) }

    when (selectedScreen) {
        "chat" -> ChatScreen(user = user) { selectedScreen = null }
        "inspections" -> InspectionsScreen(user = user) { selectedScreen = null }
        "fuel" -> FuelReceiptsScreen(user = user) { selectedScreen = null }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "More", fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text(text = user.fullName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = user.role.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = Color(0xFF8E8E93))
                            Text(text = "Tow Masters Towing", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        }
                    }
                }

                Text("Operations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        MoreMenuItem(icon = Icons.Default.ChatBubble, label = "Chat") { selectedScreen = "chat" }
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 52.dp).height(0.5.dp).background(Color(0xFFD1D1D6)))
                        MoreMenuItem(icon = Icons.Default.Checklist, label = "Inspections") { selectedScreen = "inspections" }
                        Box(modifier = Modifier.fillMaxWidth().padding(start = 52.dp).height(0.5.dp).background(Color(0xFFD1D1D6)))
                        MoreMenuItem(icon = Icons.Default.LocalGasStation, label = "Fuel Receipts") { selectedScreen = "fuel" }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onLogout() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
                        Text("Log Out", color = Color(0xFFFF3B30), fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MoreMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailingContent: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("< Back", color = Color(0xFF007AFF), fontSize = 16.sp, modifier = Modifier.clickable { onBack() }.padding(8.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        trailingContent()
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFD1D1D6)))
}

fun apiGet(endpoint: String): String? {
    return try {
        val url = java.net.URL("https://app.towmasterscorp.com/api/$endpoint")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        text
    } catch (_: Exception) { null }
}

fun apiPost(endpoint: String, body: org.json.JSONObject): Pair<Int, String> {
    return try {
        val url = java.net.URL("https://app.towmasterscorp.com/api/$endpoint")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${ApiClient.token ?: ""}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.write(body.toString().toByteArray())
        val code = conn.responseCode
        val text = try { if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
        conn.disconnect()
        code to text
    } catch (e: Exception) { 0 to (e.message ?: "Network error") }
}

@Composable
fun ChatScreen(user: User, onBack: () -> Unit) {
    var conversations by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var selectedUserName by remember { mutableStateOf("") }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadConversations() {
        Thread {
            val text = apiGet("chat.php?action=conversations")
            if (text != null) {
                val json = org.json.JSONObject(text)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("conversations") ?: org.json.JSONArray()
                    val list = mutableListOf<org.json.JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    handler.post { conversations = list; isLoading = false }
                } else handler.post { isLoading = false }
            } else handler.post { isLoading = false }
        }.start()
    }

    LaunchedEffect(Unit) { loadConversations() }

    if (selectedUserId != null) {
        ChatDetailScreen(userId = selectedUserId!!, userName = selectedUserName, currentUserId = user.id) { selectedUserId = null }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
            ScreenHeader("Chat", onBack)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
            } else if (conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No conversations yet", color = Color.Gray, fontSize = 16.sp) }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    conversations.forEach { conv ->
                        val name = "${conv.optString("first_name", "")} ${conv.optString("last_name", "")}".trim()
                        val uid = conv.optInt("user_id")
                        val unread = conv.optInt("unread_count", 0)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedUserId = uid; selectedUserName = name },
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                                    Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(conv.optString("last_message", "").take(50), fontSize = 13.sp, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (unread > 0) {
                                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF007AFF)), contentAlignment = Alignment.Center) {
                                        Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(userId: Int, userName: String, currentUserId: Int, onBack: () -> Unit) {
    var messages by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var newMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadMessages() {
        Thread {
            val text = apiGet("chat.php?action=messages&user_id=$userId")
            if (text != null) {
                val json = org.json.JSONObject(text)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("messages") ?: org.json.JSONArray()
                    val list = mutableListOf<org.json.JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    handler.post { messages = list; isLoading = false }
                } else handler.post { isLoading = false }
            } else handler.post { isLoading = false }
        }.start()
    }

    fun sendMsg() {
        if (newMessage.isBlank() || isSending) return
        isSending = true
        val msg = newMessage
        newMessage = ""
        Thread {
            val body = org.json.JSONObject()
            body.put("recipient_id", userId)
            body.put("message", msg)
            val (code, _) = apiPost("chat.php?action=send", body)
            handler.post {
                isSending = false
                if (code in 200..299) loadMessages()
                else newMessage = msg
            }
        }.start()
    }

    LaunchedEffect(Unit) { loadMessages() }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(10000); loadMessages() } }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        ScreenHeader(userName, onBack)
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                messages.forEach { msg ->
                    val isMe = msg.optInt("sender_id") == currentUserId
                    val align = if (isMe) Arrangement.End else Arrangement.Start
                    val bgColor = if (isMe) Color(0xFF007AFF) else Color.White
                    val textColor = if (isMe) Color.White else Color.Black
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = align) {
                        Box(modifier = Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp).widthIn(max = 280.dp)) {
                            Text(msg.optString("message", ""), color = textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newMessage, onValueChange = { newMessage = it },
                placeholder = { Text("Message", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true, shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFD1D1D6))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (newMessage.isNotBlank()) Color(0xFF007AFF) else Color(0xFFD1D1D6))
                    .clickable(enabled = newMessage.isNotBlank()) { sendMsg() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun InspectionsScreen(user: User, onBack: () -> Unit) {
    var inspections by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var trucks by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadData() {
        isLoading = true
        Thread {
            val text = apiGet("inspections.php?action=list")
            val truckText = apiGet("trucks.php?action=list")
            val inspList = mutableListOf<org.json.JSONObject>()
            val truckList = mutableListOf<org.json.JSONObject>()
            if (text != null) {
                val json = org.json.JSONObject(text)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("inspections") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) inspList.add(arr.getJSONObject(i))
                }
            }
            if (truckText != null) {
                val json = org.json.JSONObject(truckText)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("trucks") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) truckList.add(arr.getJSONObject(i))
                }
            }
            handler.post { inspections = inspList; trucks = truckList; isLoading = false }
        }.start()
    }

    LaunchedEffect(Unit) { loadData() }

    if (showCreate) {
        CreateInspectionScreen(trucks = trucks, onBack = { showCreate = false }, onCreated = { showCreate = false; loadData() })
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
            ScreenHeader("Inspections", onBack) {
                Text("+ New", color = Color(0xFF007AFF), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showCreate = true }.padding(8.dp))
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
            } else if (inspections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No inspections found", color = Color.Gray, fontSize = 16.sp) }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inspections.forEach { insp ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(insp.optString("truck_number", "Truck"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val status = insp.optString("status", "pending")
                                    val sColor = when { status.contains("pass") -> Color(0xFF34C759); status.contains("fail") -> Color(0xFFFF3B30); else -> Color(0xFFFF9500) }
                                    Text(status.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = sColor,
                                        modifier = Modifier.background(sColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                val driverName = insp.optString("driver_name", "")
                                if (driverName.isNotEmpty()) Text(driverName, fontSize = 13.sp, color = Color(0xFF007AFF))
                                Text(insp.optString("inspection_type", "").replace("_", " ").replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = Color(0xFF8E8E93))
                                Text(insp.optString("created_at", ""), fontSize = 11.sp, color = Color(0xFFAEAEB2))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CreateInspectionScreen(trucks: List<org.json.JSONObject>, onBack: () -> Unit, onCreated: () -> Unit) {
    var selectedTruckId by remember { mutableStateOf("") }
    var inspectionType by remember { mutableStateOf("pre_trip") }
    var odometer by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    val checkItems = listOf("tires", "brakes", "lights", "mirrors", "horn", "wipers", "fluids", "boom_winch", "wheel_lift", "chains_straps", "safety_equipment", "body_damage")
    val checkStates = remember { mutableStateMapOf<String, Int>().apply { checkItems.forEach { put(it, 1) } } }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        ScreenHeader("New Inspection", onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailSection {
                Text("Truck", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (trucks.isEmpty()) {
                    Text("No trucks available", color = Color.Gray, fontSize = 13.sp)
                } else {
                    trucks.forEach { truck ->
                        val tid = truck.optInt("id").toString()
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedTruckId = tid }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (selectedTruckId == tid) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${truck.optString("unit_number", "")} - ${truck.optString("make", "")} ${truck.optString("model", "")}", fontSize = 14.sp)
                        }
                    }
                }
            }
            DetailSection {
                Text("Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pre_trip" to "Pre-Trip", "post_trip" to "Post-Trip").forEach { (value, label) ->
                        val sel = inspectionType == value
                        Text(label, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else Color(0xFF007AFF),
                            modifier = Modifier.background(if (sel) Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).clickable { inspectionType = value }.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                EditField("Odometer", odometer) { odometer = it }
            }
            DetailSection {
                Text("Checklist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                checkItems.forEach { item ->
                    val label = item.replace("_", " ").replaceFirstChar { it.uppercase() }
                    val pass = (checkStates[item] ?: 1) == 1
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Pass", fontSize = 12.sp, color = if (pass) Color.White else Color(0xFF34C759), fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(if (pass) Color(0xFF34C759) else Color(0xFF34C759).copy(alpha = 0.1f), RoundedCornerShape(6.dp)).clickable { checkStates[item] = 1 }.padding(horizontal = 10.dp, vertical = 4.dp))
                            Text("Fail", fontSize = 12.sp, color = if (!pass) Color.White else Color(0xFFFF3B30), fontWeight = FontWeight.Bold,
                                modifier = Modifier.background(if (!pass) Color(0xFFFF3B30) else Color(0xFFFF3B30).copy(alpha = 0.1f), RoundedCornerShape(6.dp)).clickable { checkStates[item] = 0 }.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
            EditField("Notes", notes) { notes = it }
            Button(onClick = {
                if (selectedTruckId.isEmpty() || isSaving) return@Button
                isSaving = true; saveMsg = null
                Thread {
                    val body = org.json.JSONObject()
                    body.put("truck_id", selectedTruckId.toInt())
                    body.put("inspection_type", inspectionType)
                    if (odometer.isNotEmpty()) body.put("odometer", odometer.toIntOrNull() ?: 0)
                    body.put("notes", notes)
                    checkItems.forEach { body.put(it, checkStates[it] ?: 1) }
                    val (code, _) = apiPost("inspections.php?action=create", body)
                    handler.post { isSaving = false; if (code in 200..299) onCreated() else saveMsg = "Failed (HTTP $code)" }
                }.start()
            }, modifier = Modifier.fillMaxWidth(), enabled = selectedTruckId.isNotEmpty() && !isSaving, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text(if (isSaving) "Saving..." else "Submit Inspection", color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (saveMsg != null) Text(saveMsg!!, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FuelReceiptsScreen(user: User, onBack: () -> Unit) {
    var receipts by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var trucks by remember { mutableStateOf<List<org.json.JSONObject>>(emptyList()) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun loadData() {
        isLoading = true
        Thread {
            val text = apiGet("fuel.php?action=list")
            val truckText = apiGet("trucks.php?action=list")
            val rList = mutableListOf<org.json.JSONObject>()
            val tList = mutableListOf<org.json.JSONObject>()
            if (text != null) {
                val json = org.json.JSONObject(text)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("receipts") ?: json.optJSONArray("fuel_receipts") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) rList.add(arr.getJSONObject(i))
                }
            }
            if (truckText != null) {
                val json = org.json.JSONObject(truckText)
                if (json.optBoolean("success")) {
                    val arr = json.optJSONArray("trucks") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) tList.add(arr.getJSONObject(i))
                }
            }
            handler.post { receipts = rList; trucks = tList; isLoading = false }
        }.start()
    }

    LaunchedEffect(Unit) { loadData() }

    if (showCreate) {
        CreateFuelReceiptScreen(trucks = trucks, onBack = { showCreate = false }, onCreated = { showCreate = false; loadData() })
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
            ScreenHeader("Fuel Receipts", onBack) {
                Text("+ New", color = Color(0xFF007AFF), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showCreate = true }.padding(8.dp))
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = Color.Gray) }
            } else if (receipts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No fuel receipts found", color = Color.Gray, fontSize = 16.sp) }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    receipts.forEach { receipt ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(receipt.optString("truck_number", receipt.optString("place_name", "Fuel Receipt")), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val amount = receipt.optDouble("total_cost", 0.0)
                                    if (amount > 0) Text("$${String.format("%.2f", amount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF34C759))
                                }
                                val gallons = receipt.optDouble("gallons", 0.0)
                                if (gallons > 0) Text("${String.format("%.2f", gallons)} gal", fontSize = 13.sp, color = Color(0xFF8E8E93))
                                val place = receipt.optString("place_name", "")
                                if (place.isNotEmpty()) Text(place, fontSize = 13.sp, color = Color(0xFF8E8E93))
                                val driverName = receipt.optString("driver_name", "")
                                if (driverName.isNotEmpty()) Text(driverName, fontSize = 12.sp, color = Color(0xFF007AFF))
                                Text(receipt.optString("receipt_date", receipt.optString("created_at", "")), fontSize = 11.sp, color = Color(0xFFAEAEB2))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CreateFuelReceiptScreen(trucks: List<org.json.JSONObject>, onBack: () -> Unit, onCreated: () -> Unit) {
    var selectedTruckId by remember { mutableStateOf("") }
    var gallons by remember { mutableStateOf("") }
    var pricePerGallon by remember { mutableStateOf("") }
    var totalCost by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F7))) {
        ScreenHeader("New Fuel Receipt", onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailSection {
                Text("Truck", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (trucks.isEmpty()) {
                    Text("No trucks available", color = Color.Gray, fontSize = 13.sp)
                } else {
                    trucks.forEach { truck ->
                        val tid = truck.optInt("id").toString()
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedTruckId = tid }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (selectedTruckId == tid) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(truck.optString("unit_number", ""), fontSize = 14.sp)
                        }
                    }
                }
            }
            DetailSection {
                Text("Fuel Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                EditField("Gallons", gallons) { gallons = it }
                EditField("Price per Gallon", pricePerGallon) { pricePerGallon = it }
                EditField("Total Cost", totalCost) { totalCost = it }
                EditField("Station Name", placeName) { placeName = it }
                EditField("Truck Mileage", mileage) { mileage = it }
                EditField("Notes", notes) { notes = it }
            }
            Button(onClick = {
                if (gallons.isEmpty() || isSaving) return@Button
                isSaving = true; saveMsg = null
                Thread {
                    val body = org.json.JSONObject()
                    if (selectedTruckId.isNotEmpty()) body.put("truck_id", selectedTruckId.toInt())
                    body.put("gallons", gallons.toDoubleOrNull() ?: 0.0)
                    if (pricePerGallon.isNotEmpty()) body.put("price_per_gallon", pricePerGallon.toDoubleOrNull() ?: 0.0)
                    if (totalCost.isNotEmpty()) body.put("total_cost", totalCost.toDoubleOrNull() ?: 0.0)
                    if (placeName.isNotEmpty()) body.put("place_name", placeName)
                    if (mileage.isNotEmpty()) body.put("truck_mileage", mileage.toIntOrNull() ?: 0)
                    if (notes.isNotEmpty()) body.put("notes", notes)
                    val (code, _) = apiPost("fuel.php?action=create", body)
                    handler.post { isSaving = false; if (code in 200..299) onCreated() else saveMsg = "Failed (HTTP $code)" }
                }.start()
            }, modifier = Modifier.fillMaxWidth(), enabled = gallons.isNotEmpty() && !isSaving, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text(if (isSaving) "Saving..." else "Save Fuel Receipt", color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (saveMsg != null) Text(saveMsg!!, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
