package com.example.ssiam

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AppUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    private val destination: String = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + APK_NAME
    private val downloadManager: DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun deleteRedundantApk() {
        val file = File(destination)
        if (file.exists()) {
            file.delete()
        }
    }

    fun startDownload(url: String) {
        val uri = Uri.parse("file://$destination")
        val request = createDownloadRequest(uri, url)
        val downloadId = downloadManager.enqueue(request)
        registerReceiver(downloadId)
    }

    private fun createDownloadRequest(uri: Uri, apkUrl: String): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("MySSIAM")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Accept", "application/octet-stream")
            .setDestinationUri(uri)
    }

    private fun registerReceiver(downloadId: Long) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != -1L && id == downloadId) {
                    handleDownloadComplete(id)
                    context.unregisterReceiver(this)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    private fun handleDownloadComplete(id: Long) {
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(destination)
                )
                val install = Intent(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    .setDataAndType(contentUri, "application/vnd.android.package-archive")
                context.startActivity(install)
            }
        }
        cursor.close()
    }
}