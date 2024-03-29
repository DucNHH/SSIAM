package com.example.ssiam.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ssiam.CCQ_AMOUNT
import com.example.ssiam.HttpHandler
import com.example.ssiam.MyChannel
import com.example.ssiam.VALUE_URL
import com.example.ssiam.repository.UserPreferenceRepo
import kotlinx.coroutines.flow.first
import org.jsoup.Jsoup
import kotlin.math.roundToLong

class MoneyUpdateWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val userPreferenceRepo = UserPreferenceRepo(appContext)
    private val myChannel = MyChannel(appContext)

    override suspend fun doWork(): Result {
        val htmlContent = HttpHandler.handle(VALUE_URL)
//        val htmlContent = HttpHandler.handle(VERSION_URL)
        return if (htmlContent != null) {
            val newMoney = parseMoney(htmlContent)
            Log.w("MoneyUpdateWorker", "New money: $newMoney")
            val lastMoney = userPreferenceRepo.lastMoney.first()
            if (lastMoney != newMoney) {
                userPreferenceRepo.saveLastMoney(newMoney)
                myChannel.notify("Money updated: $newMoney")
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    private fun parseMoney(htmlContent: String): Long {
        val moneyText = Jsoup.parse(htmlContent).select("div.numberHeading.clone-h3").text()
//        val moneyText = JSONObject(htmlContent).getString("field2")
        val moneyValue = moneyText.replace(".", "").replace(',', '.').toDouble()
        return (moneyValue * CCQ_AMOUNT).roundToLong()
    }
}