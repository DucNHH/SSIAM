package com.example.ssiam.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.ssiam.BuildConfig
import com.example.ssiam.HttpHandler
import com.example.ssiam.VERSION_URL
import org.json.JSONObject

class AppUpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams){
    override fun doWork(): Result {
        val versionInfo = fetchVersionInfo()
        return if (versionInfo != null) {
            handleVersionInfo(versionInfo)
        } else {
            Result.failure()
        }
    }

    private fun fetchVersionInfo(): String? {
        return HttpHandler.handle(VERSION_URL)
    }

    private fun handleVersionInfo(versionInfo: String): Result {
        val latestVersion = JSONObject(versionInfo).getString("field1")
        val isUpdateAvailable = isUpdateAvailable(latestVersion)
        val outputData = workDataOf("isUpdateAvailable" to isUpdateAvailable, "latestVersion" to latestVersion)
        return Result.success(outputData)
    }

    private fun isUpdateAvailable(latestVersion: String): Boolean {
        val currentVersion = BuildConfig.VERSION_NAME
        return latestVersion > currentVersion
    }
}