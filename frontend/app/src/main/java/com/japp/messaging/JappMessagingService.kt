package com.japp.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.japp.MainActivity
import com.japp.R
import com.japp.api.CredentialsStorage
import com.japp.api.RetrofitClient
import com.japp.api.responses.user.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

class JappMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "JappMessagingService"
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Japp Notifications"

        /**
         * Get current FCM token and register with backend.
         * Should be called when starting the app (logging in).
         */
        fun refreshToken(context: Context) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Token retrieved: ${token.take(20)}...")

                // Register with backend
                registerTokenWithBackend(context, token)
            }
        }

        /**
         * Register FCM token with backend.
         */
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

        /**
         * Clear FCM token from backend on logout.
         */
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

        // Register new token with backend
        registerTokenWithBackend(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // notification payload (shown automatically if app in background)
        message.notification?.let { notification ->
            sendNotification(notification)
        }

        // data payload (for custom handling)
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }
    }

    private fun sendNotification(message: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.japp_icon)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for expenses, settlements, and group activity"
        }
        manager.createNotificationChannel(channel)

        manager.notify(Random.nextInt(), notificationBuilder.build())
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
        }
    }
}
