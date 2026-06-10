package com.dotz.launcherpro.data

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface StoreBridge {
    val isPremium: StateFlow<Boolean>
    val monthlyPrice: StateFlow<String>
    val yearlyPrice: StateFlow<String>
    val lifetimePrice: StateFlow<String>

    fun startBillingFlow(activity: Activity, productId: String)
    fun refreshPremiumStatus()
    fun getCurrentLocation(callback: (Double, Double) -> Unit, fallback: () -> Unit)
}
