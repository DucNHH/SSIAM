package com.example.ssiam

import okhttp3.OkHttpClient

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
    }
}