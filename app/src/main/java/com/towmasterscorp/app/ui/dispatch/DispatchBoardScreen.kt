@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.towmasterscorp.app.ui.dispatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.towmasterscorp.app.data.models.Call
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.theme.*


@Composable
fun DispatchBoardScreen(
    user: User,
    onCallClick: (Int) -> Unit
) {
    val viewModel: DispatchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActiveCalls()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dispatch Board", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { viewModel.loadActiveCalls() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.calls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No active calls",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val groupedCalls = uiState.calls.groupBy { it.status }
            val statusOrder = listOf(
                "new", "dispatched", "en_route", "on_scene",
                "hooked", "in_transit", "delivered"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                statusOrder.forEach { status ->
                    val calls = groupedCalls[status] ?: emptyList()
                    if (calls.isNotEmpty()) {
                        item {
                            StatusHeader(
                                status = status,
                                count = calls.size,
                                color = getStatusColor(status)
                            )
                        }
                        items(calls) { call ->
                            DispatchCallCard(
                                call = call,
                                onClick = { onCallClick(call.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }

        if (uiState.error != null) {
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(uiState.error!!)
            }
        }
    }
}

@Composable
fun StatusHeader(status: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = getStatusDisplayName(status),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun DispatchCallCard(call: Call, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getStatusColor(call.status))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = call.callNumber ?: "#${call.id}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (call.priority == "high" || call.priority == "emergency") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = call.priority!!.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Error
                            )
                        }
                    }
                }
                Text(
                    text = call.vehicleDescription,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = call.pickupAddress ?: "No pickup address",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (call.driverName != null) {
                    Text(
                        text = "Driver: ${call.driverName}",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "new" -> StatusNew
        "dispatched" -> StatusDispatched
        "en_route" -> StatusEnRoute
        "on_scene" -> StatusOnScene
        "hooked" -> StatusHooked
        "in_transit" -> StatusInTransit
        "delivered" -> StatusDelivered
        "completed" -> StatusCompleted
        "cancelled" -> StatusCancelled
        else -> StatusNew
    }
}

fun getStatusDisplayName(status: String): String {
    return when (status) {
        "new" -> "New"
        "dispatched" -> "Dispatched"
        "en_route" -> "En Route"
        "on_scene" -> "On Scene"
        "hooked" -> "Hooked"
        "in_transit" -> "In Transit"
        "delivered" -> "Delivered"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}
