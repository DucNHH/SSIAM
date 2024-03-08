package com.example.ssiam

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Response

class HttpHandler {
    companion object {
        private val client = OkHttpClient()
        fun handle(url: String): String? {
            val request = okhttp3.Request.Builder()
                .url(url)
                .build()
            return client.newCall(request).execute().body?.string()
        }

        fun handle(url: String, token: String): String? {
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            return client.newCall(request).execute().body?.string()
        }

        fun asyncHandle(url: String, callback: (Response) -> Unit) {
            val request = okhttp3.Request.Builder()
                .url(url)
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w("HttpHandler", "Failed to execute request for ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    callback(response)
                }
            })
        }

        fun asyncHandle(url: String, token: String, callback: (Response) -> Unit) {
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w("HttpHandler", "Failed to execute request for ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    callback(response)
                }
            })
        }
    }
}