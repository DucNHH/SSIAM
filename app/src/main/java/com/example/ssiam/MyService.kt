package com.example.ssiam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlin.math.roundToLong

class MyService : Service() {

    companion object {
        var isRunning = mutableStateOf(false)
        var delayTime = Constant.FOREGROUND_DELAY
    }
    private val handler = Handler(Looper.getMainLooper())
    private val sharedPreferences: SharedPreferences
        get() = getSharedPreferences(Constant.PREF_NAME, MODE_PRIVATE)
    private val manager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(1, createNotification("SSIAM is running"))
        getValue() // 12 hours in milliseconds
        isRunning.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        handler.removeCallbacks(::getValue)
        scope.cancel()
    }

    private fun getValue() {
        scope.launch {
            HttpHandler.handle(Constant.VALUE_URL)?.let {
                val money = parseMoney(it)
                val lastMoney = sharedPreferences.getLong(Constant.PREF_MONEY, 0)
                if (money != lastMoney) {
                    updateMoney(money)
                    sendMoneyUpdateBroadcast(money)
                    notifyMoneyChange(money)
                }
            }
            handler.postDelayed(::getValue, delayTime)
        }
    }

    private fun parseMoney(response: String): Long {
        val moneyText = Jsoup.parse(response).select("div.numberHeading.clone-h3").text()
        val moneyValue = moneyText.replace(".", "").replace(',', '.').toDouble() * Constant.CCQ_AMOUNT
        return moneyValue.roundToLong()
    }

    private fun updateMoney(money: Long) {
        sharedPreferences.edit().putLong(Constant.PREF_MONEY, money).apply()
    }

    private fun sendMoneyUpdateBroadcast(money: Long) {
        sendBroadcast(Intent(Constant.MONEY_UPDATE).putExtra(Constant.PREF_MONEY, money))
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