package com.example.ssiam.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ssiam.CCQ_AMOUNT
import com.example.ssiam.HttpHandler
import com.example.ssiam.VALUE_URL
import com.example.ssiam.VERSION_URL
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import org.jsoup.Jsoup
import kotlin.math.roundToLong

class MoneyUpdateWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    companion object {
        val outputData = MutableSharedFlow<Long>()
    }

    override suspend fun doWork(): Result {
        val htmlContent = HttpHandler.handle(VALUE_URL)
//        val htmlContent = HttpHandler.handle(VERSION_URL)
        return if (htmlContent != null) {
            val newMoney = parseMoney(htmlContent)
            Log.w("MoneyUpdateWorker", "New money: $newMoney")
            outputData.emit(newMoney)
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