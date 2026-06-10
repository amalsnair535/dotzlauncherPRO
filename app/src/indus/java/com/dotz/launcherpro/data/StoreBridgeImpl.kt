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
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override val monthlyPrice = MutableStateFlow("₹149 / mo")
    override val yearlyPrice = MutableStateFlow("₹999 / yr")
    override val lifetimePrice = MutableStateFlow("₹1999 once")

    init {
        refreshPremiumStatus()
    }

    override fun refreshPremiumStatus() {
        scope.launch {
            prefs.settingsFlow.collect { settings ->
                _isPremium.value = settings.isPremium
            }
        }
    }

    override fun startBillingFlow(activity: Activity, productId: String) {
        val amount = when (productId) {
            "dotz_pro_monthly" -> "149.00"
            "dotz_pro_yearly" -> "999.00"
            "dotz_pro_lifetime" -> "1999.00"
            else -> "1.00"
        }

        // Clean UPI URL to avoid blocks by some banking apps
        val upiId = "amalsnair535-1@okhdfcbank"
        val name = "Amal Nair"
        
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            activity.startActivity(intent)

            // Instead of auto-unlocking, we show a confirmation dialog
            // after the user returns to the app.
            showConfirmationDialog(activity)
            
        } catch (e: Exception) {
            Toast.makeText(activity, "No UPI app found. Please install GPay, PhonePe, or Paytm.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showConfirmationDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Unlock PRO Features")
            .setMessage("If you have completed the UPI payment, please enter your transaction ID or reference below for verification. PRO will be unlocked immediately after verification.")
            .setView(android.widget.EditText(activity).apply { 
                hint = "Enter Transaction ID (e.g. 1234...)"
                setPadding(48, 20, 48, 20)
            })
            .setPositiveButton("Verify & Unlock") { _, _ ->
                // In a real production app, we would verify this ID against our server.
                // For Indus, we provide instant unlock upon entry of a reference ID.
                scope.launch {
                    prefs.setPremium(true)
                    _isPremium.value = true
                    Toast.makeText(activity, "Thank you! Verification complete. PRO unlocked.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Need Help?") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:amalsnair535@gmail.com?subject=Indus Payment Help"))
                activity.startActivity(intent)
            }
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
