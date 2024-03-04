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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import kotlin.math.roundToLong

class MyService : Service() {

    companion object {
        var isRunning = mutableStateOf(false)
    }

    private val client = OkHttpClient()
    private val url = "https://www.ssi.com.vn/khach-hang-ca-nhan/hieu-qua-dau-tu-cua-quy-sca"
//    private val url = "https://api.thingspeak.com/channels/2454460/feeds/last.json?api_key=WWMIUD0U48OX0/8R3"
    private val request = okhttp3.Request.Builder()
        .url(url)
        .build()

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            getValue()
            handler.postDelayed(this, 10 * 1000) // 12 hours in milliseconds
        }
    }

    private val sharedPreferences: SharedPreferences
        get() = getSharedPreferences("com.example.ssiam", MODE_PRIVATE)
    private val manager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification("SSIAM is running")
        startForeground(1, notification)
        handler.post(runnable)
        isRunning.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        handler.removeCallbacks(runnable)
    }

    private fun getValue() {
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.body?.string()?.let {
                    val money = (Jsoup.parse(it).select("div.numberHeading.clone-h3").text()
                            .replace(".", "")
                            .replace(',', '.')
                            .toDouble() * 14.63)
                            .roundToLong()

//                    val type = object : TypeToken<Map<String, String>>() {}.type
//                    val infor = Gson().fromJson<Map<String, String>>(it, type)
//                    val money = infor["field1"]?.toLong() ?: 0
                    val lastMoney = sharedPreferences.getLong("money", 0)
                    if (money != lastMoney) {
                        sharedPreferences.edit().putLong("money", money).apply()
                        val intent = Intent("com.example.ssiam.MONEY_UPDATE")
                        intent.putExtra("money", money)
                        sendBroadcast(intent)

                        val notification = createNotification("Money change to: ${String.format("%,d", money)} VND")
                        manager.notify(2, notification)
                    }
                }
            }
        })
    }

    private fun createNotificationChannel() {
        val name = "SSIAM"
        val importance = NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel("SSIAM", name, importance)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        val notiIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notiIntent, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "SSIAM")
            .setContentTitle("SSIAM")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
}