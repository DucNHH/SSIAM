package com.example.ssiam.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.ssiam.MONEY_UPDATE_WORK
import com.example.ssiam.worker.AppUpdateWorker
import com.example.ssiam.worker.MoneyUpdateWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerRepo @Inject constructor(@ApplicationContext context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun getUpdatedMoney() {
        val request = PeriodicWorkRequestBuilder<MoneyUpdateWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(MONEY_UPDATE_WORK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun checkForUpdate(): LiveData<WorkInfo> {
        val request = OneTimeWorkRequestBuilder<AppUpdateWorker>().build()
        workManager.enqueue(request)
        return workManager.getWorkInfoByIdLiveData(request.id)
    }
}