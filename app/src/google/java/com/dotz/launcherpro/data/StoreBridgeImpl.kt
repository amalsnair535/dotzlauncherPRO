package com.dotz.launcherpro.data

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.android.billingclient.api.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
) : StoreBridge, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Billing
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override val isUpgradeAvailable: Boolean = true

    private val _monthlyPrice = MutableStateFlow("$1.99 / month")
    override val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow("$14.99 / year")
    override val yearlyPrice: StateFlow<String> = _yearlyPrice.asStateFlow()

    private val _lifetimePrice = MutableStateFlow("$29.99 once")
    override val lifetimePrice: StateFlow<String> = _lifetimePrice.asStateFlow()

    private var productDetailsList: List<ProductDetails> = emptyList()

    // Location
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshPremiumStatus()
                    queryProductDetails()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    override fun refreshPremiumStatus() {
        if (!billingClient.isReady) return
        
        // Subs
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val premium = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                updatePremium(premium)
                if (!premium) queryOneTimePurchases()
            }
        }
    }

    private fun queryOneTimePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                    updatePremium(true)
                }
            }
        }
    }

    private fun updatePremium(value: Boolean) {
        _isPremium.value = value
        scope.launch { prefs.setPremium(value) }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_pro_monthly").setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_pro_yearly").setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_pro_lifetime").setProductType(BillingClient.ProductType.INAPP).build()
        )

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        ) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList = detailsList
                detailsList.forEach { details ->
                    when (details.productId) {
                        "dotz_pro_monthly" -> _monthlyPrice.value = details.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice ?: _monthlyPrice.value
                        "dotz_pro_yearly" -> _yearlyPrice.value = details.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice ?: _yearlyPrice.value
                        "dotz_pro_lifetime" -> _lifetimePrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice ?: _lifetimePrice.value
                    }
                }
            }
        }
    }

    override fun startBillingFlow(activity: Activity, productId: String) {
        val details = productDetailsList.find { it.productId == productId }
        if (details == null) {
            android.widget.Toast.makeText(activity, "Store connection in progress. Please try again in a moment.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val offerToken = details.subscriptionOfferDetails?.get(0)?.offerToken
        
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .apply { if (offerToken != null) setOfferToken(offerToken) }
                    .build()
            )).build()

        billingClient.launchBillingFlow(activity, params)
    }

    override fun getCurrentLocation(callback: (Double, Double) -> Unit, fallback: () -> Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) callback(loc.latitude, loc.longitude) else fallback()
                    }
                    .addOnFailureListener { fallback() }
            } catch (e: SecurityException) { fallback() }
        } else {
            fallback()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                ) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        updatePremium(true)
                    }
                }
            } else {
                updatePremium(true)
            }
        }
    }
}
