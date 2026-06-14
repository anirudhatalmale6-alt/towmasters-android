package com.towmasterscorp.app.ui.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(user: User) {
    val viewModel: DashboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Dashboard", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Welcome, ${user.firstName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Stats Row
                Text(
                    text = "Today's Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Calls",
                        value = "${uiState.stats?.todayCalls ?: 0}",
                        icon = Icons.Default.Phone,
                        color = Primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Active",
                        value = "${uiState.stats?.activeCalls ?: 0}",
                        icon = Icons.Default.DirectionsCar,
                        color = Secondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Completed",
                        value = "${uiState.stats?.completedToday ?: 0}",
                        icon = Icons.Default.CheckCircle,
                        color = Success
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Cancelled",
                        value = "${uiState.stats?.cancelledToday ?: 0}",
                        icon = Icons.Default.Cancel,
                        color = Error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Revenue
                Text(
                    text = "Revenue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        RevenueRow("Today", uiState.stats?.todayRevenue ?: 0.0)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RevenueRow("This Week", uiState.stats?.weekRevenue ?: 0.0)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        RevenueRow("This Month", uiState.stats?.monthRevenue ?: 0.0)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drivers
                Text(
                    text = "Drivers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Online",
                        value = "${uiState.stats?.driversOnline ?: 0}",
                        icon = Icons.Default.PersonPin,
                        color = Success
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total",
                        value = "${uiState.stats?.driversTotal ?: 0}",
                        icon = Icons.Default.Group,
                        color = Info
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Response",
                        value = "${uiState.stats?.avgResponseTime ?: 0} min",
                        icon = Icons.Default.Timer,
                        color = Warning
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Invoices",
                        value = "${uiState.stats?.pendingInvoices ?: 0}",
                        icon = Icons.Default.Receipt,
                        color = StatusDispatched
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Error
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(uiState.error!!)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
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
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RevenueRow(label: String, amount: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatter.format(amount),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Success
        )
    }
}
