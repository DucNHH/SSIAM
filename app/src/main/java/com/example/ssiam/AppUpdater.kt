package com.example.ssiam

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject

class AppUpdater {
    companion object {
        private lateinit var apkUrl: String
        fun checkForUpdates(): Boolean {
            HttpHandler.handle(Constant.UPDATE_URL, Constant.AUTHORIZATION_TOKEN)?.let {
                val json = JSONObject(it)
                val version = json.getString("tag_name")
                if (version != BuildConfig.VERSION_NAME) {
                    apkUrl = json.getJSONArray("assets").getJSONObject(0).getString("url")
                    return true
                }
            }
            return false
        }

        fun downloadAndInstallApk(context: Context) {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .addRequestHeader("Authorization", "Bearer ${Constant.AUTHORIZATION_TOKEN}")
                .addRequestHeader("Accept", "application/octet-stream")
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "MySSIAM.apk"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(DownloadManager::class.java)
            val downloadId = downloadManager.enqueue(request)

            val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.w("AppUpdater", "APK Download Complete")
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                val uriColumnIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                val uriString =
                                    if (uriColumnIndex != -1) cursor.getString(uriColumnIndex) else null
                                Log.w("AppUpdater", "URI: $uriString")
                                installApkPendingActivity(context, Uri.parse(uriString))
                            }
                        }
                        cursor.close()
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        fun installApkPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}