package com.japp.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.japp.MainActivity
import com.japp.api.CredentialsStorage
import com.japp.api.RetrofitClient
import com.japp.api.responses.user.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class JappMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "JappMessagingService"
        private const val CHANNEL_ID = "japp_notifications_v2"
        private const val CHANNEL_NAME = "Japp Notifications"

        fun refreshToken(context: Context) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Token retrieved: ${token.take(20)}...")

                registerTokenWithBackend(context, token)
            }
        }

        private fun registerTokenWithBackend(context: Context, token: String) {
            val credentials = CredentialsStorage.load(context)
            if (credentials == null) {
                Log.d(TAG, "No credentials, skipping token registration")
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.userService.registerFcmToken(
                        FcmTokenRequest(token)
                    )
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token registered with backend")
                    } else {
                        Log.e(TAG, "Failed to register token: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering FCM token", e)
                }
            }
        }

        fun clearToken(context: Context) {
            val credentials = CredentialsStorage.load(context)
            if (credentials == null) {
                Log.d(TAG, "No credentials, skipping token clear")
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.userService.clearFcmToken()
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token cleared from backend")
                    } else {
                        Log.w(TAG, "Failed to clear token: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing FCM token", e)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(20)}...")
        registerTokenWithBackend(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Has notification: ${message.notification != null}")
        Log.d(TAG, "Has data: ${message.data.isNotEmpty()}")

        val notificationTitle = message.notification?.title ?: message.data["title"]
        val notificationBody = message.notification?.body ?: message.data["body"]

        Log.d(TAG, "Title: $notificationTitle, Body: $notificationBody")

        if (notificationTitle != null && notificationBody != null) {
            sendNotification(notificationTitle, notificationBody, message.data)
        }

        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        Log.d(TAG, "sendNotification called - Title: $title, Body: $body")

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random.nextInt()

        Log.d(TAG, "Showing notification with ID: $notificationId")
        manager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification posted")
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel != null) {
            Log.d(TAG, "Channel exists with importance: ${existingChannel.importance}")
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for expenses, settlements, and group activity"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }

        manager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created/updated")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val groupId = data["groupId"]

        when (type) {
            "expense_created" -> {
                Log.d(TAG, "New expense in group $groupId")
            }
            "settlement_completed" -> {
                Log.d(TAG, "Settlement completed in group $groupId")
            }
            "message_received" -> {
                Log.d(TAG, "New message in group $groupId")
            }
            "added_to_group" -> {
                Log.d(TAG, "Added to group $groupId")
            }
        }
    }
}
