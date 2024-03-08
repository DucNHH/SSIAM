package com.example.ssiam

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

class AppUpdater(private val context: Context) {
    companion object {
        var downloadId = -1L
        private lateinit var downloadManager: DownloadManager
        lateinit var destination: String
    }

    private lateinit var apkUrl: String
    var isUpdateAvailable = mutableStateOf(false)
    fun checkForUpdates() {
        HttpHandler.asyncHandle(Constant.UPDATE_URL, Constant.AUTHORIZATION_TOKEN, ::callback)
    }

    private fun callback(response: okhttp3.Response) {
        response.body?.string()?.let {
            val json = JSONObject(it)
            val version = json.getString("tag_name")
            if (version != BuildConfig.VERSION_NAME) {
                apkUrl = json.getJSONArray("assets").getJSONObject(0).getString("url")
                isUpdateAvailable.value = true
            }
        }
    }

    fun downloadApk() {
        destination =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/MySSIAM.apk"
        val uri = Uri.parse("file://$destination")
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("MySSIAM")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Authorization", "Bearer ${Constant.AUTHORIZATION_TOKEN}")
            .addRequestHeader("Accept", "application/octet-stream")
            .setDestinationUri(uri)

        downloadManager = context.getSystemService(DownloadManager::class.java)
        downloadId = downloadManager.enqueue(request)
    }
}

class DownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.w("AppUpdater", "APK Download Complete")
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id != -1L && id == AppUpdater.downloadId) {
            val query = DownloadManager.Query().setFilterById(id)
            val downloadManager = context.getSystemService(DownloadManager::class.java)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        File(AppUpdater.destination)
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
}