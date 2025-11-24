package com.japp.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Intent
import android.app.PendingIntent;
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.remoteMessage
import com.japp.MainActivity
import kotlin.random.Random
import kotlin.random.nextInt


class JappMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        message.notification?.let { msg ->
            sendNotification(msg)
        }
    }

    private fun sendNotification(message: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val channel_id = "default_channel"
        val channelName = "Main notification channel"

        val notificationBuilder = NotificationCompat.Builder(this, channel_id)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channel_id, channelName, IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val random: Random = Random.Default

        manager.notify(random.nextInt(), notificationBuilder.build())


    }
}
