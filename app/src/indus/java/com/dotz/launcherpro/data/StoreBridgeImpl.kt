package com.dotz.launcherpro.data

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StoreBridgeImpl(
    private val context: Context,
    private val prefs: DotzPreferencesRepository
) : StoreBridge {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override val monthlyPrice = MutableStateFlow("Free Trial")
    override val yearlyPrice = MutableStateFlow("Indus Special")
    override val lifetimePrice = MutableStateFlow("Unlock PRO")

    init {
        // In the Indus version, we can check a custom license or just default to prefs
        // For now, let's keep it restricted like Google but without the actual Billing SDK
        refreshPremiumStatus()
    }

    override fun refreshPremiumStatus() {
        // You could implement Indus-specific license check here
    }

    override fun startBillingFlow(activity: Activity, productId: String) {
        // Redirect to Indus Pay or show a message
        // For now, we'll just unlock it to allow testing the Indus build
        _isPremium.value = true
        // Note: In a real app, you would integrate the Indus SDK here
    }

    override fun getCurrentLocation(callback: (Double, Double) -> Unit, fallback: () -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else {
                    null
                }

                if (provider != null) {
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null) {
                        callback(lastKnown.latitude, lastKnown.longitude)
                    } else {
                        // Request a single update
                        locationManager.requestSingleUpdate(provider, object : LocationListener {
                            override fun onLocationChanged(l: Location) { callback(l.latitude, l.longitude) }
                            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                            override fun onProviderEnabled(p: String) {}
                            override fun onProviderDisabled(p: String) {}
                        }, null)
                    }
                } else {
                    fallback()
                }
            } catch (e: Exception) { fallback() }
        } else {
            fallback()
        }
    }
}
