package com.example.ssiam.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferenceRepo(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val LAST_MONEY = longPreferencesKey("last_money")
    }

    val lastMoney: Flow<Long> = dataStore.data
        .catch {
            if (it is Exception) {
                Log.e("UserPreferenceRepo", "Error reading preferences", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[LAST_MONEY] ?: 0L
        }

    suspend fun saveLastMoney(money: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_MONEY] = money
        }
    }

    suspend fun getLastMoney(): Long {
        return dataStore.data.first()[LAST_MONEY] ?: 0L
    }
}