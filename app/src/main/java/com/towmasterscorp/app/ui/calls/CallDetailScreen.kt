@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.towmasterscorp.app.ui.calls

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.Call
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.dispatch.getStatusColor
import com.towmasterscorp.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale


@Composable
fun CallDetailScreen(
    callId: Int,
    user: User,
    onBack: () -> Unit
) {
    var call by remember { mutableStateOf<Call?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadCall() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.getApi().getCall(callId)
                if (response.isSuccessful && response.body()?.success == true) {
                    call = response.body()?.call
                } else {
                    error = response.body()?.error ?: "Failed to load call"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Network error"
            }
            isLoading = false
        }
    }

    fun updateStatus(newStatus: String) {
        scope.launch {
            isUpdating = true
            try {
                val response = ApiClient.getApi().updateCall(
                    id = callId,
                    request = mapOf("status" to newStatus)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    call = call?.copy(status = newStatus)
                }
            } catch (e: Exception) {
                error = e.localizedMessage
            }
            isUpdating = false
        }
    }

    LaunchedEffect(callId) {
        loadCall()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(call?.callNumber ?: "Call #$callId", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadCall() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.Gray)
            }
        } else if (call == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(error ?: "Call not found")
            }
        } else {
            val currentCall = call!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getStatusColor(currentCall.status).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = currentCall.statusDisplayName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = getStatusColor(currentCall.status)
                        )
                    }

                    if (currentCall.priority != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentCall.priority == "high" || currentCall.priority == "emergency")
                                Error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = currentCall.priority.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentCall.priority == "high" || currentCall.priority == "emergency")
                                    Error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Vehicle Info
                DetailSection(title = "Vehicle") {
                    DetailRow("Vehicle", currentCall.vehicleDescription)
                    if (currentCall.vehicleColor != null) DetailRow("Color", currentCall.vehicleColor)
                    if (currentCall.vehiclePlate != null) DetailRow("Plate", "${currentCall.vehiclePlate} ${currentCall.plateState ?: ""}")
                    if (currentCall.vehicleVin != null) DetailRow("VIN", currentCall.vehicleVin)
                }

                // Type & Reason
                DetailSection(title = "Service") {
                    if (currentCall.callType != null) DetailRow("Type", currentCall.callType.replace("_", " ").replaceFirstChar { it.uppercase() })
                    if (currentCall.reasonForTow != null) DetailRow("Reason", currentCall.reasonForTow)
                    if (currentCall.customerName != null) DetailRow("Account", currentCall.customerName)
                    if (currentCall.poNumber != null) DetailRow("PO #", currentCall.poNumber)
                }

                // Pickup & Dropoff
                DetailSection(title = "Locations") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pickup", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentCall.fullPickupAddress, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Drop-off", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentCall.fullDropoffAddress, fontSize = 14.sp)
                        }
                    }
                    if (currentCall.mileage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("Distance", "${currentCall.mileage} miles")
                    }
                }

                // Customer / Caller
                DetailSection(title = "Contact") {
                    if (currentCall.customerName != null) DetailRow("Customer", currentCall.customerName)
                    if (currentCall.callerName != null) DetailRow("Caller", currentCall.callerName)
                    if (currentCall.callerPhone != null) DetailRow("Phone", currentCall.callerPhone)
                    if (currentCall.customerPhone != null) DetailRow("Customer Phone", currentCall.customerPhone)
                }

                // Driver & Truck
                DetailSection(title = "Assignment") {
                    DetailRow("Driver", currentCall.driverName ?: "Unassigned")
                    DetailRow("Truck", currentCall.truckNumber ?: "Unassigned")
                    if (currentCall.mileage != null) DetailRow("ETA", "${currentCall.mileage} min")
                }

                // Billing
                if (currentCall.totalAmount != null) {
                    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                    DetailSection(title = "Billing") {
                        DetailRow("Amount", formatter.format(currentCall.totalAmount))
                        if (currentCall.paymentMethod != null) DetailRow("Payment", currentCall.paymentMethod)
                        if (currentCall.paymentStatus != null) DetailRow("Status", currentCall.paymentStatus)
                        if (currentCall.poNumber != null) DetailRow("Invoice #", currentCall.poNumber)
                    }
                }

                // Notes
                if (!currentCall.dispatchNotes.isNullOrBlank()) {
                    DetailSection(title = "Notes") {
                        Text(currentCall.dispatchNotes, fontSize = 14.sp)
                    }
                }

                if (!currentCall.driverNotes.isNullOrBlank() && user.isDispatcher) {
                    DetailSection(title = "Internal Notes") {
                        Text(currentCall.driverNotes, fontSize = 14.sp)
                    }
                }

                // Status Update Button (for drivers)
                if (currentCall.isActive && currentCall.nextDriverStatus != null) {
                    val canUpdate = user.isDriver && currentCall.assignedDriverId == user.id || user.isDispatcher
                    if (canUpdate) {
                        Button(
                            onClick = { updateStatus(currentCall.nextDriverStatus!!) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isUpdating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getStatusColor(currentCall.nextDriverStatus!!)
                            )
                        ) {
                            if (isUpdating) {
                                Text("...", color = OnPrimary)
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Update to ${currentCall.nextStatusDisplayName}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Timestamps
                DetailSection(title = "Timeline") {
                    if (currentCall.createdAt != null) DetailRow("Created", currentCall.createdAt)
                    if (currentCall.dispatchedAt != null) DetailRow("Dispatched", currentCall.dispatchedAt)
                    if (currentCall.enRouteAt != null) DetailRow("En Route", currentCall.enRouteAt)
                    if (currentCall.onSceneAt != null) DetailRow("On Scene", currentCall.onSceneAt)
                    if (currentCall.hookedAt != null) DetailRow("Hooked", currentCall.hookedAt)
                    if (currentCall.deliveredAt != null) DetailRow("Drop-off", currentCall.deliveredAt)
                    if (currentCall.completedAt != null) DetailRow("Completed", currentCall.completedAt)
                    if (currentCall.completedAt != null) DetailRow("Cancelled", currentCall.completedAt)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
