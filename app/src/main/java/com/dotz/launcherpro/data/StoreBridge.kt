package com.dotz.launcherpro.data

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface StoreBridge {
    val isPremium: StateFlow<Boolean>
    val isUpgradeAvailable: Boolean
    val isLiteVersion: Boolean
    val monthlyPrice: StateFlow<String>
    val yearlyPrice: StateFlow<String>
    val lifetimePrice: StateFlow<String>
    val isStoreConnected: StateFlow<Boolean>

    fun startBillingFlow(activity: Activity, productId: String)
    fun refreshPremiumStatus()
}
