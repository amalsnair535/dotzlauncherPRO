package com.dotz.launcherpro.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdsManager(private val app: Application) {

    private var rewardedAd: RewardedAd? = null
    private var nativeAd: NativeAd? = null

    private val _nativeAdFlow = MutableStateFlow<NativeAd?>(null)
    val nativeAdFlow = _nativeAdFlow.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading = _isAdLoading.asStateFlow()

    fun init() {
        MobileAds.initialize(app)
    }

    private fun createPrivacyRequest(): AdRequest {
        val extras = Bundle()
        extras.putString("npa", "1")
        return AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
    }

    fun loadRewardedAd(onAdLoaded: () -> Unit = {}) {
        if (rewardedAd != null) {
            onAdLoaded()
            return
        }
        _isAdLoading.value = true
        RewardedAd.load(app, "ca-app-pub-9236556912103771/9239680860", createPrivacyRequest(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                _isAdLoading.value = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                _isAdLoading.value = false
                onAdLoaded()
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) {
                onRewardEarned()
                rewardedAd = null
            }
        } else {
            loadRewardedAd {
                rewardedAd?.show(activity) {
                    onRewardEarned()
                    rewardedAd = null
                }
            }
        }
    }

    fun loadNativeAd() {
        val adLoader = AdLoader.Builder(app, "ca-app-pub-9236556912103771/1133960139")
            .forNativeAd { ad : NativeAd ->
                nativeAd?.destroy()
                nativeAd = ad
                _nativeAdFlow.value = ad
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(createPrivacyRequest())
    }

    fun destroy() {
        nativeAd?.destroy()
    }
}
