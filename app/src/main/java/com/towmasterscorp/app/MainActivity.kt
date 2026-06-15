package com.towmasterscorp.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.towmasterscorp.app.data.api.ApiClient
import com.towmasterscorp.app.data.models.User
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.services.LocationService
import com.towmasterscorp.app.ui.auth.LoginScreen
import com.towmasterscorp.app.ui.auth.LoginViewModel
import com.towmasterscorp.app.ui.main.MainScreen
import com.towmasterscorp.app.ui.theme.TowMastersTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var authPreferences: AuthPreferences

    companion object {
        var pendingCallId: Int? = null
            private set

        fun consumePendingCallId(): Int? {
            val id = pendingCallId
            pendingCallId = null
            return id
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocation) {
            startLocationService()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authPreferences = (application as TowMastersApp).authPreferences

        handleNotificationIntent(intent)
        requestNotificationPermission()

        setContent {
            TowMastersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(authPreferences = authPreferences)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val callId = intent?.getStringExtra("call_id")?.toIntOrNull()
        if (callId != null) {
            pendingCallId = callId
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestLocationPermission() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        locationPermissionLauncher.launch(permissions.toTypedArray())
    }

    fun startLocationService() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            startForegroundService(intent)
        } else {
            requestLocationPermission()
        }
    }

    fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun AppContent(authPreferences: AuthPreferences) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isAuthenticated by remember { mutableStateOf(false) }

    // Check initial auth state
    LaunchedEffect(Unit) {
        val token = authPreferences.tokenFlow.first()
        if (!token.isNullOrEmpty()) {
            ApiClient.token = token
            val user = authPreferences.userFlow.first()
            if (user != null) {
                currentUser = user
                isAuthenticated = true
            }
        }
    }

    if (isAuthenticated && currentUser != null) {
        MainScreen(
            user = currentUser!!,
            authPreferences = authPreferences,
            onLogout = {
                ApiClient.token = null
                currentUser = null
                isAuthenticated = false
            }
        )
    } else {
        val loginViewModel = remember { LoginViewModel(authPreferences) }
        LoginScreen(
            viewModel = loginViewModel,
            onLoginSuccess = {
                val state = loginViewModel.uiState.value
                currentUser = state.user
                isAuthenticated = true
            }
        )
    }
}
