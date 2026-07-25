package com.dotz.launcherpro

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dotz.launcherpro.data.DotzPreferencesRepository
import com.dotz.launcherpro.data.StoreBridge
import com.dotz.launcherpro.data.StoreBridgeImpl
import com.dotz.launcherpro.manager.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dotz_settings")

class DotzApp : Application() {

    lateinit var prefsRepository: DotzPreferencesRepository
    lateinit var storeBridge: StoreBridge
    lateinit var locationManager: LocationManager
    lateinit var systemStateManager: SystemStateManager
    lateinit var weatherManager: WeatherManager
    lateinit var mediaManager: MediaManager
    lateinit var adsManager: AdsManager

    companion object {
        lateinit var instance: DotzApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        android.util.Log.d("DotzApp", "onCreate: Initializing app...")
        
        prefsRepository = DotzPreferencesRepository(this)
        
        locationManager = LocationManager(this)
        systemStateManager = SystemStateManager(this)
        weatherManager = WeatherManager(prefsRepository, locationManager)
        mediaManager = MediaManager(this)
        adsManager = AdsManager(this)
        
        adsManager.init()
        systemStateManager.start()
        mediaManager.start()

        android.util.Log.d("DotzApp", "Initializing StoreBridgeImpl...")
        storeBridge = StoreBridgeImpl(this, prefsRepository)
    }
}
