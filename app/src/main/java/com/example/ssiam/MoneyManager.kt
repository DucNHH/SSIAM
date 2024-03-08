package com.example.ssiam

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlin.math.roundToLong

class MoneyManager private constructor(context: Context) {

    private val sharedPref = context.getSharedPreferences(Constant.PREF_NAME, MODE_PRIVATE)

    val money = mutableLongStateOf(getSavedMoney())

    private fun getSavedMoney(): Long {
        return sharedPref.getLong(Constant.PREF_MONEY, 0)
    }

    private fun updatePrefMoney(money: Long) {
        sharedPref.edit().putLong(Constant.PREF_MONEY, money).apply()
    }

    fun handleUpdate(response: String): Long? {
        val moneyText = Jsoup.parse(response).select("div.numberHeading.clone-h3").text()
        val moneyValue =
            moneyText.replace(".", "").replace(',', '.').toDouble() * Constant.CCQ_AMOUNT
        val newMoney = moneyValue.roundToLong()
        if (newMoney != money.longValue) {
            CoroutineScope(Dispatchers.Main).launch { money.longValue = newMoney }
            updatePrefMoney(newMoney)
            return newMoney
        }
        return null
    }

    companion object {
        private var instance: MoneyManager? = null

        fun getInstance(context: Context): MoneyManager = instance ?: synchronized(this) {
            instance ?: MoneyManager(context).also { instance = it }
        }
    }
}