package com.example.ssiam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.mutableStateOf

class MoneyUpdateService : Service() {

    companion object {
        var isRunning = mutableStateOf(false)
        var delayTime = Constant.FOREGROUND_DELAY
    }

    private val handler = Handler(Looper.getMainLooper())
    private val manager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1, createNotification("SSIAM is running"))
        getUpdateValue() // 12 hours in milliseconds
        isRunning.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        handler.removeCallbacks(::getUpdateValue)
    }

    private fun getUpdateValue() {
        HttpHandler.asyncHandle(Constant.VALUE_URL, ::callback)
        handler.postDelayed(::getUpdateValue, delayTime)
    }

    private fun callback(response: okhttp3.Response) {
        response.body?.string()?.let {
            MoneyManager.getInstance(this).handleUpdate(it)?.let { money ->
                notifyMoneyChange(money)
            }
        }
    }
    private fun notifyMoneyChange(money: Long) {
        val notification = createNotification("Money change to: ${String.format("%,d", money)} VND")
        manager.notify(2, notification)
    }

    private fun createNotificationChannel() {
        val name = "SSIAM"
        val importance = NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel("SSIAM", name, importance)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val notiIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notiIntent, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "SSIAM")
            .setContentTitle("SSIAM")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
}