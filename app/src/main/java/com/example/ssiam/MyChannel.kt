package com.example.ssiam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MyChannel @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel("SSIAM", "SSIAM", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val notiIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notiIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, "SSIAM")
            .setContentTitle("SSIAM")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun notify(message: String) {
        val notification = createNotification(message)
        manager.notify(2, notification)
    }
}