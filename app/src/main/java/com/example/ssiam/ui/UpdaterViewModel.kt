package com.example.ssiam.ui

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import com.example.ssiam.APK_NAME
import com.example.ssiam.BASE_UPDATE_URL
import com.example.ssiam.BuildConfig
import com.example.ssiam.MySSIAMApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class UpdaterViewModel(private val app: MySSIAMApp) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MySSIAMApp
                UpdaterViewModel(app)
            }
        }
    }

    private val _uiState = MutableStateFlow(UpdaterUiState())
    val uiState: StateFlow<UpdaterUiState> = _uiState.asStateFlow()

    private val destination = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + APK_NAME

    init {
        deleteRedundantApk()
        observeWorkInfo()
    }

    private fun observeWorkInfo() {
        lateinit var observer: Observer<WorkInfo>
        val workInfoLiveData = app.workManagerRepo.checkForUpdate()
        observer = Observer { workInfo ->
            if (workInfo.state.isFinished) {
                val outputData = workInfo.outputData
                val isUpdateAvailable = outputData.getBoolean("isUpdateAvailable", false)
                val latestVersion = outputData.getString("latestVersion")
                if (isUpdateAvailable) {
                    _uiState.update { curState -> curState.copy(isUpdateAvailable = true, version = latestVersion!!) }
                    workInfoLiveData.removeObserver(observer)
                }
            }
        }
        workInfoLiveData.observeForever(observer)
    }

    fun ignoreUpdate() {
        _uiState.update { curState -> curState.copy(isUpdateAvailable = false) }
    }

    fun startUpdate() {
        _uiState.update { curState -> curState.copy(isUpdateAvailable = false) }
        val uri = Uri.parse("file://$destination")
        val apkUrl = "$BASE_UPDATE_URL/${uiState.value.version}/$APK_NAME"
        val request = createDownloadRequest(uri, apkUrl)
        val downloadManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        registerReceiver(downloadManager, downloadId)
    }

    private fun createDownloadRequest(uri: Uri, apkUrl: String): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("MySSIAM")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Accept", "application/octet-stream")
            .setDestinationUri(uri)
    }

    private fun registerReceiver(downloadManager: DownloadManager, downloadId: Long) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != -1L && id == downloadId) {
                    handleDownloadComplete(downloadManager, id)
                    context.unregisterReceiver(this)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                app,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    private fun handleDownloadComplete(downloadManager: DownloadManager, id: Long) {
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                val contentUri = FileProvider.getUriForFile(
                    app,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(destination)
                )
                val install = Intent(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    .setDataAndType(contentUri, "application/vnd.android.package-archive")
                app.startActivity(install)
            }
        }
        cursor.close()
    }

    private fun deleteRedundantApk() {
        val file = File(destination)
        if (file.exists()) {
            file.delete()
        }
    }
}