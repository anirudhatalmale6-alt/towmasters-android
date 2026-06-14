package com.towmasterscorp.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.towmasterscorp.app.data.preferences.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if user was clocked in and restart location service
            val authPreferences = AuthPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                val token = authPreferences.tokenFlow.first()
                val user = authPreferences.userFlow.first()
                if (!token.isNullOrEmpty() && user?.isClockedIn == true) {
                    val serviceIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START
                    }
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}
