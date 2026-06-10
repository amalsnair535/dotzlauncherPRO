package com.dotz.launcherpro.data

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreBridgeImpl(
    private val context: Context,
    private val prefs: DotzPreferencesRepository
) : StoreBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Always false for Indus build - Pro features are Play Store exclusive
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override val isUpgradeAvailable: Boolean = false

    override val monthlyPrice = MutableStateFlow("N/A")
    override val yearlyPrice = MutableStateFlow("N/A")
    override val lifetimePrice = MutableStateFlow("N/A")

    init {
        // Ensure premium is false in prefs for Indus build
        scope.launch {
            prefs.setPremium(false)
        }
    }

    override fun refreshPremiumStatus() {
        _isPremium.value = false
    }

    override fun startBillingFlow(activity: Activity, productId: String) {
        AlertDialog.Builder(activity)
            .setTitle("PRO Exclusive")
            .setMessage("Premium features like Transparency, Wallpapers, and List Layout are exclusive to the Google Play Store version of Dotz Launcher.")
            .setPositiveButton("OK", null)
            .show()
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
                        locationManager.requestSingleUpdate(provider, object : LocationListener {
                            override fun onLocationChanged(l: Location) { callback(l.latitude, l.longitude) }
                            @Deprecated("Deprecated in Java")
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
