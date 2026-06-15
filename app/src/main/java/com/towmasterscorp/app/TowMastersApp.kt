package com.towmasterscorp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.towmasterscorp.app.data.preferences.AuthPreferences
import com.towmasterscorp.app.services.FCMService
import com.towmasterscorp.app.services.LocationService
import kotlinx.coroutines.runBlocking

class TowMastersApp : Application() {

    lateinit var authPreferences: AuthPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        authPreferences = AuthPreferences(this)

        val crashPrefs = getSharedPreferences("crash_flag", Context.MODE_PRIVATE)
        if (crashPrefs.getBoolean("clear_auth", false)) {
            runBlocking { authPreferences.clearAll() }
            crashPrefs.edit().putBoolean("clear_auth", false).commit()
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("TowMasters", "UNCAUGHT CRASH: ${throwable.javaClass.simpleName}: ${throwable.message}", throwable)
            try {
                getSharedPreferences("crash_flag", Context.MODE_PRIVATE)
                    .edit().putBoolean("clear_auth", true).commit()
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Main notification channel
            val mainChannel = NotificationChannel(
                FCMService.CHANNEL_ID,
                "TowMasters Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dispatch notifications and call updates"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(mainChannel)

            // Location tracking channel
            val locationChannel = NotificationChannel(
                LocationService.CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS location tracking for dispatch"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(locationChannel)
        }
    }

    companion object {
        lateinit var instance: TowMastersApp
            private set
    }
}
