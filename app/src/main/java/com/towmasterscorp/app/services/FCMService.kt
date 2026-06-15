package com.towmasterscorp.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.towmasterscorp.app.MainActivity
import com.towmasterscorp.app.R
import com.towmasterscorp.app.data.preferences.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "towmasters_notifications"
        const val CHAT_CHANNEL_ID = "towmasters_chat"
        private var notificationId = 2000
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val authPreferences = AuthPreferences(applicationContext)
        serviceScope.launch {
            authPreferences.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        createNotificationChannels()

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "TowMasters"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: "You have a new notification"

        val callId = message.data["call_id"]
        val alertType = message.data["alert_type"] ?: message.data["type"] ?: ""

        showNotification(title, body, callId, alertType)
    }

    private fun showNotification(title: String, body: String, callId: String?, alertType: String) {
        val isChat = alertType == "chat"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (callId != null) putExtra("call_id", callId)
            putExtra("notification_type", alertType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isChat) CHAT_CHANNEL_ID else CHANNEL_ID
        val soundUri = if (isChat) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            android.net.Uri.parse("android.resource://${packageName}/${R.raw.alarm_tone}")
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (isChat) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(if (isChat) longArrayOf(0, 200) else longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val callChannel = NotificationChannel(
                CHANNEL_ID, "Call Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dispatch notifications and call updates"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(callChannel)

            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Chat message notifications"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(chatChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
