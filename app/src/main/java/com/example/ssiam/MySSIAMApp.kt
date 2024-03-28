package com.example.ssiam

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.ssiam.repository.UserPreferenceRepo
import com.example.ssiam.repository.WorkManagerRepo

private const val PREFERENCE_NAME = "user_preference"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

class MySSIAMApp: Application() {
    lateinit var userPreferenceRepo: UserPreferenceRepo
    lateinit var workManagerRepo: WorkManagerRepo

    override fun onCreate() {
        super.onCreate()
        userPreferenceRepo = UserPreferenceRepo(dataStore)
        workManagerRepo = WorkManagerRepo(this)
    }
}