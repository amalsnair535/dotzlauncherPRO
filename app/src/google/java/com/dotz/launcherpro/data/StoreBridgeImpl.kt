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
import kotlinx.coroutines.delay

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
    override val isLiteVersion: Boolean = false

    private val _monthlyPrice = MutableStateFlow("Connecting...")
    override val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow("Connecting...")
    override val yearlyPrice: StateFlow<String> = _yearlyPrice.asStateFlow()

    private val _lifetimePrice = MutableStateFlow("Connecting...")
    override val lifetimePrice: StateFlow<String> = _lifetimePrice.asStateFlow()

    private val _isStoreConnected = MutableStateFlow(false)
    override val isStoreConnected: StateFlow<Boolean> = _isStoreConnected.asStateFlow()

    private var productDetailsList: MutableList<ProductDetails> = mutableListOf()
    private var connectionRetryCount = 0
    private val maxRetries = 5

    // Location
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    init {
        android.util.Log.i("StoreBridgeImpl", "StoreBridgeImpl (GOOGLE) initialized")
        startConnection()
    }

    private fun startConnection() {
        if (billingClient.isReady) {
            android.util.Log.i("StoreBridgeImpl", "BillingClient already ready")
            refreshPremiumStatus()
            fetchProducts()
            return
        }
        
        android.util.Log.i("StoreBridgeImpl", "Starting billing connection...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                android.util.Log.i("StoreBridgeImpl", "onBillingSetupFinished: ${billingResult.responseCode}, ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    connectionRetryCount = 0
                    refreshPremiumStatus()
                    fetchProducts()
                } else {
                    _isStoreConnected.value = false
                    retryConnection()
                }
            }
            override fun onBillingServiceDisconnected() {
                android.util.Log.i("StoreBridgeImpl", "onBillingServiceDisconnected")
                _isStoreConnected.value = false
                retryConnection()
            }
        })
    }

    private fun retryConnection() {
        if (connectionRetryCount < maxRetries) {
            connectionRetryCount++
            android.util.Log.i("StoreBridgeImpl", "Retrying connection ($connectionRetryCount/$maxRetries)...")
            scope.launch {
                delay(3000L * connectionRetryCount)
                startConnection()
            }
        }
    }

    override fun refreshPremiumStatus() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        
        android.util.Log.i("StoreBridgeImpl", "Refreshing premium status...")
        scope.launch {
            val subParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            billingClient.queryPurchasesAsync(subParams) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val premium = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    if (premium) {
                        updatePremium(true)
                    } else {
                        queryOneTimePurchases()
                    }
                }
            }
        }
    }

    private fun queryOneTimePurchases() {
        if (!billingClient.isReady) return
        val inAppParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(inAppParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                    updatePremium(true)
                }
            }
        }
    }

    private fun updatePremium(value: Boolean) {
        android.util.Log.i("StoreBridgeImpl", "Updating premium status to: $value")
        _isPremium.value = value
        if (value) {
            scope.launch { prefs.setPremium(true) }
        }
    }

    private fun fetchProducts() = scope.launch {
        if (!billingClient.isReady) return@launch

        android.util.Log.i("StoreBridgeImpl", "Fetching products...")

        // Fetch INAPP products
        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_launcher_pro").setProductType(BillingClient.ProductType.INAPP).build()
        )
        val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()
        
        billingClient.queryProductDetailsAsync(inAppParams) { result, detailsList ->
            android.util.Log.i("StoreBridgeImpl", "INAPP query result: ${result.responseCode}, count: ${detailsList.size}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                detailsList.forEach { details ->
                    android.util.Log.i("StoreBridgeImpl", "Found INAPP: ${details.productId}")
                    if (details.productId == "dotz_launcher_pro") {
                        _lifetimePrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice ?: _lifetimePrice.value
                        _isStoreConnected.value = true
                    }
                    productDetailsList.removeAll { it.productId == details.productId }
                    productDetailsList.add(details)
                }
            }
        }

        // Fetch SUBS products
        val subList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_pro_monthly").setProductType(BillingClient.ProductType.SUBS).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("dotz_pro_yearly").setProductType(BillingClient.ProductType.SUBS).build()
        )
        val subParams = QueryProductDetailsParams.newBuilder().setProductList(subList).build()

        billingClient.queryProductDetailsAsync(subParams) { result, detailsList ->
            android.util.Log.i("StoreBridgeImpl", "SUBS query result: ${result.responseCode}, count: ${detailsList.size}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                detailsList.forEach { details ->
                    android.util.Log.i("StoreBridgeImpl", "Found SUBS: ${details.productId}")
                    val offer = details.subscriptionOfferDetails?.firstOrNull()
                    val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    when (details.productId) {
                        "dotz_pro_monthly" -> _monthlyPrice.value = price ?: _monthlyPrice.value
                        "dotz_pro_yearly" -> _yearlyPrice.value = price ?: _yearlyPrice.value
                    }
                    productDetailsList.removeAll { it.productId == details.productId }
                    productDetailsList.add(details)
                }
            }
        }
    }

    override fun startBillingFlow(activity: Activity, productId: String) {
        android.util.Log.i("StoreBridgeImpl", "startBillingFlow: $productId")
        val details = productDetailsList.find { it.productId == productId }
        if (details == null) {
            val msg = if (productDetailsList.isEmpty()) {
                "Store connection in progress. Please try again in a moment."
            } else {
                "Product '$productId' not found in the store. Please contact support."
            }
            android.util.Log.w("StoreBridgeImpl", "startBillingFlow failed: $msg")
            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show()
            
            // Try to re-fetch if empty
            if (productDetailsList.isEmpty()) {
                fetchProducts()
            }
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // Only add offer token for subscriptions
        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }
        }
        
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        android.util.Log.i("StoreBridgeImpl", "Launching billing flow for $productId")
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
        android.util.Log.i("StoreBridgeImpl", "onPurchasesUpdated: ${billingResult.responseCode}, ${billingResult.debugMessage}")
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
