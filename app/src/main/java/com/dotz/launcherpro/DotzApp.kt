package com.dotz.launcherpro

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dotz_settings")

class DotzApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
