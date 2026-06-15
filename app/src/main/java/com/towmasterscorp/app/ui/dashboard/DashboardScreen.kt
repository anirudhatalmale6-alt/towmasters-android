package com.towmasterscorp.app.ui.dashboard

import android.util.Log
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
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(user: User) {
    var todayCalls by remember { mutableIntStateOf(0) }
    var activeCalls by remember { mutableIntStateOf(0) }
    var completedToday by remember { mutableIntStateOf(0) }
    var todayRevenue by remember { mutableStateOf(0.0) }
    var driversOnline by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.getApi().getDashboardStats()
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                todayCalls = body.today?.getTotalCalls() ?: 0
                activeCalls = body.today?.getActive() ?: 0
                completedToday = body.today?.getCompleted() ?: 0
                todayRevenue = body.today?.getTotalRevenue() ?: 0.0
                driversOnline = body.driversActive
            }
        } catch (e: Exception) {
            Log.e("Dashboard", "Failed to load stats", e)
            error = e.message
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Today's Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(Modifier.weight(1f), "Total Calls", "$todayCalls", Icons.Default.Phone, Primary)
                    StatCard(Modifier.weight(1f), "Active", "$activeCalls", Icons.Default.DirectionsCar, Secondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(Modifier.weight(1f), "Completed", "$completedToday", Icons.Default.CheckCircle, Success)
                    StatCard(Modifier.weight(1f), "Drivers Online", "$driversOnline", Icons.Default.Person, Info)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Revenue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                        Text(
                            text = "Today: ${formatter.format(todayRevenue)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                    }
                }

                if (error != null) {
                    Text(text = "Note: $error", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
