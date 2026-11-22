package com.example.falldetection.detection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.falldetection.MainActivity
import com.example.falldetection.R

object AppNotifications {
    const val CHANNEL_MONITOR = "monitoring"
    const val CHANNEL_ALERT = "emergency"
    const val ID_MONITOR = 1001
    const val ID_RESULT = 1002
    const val ID_ALERT = 1003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
        val monitor = NotificationChannel(
            CHANNEL_MONITOR,
            context.getString(R.string.notification_channel_monitor),
            NotificationManager.IMPORTANCE_LOW,
        )
        val alert = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.notification_channel_alert),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { enableVibration(true) }
        manager.createNotificationChannel(monitor)
        manager.createNotificationChannel(alert)
    }

    fun monitorNotification(context: Context, title: String, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun resultNotification(context: Context, title: String, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    /** Peringatan jatuh memakai full-screen intent agar langsung terlihat walau layar mati. */
    fun alertNotification(context: Context, text: String): Notification {
        val intent = Intent(context, AlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreen = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Jatuh terdeteksi")
            .setContentText(text)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    fun show(context: Context, id: Int, notification: Notification) {
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
        manager.notify(id, notification)
    }

    fun cancel(context: Context, id: Int) {
        requireNotNull(context.getSystemService(NotificationManager::class.java)).cancel(id)
    }
}
