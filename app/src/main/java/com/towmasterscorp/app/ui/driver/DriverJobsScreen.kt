package com.towmasterscorp.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.towmasterscorp.app.ui.dispatch.getStatusDisplayName
import com.towmasterscorp.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverJobsScreen(
    user: User,
    onCallClick: (Int) -> Unit
) {
    var calls by remember { mutableStateOf<List<Call>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadJobs() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val response = ApiClient.getApi().getActiveCalls()
                if (response.isSuccessful && response.body()?.success == true) {
                    calls = (response.body()?.calls ?: emptyList()).filter {
                        it.driverId == user.id
                    }
                } else {
                    error = response.body()?.error ?: "Failed to load jobs"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Network error"
            }
            isLoading = false
        }
    }

    fun updateStatus(callId: Int, newStatus: String) {
        scope.launch {
            try {
                val response = ApiClient.getApi().updateCall(
                    id = callId,
                    request = mapOf("status" to newStatus)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    loadJobs() // Refresh the list
                }
            } catch (e: Exception) {
                error = e.localizedMessage
            }
        }
    }

    LaunchedEffect(Unit) {
        loadJobs()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("My Jobs", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${calls.size} active",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { loadJobs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (calls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WorkOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No active jobs",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Waiting for dispatch",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(calls) { call ->
                    DriverJobCard(
                        call = call,
                        onStatusUpdate = { newStatus -> updateStatus(call.id, newStatus) },
                        onClick = { onCallClick(call.id) }
                    )
                }
            }
        }

        if (error != null) {
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(error!!)
            }
        }
    }
}

@Composable
fun DriverJobCard(
    call: Call,
    onStatusUpdate: (String) -> Unit,
    onClick: () -> Unit
) {
    var isUpdating by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getStatusColor(call.status).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = call.statusDisplayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(call.status)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vehicle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = call.vehicleDescription,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (call.vehicleColor != null) {
                    Text(
                        text = " (${call.vehicleColor})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pickup
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.TripOrigin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Success
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Pickup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(call.fullPickupAddress, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dropoff
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Drop-off", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(call.fullDropoffAddress, fontSize = 13.sp)
                }
            }

            // Notes preview
            if (!call.dispatchNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = call.dispatchNotes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Status Update Button
            if (call.nextDriverStatus != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        isUpdating = true
                        onStatusUpdate(call.nextDriverStatus!!)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = getStatusColor(call.nextDriverStatus!!)
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mark as ${call.nextStatusDisplayName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
