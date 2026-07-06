package com.dotz.launcherpro.viewmodel

import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotz.launcherpro.data.*
import com.dotz.launcherpro.services.DotzNotificationService
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.ads.mediation.admob.AdMobAdapter

data class LauncherUiState(
    val page0Tiles: List<AppTile> = DefaultApps.page0Defaults,
    val page1Tiles: List<AppTile> = DefaultApps.page1Defaults,
    val page2Tiles: List<AppTile> = DefaultApps.page2Defaults,
    val ultraFocusTiles: List<AppTile> = emptyList(),
    val settings: DotzSettings = DotzSettings(),
    val notificationCounts: Map<String, Int> = emptyMap(),
    val batteryLevel: Int = -1,
    val networkStatus: String = "None",
    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isSilentMode: Boolean = false,
    val isTorchOn: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isDarkModeOn: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val weatherTemp: String? = null,
    val weatherCondition: String? = null,
    val activeNotifications: List<com.dotz.launcherpro.services.NotificationItem> = emptyList(),
    val blockedNotificationsCount: Int = 0,
    val nowPlayingTitle: String = "Not Playing",
    val nowPlayingArtist: String = "",
    val nowPlayingAlbum: String = "",
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0,
    val playbackDuration: Long = 0,
    val focusTimeToday: String = "0h 0m",
    val focusTimeMillis: Long = 0,
    val focusStreak: Int = 0,
    val isUpdateAvailable: Boolean = false,
    val isPremium: Boolean = false,
    val isUpgradeAvailable: Boolean = true,
    val isLiteVersion: Boolean = false,
    val isFastlaneVisible: Boolean = false,
    val unlockCount: Int = 0,
    val notificationsReceivedToday: Int = 0,
    val focusScore: Int = 100,
    val topApps: List<DrawerApp> = emptyList(),
    val timelineItems: List<TimelineItem> = emptyList(),
    val nativeAd: com.google.android.gms.ads.nativead.NativeAd? = null,
    val isAdLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val ultraFocusRemainingMillis: Long = 0,
)

enum class ThemeMode { LIGHT, DARK, CIRCADIAN, TRANSPARENT }

data class UsageStatsResult(
    val appStats: Map<String, Pair<String?, Int>>,
    val totalScreenTime: Long,
    val unlockCount: Int,
    val notificationsReceived: Int
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DotzPreferencesRepository(application)
    private val storeBridge = StoreBridgeImpl(application, prefs)
    val iconCache = IconCacheManager(application)
    private val pm: PackageManager = application.packageManager
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val mediaSessionManager = application.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo(metadata, activeController?.playbackState)
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo(activeController?.metadata, state)
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private var rewardedAd: RewardedAd? = null
    private var nativeAd: NativeAd? = null
    private val _nativeAdFlow = MutableStateFlow<NativeAd?>(null)
    private val _isAdLoading = MutableStateFlow(false)
    
    private val installedCache = mutableMapOf<String, Boolean>()
    private var cachedIsDefault = false
    
    /** 
     * Creates a privacy-first AdRequest that forces Non-Personalized Ads (NPA).
     * This prevents user profiling and data harvesting by the ad server.
     */
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
        RewardedAd.load(getApplication(), "ca-app-pub-9236556912103771/9239680860", createPrivacyRequest(), object : RewardedAdLoadCallback() {
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
            rewardedAd?.show(activity, OnUserEarnedRewardListener {
                onRewardEarned()
                rewardedAd = null
            })
        } else {
            loadRewardedAd {
                rewardedAd?.show(activity, OnUserEarnedRewardListener {
                    onRewardEarned()
                    rewardedAd = null
                })
            }
        }
    }

    fun loadNativeAd() {
        val adLoader = AdLoader.Builder(getApplication(), "ca-app-pub-9236556912103771/1133960139")
            .forNativeAd { ad : NativeAd ->
                nativeAd?.destroy()
                nativeAd = ad
                _nativeAdFlow.value = ad
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(createPrivacyRequest())
    }

    fun grant24HourPremium() = viewModelScope.launch {
        val expiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        prefs.setPremiumExpiry(expiry)
    }

    private val _uiState = MutableStateFlow(LauncherUiState(isDefaultLauncher = isDefaultLauncher()))
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _batteryLevel = MutableStateFlow(-1)
    private val _networkStatus = MutableStateFlow("None")
    private val _isWifiEnabled = MutableStateFlow(value = false)
    private val _isBluetoothEnabled = MutableStateFlow(value = false)
    private val _isSilentMode = MutableStateFlow(value = false)
    private val _isTorchOn = MutableStateFlow(value = false)
    private val _isAirplaneModeOn = MutableStateFlow(value = false)
    private val _isDarkModeOn = MutableStateFlow(value = false)
    private val _weatherTemp = MutableStateFlow<String?>(null)
    private val _weatherCondition = MutableStateFlow<String?>(null)
    private val _nowPlaying = MutableStateFlow<Triple<String, String, String>>(Triple("Not Playing", "", ""))
    private val _playbackState = MutableStateFlow<Triple<Boolean, Long, Long>>(Triple(false, 0L, 0L))
    private val _refreshTrigger = MutableStateFlow(Unit)
    private val _timerTicker = MutableStateFlow(System.currentTimeMillis())
    private val _usageStats = MutableStateFlow(UsageStatsResult(emptyMap(), 0L, 0, 0))
    private val _installedAppsCache = MutableStateFlow<List<DrawerApp>>(emptyList())

    private val _isFastlaneVisible = MutableStateFlow(false)
    fun setFastlaneVisible(visible: Boolean) {
        _isFastlaneVisible.value = visible
    }

    private val _currentInnerPage = MutableStateFlow(0)
    val currentInnerPage: StateFlow<Int> = _currentInnerPage.asStateFlow()
    fun setInnerPage(index: Int) {
        _currentInnerPage.value = index
    }

    fun startUltraFocusSession(minutes: Int) = viewModelScope.launch {
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.setUltraFocusEndTime(endTime)
    }

    fun endUltraFocusSession() = viewModelScope.launch {
        prefs.setUltraFocusEndTime(0L)
    }

    private fun refreshIsDefault() {
        cachedIsDefault = isDefaultLauncher()
    }

    private val WEATHER_REFRESH_INTERVAL = 30 * 60 * 1000L // 30 minutes

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if ((level != -1) && (scale != -1)) {
                _batteryLevel.value = ((level * 100) / scale.toFloat()).toInt()
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    _isWifiEnabled.value = wifiManager.isWifiEnabled
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    _isAirplaneModeOn.value = intent.getBooleanExtra("state", false)
                }
            }
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            _isTorchOn.value = enabled
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { updateNetwork() }
        override fun onLost(network: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateNetwork() }

        private fun updateNetwork() {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            _networkStatus.value = when {
                caps == null -> "None"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Eth"
                else -> "Online"
            }
        }
    }

    private var sessionStartTime = System.currentTimeMillis()

    init {
        val app = getApplication<Application>()
        
        // Update streak and reset focus time if it's a new day
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val now = System.currentTimeMillis()
            val lastDate = settings.lastUsedDate
            
            val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val calendarLast = java.util.Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = calendarNow[java.util.Calendar.DAY_OF_YEAR] == calendarLast[java.util.Calendar.DAY_OF_YEAR] &&
                           calendarNow[java.util.Calendar.YEAR] == calendarLast[java.util.Calendar.YEAR]
            
            val isNextDay = !isSameDay && (now - lastDate < 48 * 60 * 60 * 1000) // roughly next day check
            
            val newStreak = if (isNextDay) settings.focusStreak + 1 else if (isSameDay) settings.focusStreak else 1
            val newFocusTime = if (isSameDay) settings.focusTimeToday else 0L
            
            prefs.updateFocusStats(newStreak, now, newFocusTime, resetDrawerCount = !isSameDay)
            if (!isSameDay) {
                _refreshTrigger.value = Unit
            }
        }

        // Periodic update of focus time
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000) // Update every minute
                updateSessionTime()
            }
        }
        
        // Register Battery Receiver
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(batteryReceiver, batteryFilter)
        }

        // Register System Toggles Receiver
        val systemFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        app.registerReceiver(systemReceiver, systemFilter)

        // Initial System States
        refreshIsDefault()
        _isWifiEnabled.value = wifiManager.isWifiEnabled
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        _isAirplaneModeOn.value = Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        _isDarkModeOn.value = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // Torch state tracking
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) { e.printStackTrace() }

        // Register Network Callback
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        } catch (e: Exception) { e.printStackTrace() }

        refreshWeather()
        
        // Periodic update of weather
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _timerTicker.value = System.currentTimeMillis()
                
                // Keep existing weather check frequency
                if (System.currentTimeMillis() % WEATHER_REFRESH_INTERVAL < 1000) {
                    refreshWeather()
                    refreshIsDefault()
                }
            }
        }

        // Periodic update of usage stats
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(300_000) // Every 5 minutes
                _refreshTrigger.value = Unit
            }
        }
        
        // Register Media Session Listener
        val componentName = ComponentName(app, DotzNotificationService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            updateActiveController(mediaSessionManager.getActiveSessions(componentName))
        } catch (e: Exception) { e.printStackTrace() }

        // Periodic playback position and session check
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val current = _playbackState.value
                val fastlaneEnabled = _uiState.value.settings.enableFastlane
                val fastlaneVisible = _isFastlaneVisible.value

                // Only update position if something is playing AND fastlane is actually visible to user
                if (current.component1() && fastlaneEnabled && fastlaneVisible) {
                    val newPos = activeController?.playbackState?.position ?: current.component2()
                    if (newPos != current.component2()) {
                        _playbackState.value = Triple(current.component1(), newPos, current.component3())
                    }
                }

                // If no active controller, or current one is not playing, check for other playing sessions
                // but do this less frequently (e.g. every 5 seconds) to save battery
                if (System.currentTimeMillis() % 5000 < 1000) {
                    if (activeController == null || !current.component1()) {
                        val componentName = ComponentName(app, DotzNotificationService::class.java)
                        try {
                            val sessions = mediaSessionManager.getActiveSessions(componentName)
                            if (sessions.isNotEmpty()) {
                                val playingSession = sessions.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                                if (playingSession != null && playingSession != activeController) {
                                    updateActiveController(sessions)
                                } else if (activeController == null) {
                                    updateActiveController(sessions)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Listen to notifications
        viewModelScope.launch {
            DotzNotificationService.notifications.collect {
                refreshState()
            }
        }

        // Heavy data updates (Apps & Usage)
        viewModelScope.launch(Dispatchers.IO) {
            _refreshTrigger.collect {
                installedCache.clear()
                val usage = if (hasUsageStatsPermission()) getAllAppStatsToday() else UsageStatsResult(emptyMap(), 0L, 0, 0)
                _usageStats.value = usage

                val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0)
                    .map {
                        val pkg = it.activityInfo.packageName
                        val label = it.loadLabel(pm).toString()
                        val stats = usage.appStats[pkg]
                        DrawerApp(pkg, label, stats?.component1(), stats?.component2() ?: 0)
                    }
                    .distinctBy { it.packageName }
                _installedAppsCache.value = apps
            }
        }

        // Main UI State combination
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                DotzNotificationService.notificationCounts,
                _batteryLevel,
                _networkStatus,
                _isWifiEnabled,
                _isBluetoothEnabled,
                _isSilentMode,
                _isTorchOn,
                _isAirplaneModeOn,
                _isDarkModeOn,
                _weatherTemp,
                _weatherCondition,
                DotzNotificationService.notifications,
                DotzNotificationService.blockedCount,
                _nowPlaying,
                _playbackState,
                storeBridge.isPremium,
                _usageStats,
                _installedAppsCache,
                _isFastlaneVisible,
                _nativeAdFlow,
                _isAdLoading,
                _timerTicker
            ) { args: Array<Any?> ->
                val settings = args[0] as DotzSettings
                @Suppress("UNCHECKED_CAST")
                val notifCounts = args[1] as Map<String, Int>
                val battery = args[2] as Int
                val network = args[3] as String
                val wifi = args[4] as Boolean
                val bt = args[5] as Boolean
                val silent = args[6] as Boolean
                val torch = args[7] as Boolean
                val airplane = args[8] as Boolean
                val dark = args[9] as Boolean
                val temp = args[10] as String?
                val condition = args[11] as String?
                @Suppress("UNCHECKED_CAST")
                val notifications = args[12] as List<com.dotz.launcherpro.services.NotificationItem>
                val blocked = args[13] as Int
                @Suppress("UNCHECKED_CAST")
                val nowPlaying = args[14] as Triple<String, String, String>
                @Suppress("UNCHECKED_CAST")
                val playback = args[15] as Triple<Boolean, Long, Long>
                val isPremiumStatus = args[16] as Boolean
                @Suppress("UNCHECKED_CAST")
                val usageResult = args[17] as UsageStatsResult
                @Suppress("UNCHECKED_CAST")
                val allApps = args[18] as List<DrawerApp>
                val fastlaneVisible = args[19] as Boolean
                val nativeAd = args[20] as NativeAd?
                val isAdLoading = args[21] as Boolean
                val tickTime = args[22] as Long

                val isDefault = cachedIsDefault
                val allUsage = usageResult.appStats
                val totalTimeMillis = usageResult.totalScreenTime

                val isCustomProfile = settings.activeProfileId != "default"
                
                val isCurrentlyPremium = settings.isPremium || isPremiumStatus || (settings.premiumExpiry > System.currentTimeMillis())

                val allTilesUnordered = buildTilesFast(DefaultApps.allDefaults, settings, notifCounts, allUsage)
                val allTiles = settings.tileOrder.mapNotNull { id ->
                    allTilesUnordered.find { it.tileId == id }
                }

                // Calculate Focus Score
                // 100 points base. 
                // Deduct 1 point for every unlock above 20.
                // Deduct 1 point for every 10 minutes of screen time.
                val unlockPenalty = ((usageResult.unlockCount - 20).coerceAtLeast(0)) * 1
                val minutesUsed = totalTimeMillis / 60000
                val screenTimePenalty = minutesUsed / 10
                val calculatedScore = (100 - unlockPenalty - screenTimePenalty).toInt().coerceIn(0, 100)

                val p0 = allTiles.take(6)
                val p1 = allTiles.drop(6).take(6)
                val p2 = if (settings.enableExtraPage || isCustomProfile) allTiles.drop(12).take(if (isCustomProfile) 6 else settings.extraTileCount) else emptyList()
                
                val currentTime = System.currentTimeMillis()
                val isUltraFocusActive = settings.ultraFocusEndTime > currentTime
                val remainingMillis = if (isUltraFocusActive) settings.ultraFocusEndTime - tickTime else 0L

                val currentLayoutStyle = if (isUltraFocusActive) "ultra_focus" else settings.layoutStyle
                val ultraFocusTiles = if (currentLayoutStyle == "ultra_focus") allTiles.take(18) else emptyList()

                // Auto Grayscale Check (10 PM to 6 AM)
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val isNightTime = hour >= 22 || hour < 6
                val effectiveGrayscale = settings.grayscaleMode || (settings.autoGrayscale && isNightTime)

                val effectiveSettings = if (isCurrentlyPremium) {
                    settings.copy(grayscaleMode = effectiveGrayscale, isPremium = true, layoutStyle = currentLayoutStyle)
                } else {
                    settings.copy(
                        tileTransparency = 1.0f,
                        layoutStyle = if (isUltraFocusActive) "ultra_focus" else "classic",
                        showWallpaper = false,
                        useCircadianTheming = false,
                        grayscaleMode = effectiveGrayscale,
                        isPremium = false
                    )
                }

                val topApps = allApps
                    .filter { it.usageTime != null }
                    .sortedByDescending { app ->
                        val time = app.usageTime ?: "0m"
                        val hours = if (time.contains("h")) time.substringBefore("h").trim().toInt() else 0
                        val mins = if (time.contains("m")) time.substringAfter("h", time).substringBefore("m").trim().toInt() else 0
                        hours * 60 + mins
                    }
                    .take(5)

                // Calculate Timeline Items within combine to ensure they have the latest data
                val timeline = mutableListOf<TimelineItem>()
                
                // 1. Notifications
                notifications.forEach { notif ->
                    val type = when {
                        notif.packageName.contains("dialer") || notif.packageName.contains("telecom") -> TimelineType.CALL
                        notif.packageName.contains("message") || notif.packageName.contains("whatsapp") || notif.packageName.contains("telegram") -> TimelineType.MESSAGE
                        else -> TimelineType.MESSAGE
                    }
                    timeline.add(TimelineItem(
                        id = notif.key,
                        type = type,
                        title = notif.title ?: "Notification",
                        subtitle = notif.text ?: "",
                        timestamp = notif.postTime,
                        packageName = notif.packageName,
                        canReply = notif.canReply,
                        notificationKey = notif.key
                    ))
                }

                // 2. Music
                if (nowPlaying.component1() != "Not Playing" && nowPlaying.component1().isNotBlank()) {
                    timeline.add(TimelineItem(
                        id = "music_${nowPlaying.component1()}",
                        type = TimelineType.MUSIC,
                        title = nowPlaying.component1(),
                        subtitle = nowPlaying.component2(),
                        timestamp = System.currentTimeMillis(),
                        packageName = activeController?.packageName
                    ))
                }

                // 3. App Launches (Fallback to primary tiles if usage stats are missing)
                val appsToRecommend = if (topApps.isNotEmpty()) {
                    topApps.take(3)
                } else {
                    p0.take(3).map { DrawerApp(it.packageName, it.label, it.usageTime, it.launchCount) }
                }

                appsToRecommend.forEach { app ->
                    timeline.add(TimelineItem(
                        id = "app_${app.packageName}",
                        type = TimelineType.APP_LAUNCH,
                        title = app.label,
                        subtitle = if (app.usageTime != null) "Used for ${app.usageTime}" else "Frequent activity",
                        timestamp = System.currentTimeMillis() - 10000,
                        packageName = app.packageName
                    ))
                }

                // 4. Final List Processing & Sponsored Insertion (Once per 24h, non-premium only)
                val finalTimeline = timeline.sortedByDescending { it.timestamp }.distinctBy { it.id }.toMutableList()
                
                val dayMillis = 24 * 60 * 60 * 1000L
                if (!isCurrentlyPremium && (currentTime - settings.lastSponsoredShowTime > dayMillis)) {
                    // --- ON-DEVICE AD FILTERING ENGINE ---
                    // 1. Define a Local Pool of sponsored items
                    val pool = listOf(
                        TimelineItem(
                            id = "sponsored_weather",
                            type = TimelineType.SPONSORED,
                            title = "Weather Companion",
                            subtitle = "Get minimalist hourly forecasts synced with Dotz.",
                            timestamp = currentTime,
                            packageName = "com.google.android.apps.magellan"
                        ),
                        TimelineItem(
                            id = "sponsored_focus",
                            type = TimelineType.SPONSORED,
                            title = "Deep Work Mode",
                            subtitle = "Enhance your focus score with this Pomodoro tool.",
                            timestamp = currentTime,
                            packageName = "com.google.android.calendar"
                        ),
                        TimelineItem(
                            id = "sponsored_music",
                            type = TimelineType.SPONSORED,
                            title = "Soundscape Discovery",
                            subtitle = "Find high-fidelity tracks that match your minimalist vibe.",
                            timestamp = currentTime,
                            packageName = "com.spotify.music"
                        )
                    )

                    // 2. Local Intent Filtering (analyze top apps to select best ad)
                    val topPkg = topApps.firstOrNull()?.packageName ?: ""
                    val selectedAd = when {
                        topPkg.contains("music") || topPkg.contains("spotify") -> pool[2] // Music
                        topPkg.contains("calendar") || topPkg.contains("notes") -> pool[1] // Productivity
                        else -> pool[0] // Default/Weather
                    }

                    // 3. Insert into the timeline after 6 regular items
                    if (finalTimeline.size >= 6) {
                        finalTimeline.add(6, selectedAd)
                    } else {
                        finalTimeline.add(selectedAd)
                    }
                }

                LauncherUiState(
                    page0Tiles = p0,
                    page1Tiles = p1,
                    page2Tiles = p2,
                    ultraFocusTiles = ultraFocusTiles,
                    settings = effectiveSettings,
                    notificationCounts = notifCounts,
                    batteryLevel = battery,
                    networkStatus = network,
                    isWifiEnabled = wifi,
                    isBluetoothEnabled = bt,
                    isSilentMode = silent,
                    isTorchOn = torch,
                    isAirplaneModeOn = airplane,
                    isDarkModeOn = dark,
                    isDefaultLauncher = isDefault,
                    weatherTemp = temp,
                    weatherCondition = condition,
                    activeNotifications = notifications,
                    blockedNotificationsCount = blocked,
                    nowPlayingTitle = nowPlaying.component1(),
                    nowPlayingArtist = nowPlaying.component2(),
                    nowPlayingAlbum = nowPlaying.component3(),
                    isPlaying = playback.component1(),
                    playbackPosition = playback.component2(),
                    playbackDuration = playback.component3(),
                    focusTimeToday = formatDuration(totalTimeMillis),
                    focusStreak = settings.focusStreak,
                    isPremium = isCurrentlyPremium,
                    isUpgradeAvailable = storeBridge.isUpgradeAvailable,
                    isLiteVersion = storeBridge.isLiteVersion,
                    isFastlaneVisible = fastlaneVisible,
                    unlockCount = usageResult.unlockCount,
                    notificationsReceivedToday = usageResult.notificationsReceived,
                    focusScore = calculatedScore,
                    topApps = topApps,
                    timelineItems = finalTimeline,
                    nativeAd = nativeAd,
                    isAdLoading = isAdLoading,
                    isLoaded = true,
                    ultraFocusRemainingMillis = remainingMillis
                )
            }.flowOn(Dispatchers.Default).collect { state ->
                _uiState.value = state
            }
        }
        
        // Initial refresh
        refreshState()
        loadRewardedAd()
        loadNativeAd()
    }

    fun checkForUpdates() {
        // Removed for Play Store compliance
    }

    fun downloadUpdate(url: String) {
        // Redirect to Play Store
        val app = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        }
    }

    private fun installApk(file: java.io.File) {
        // Removed for Play Store compliance
    }

    private fun updateSessionTime() {
        val now = System.currentTimeMillis()
        val duration = now - sessionStartTime
        sessionStartTime = now
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val lastDate = settings.lastUsedDate
            
            val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
            val calendarLast = Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = calendarNow[Calendar.DAY_OF_YEAR] == calendarLast[Calendar.DAY_OF_YEAR] &&
                           calendarNow[Calendar.YEAR] == calendarLast[Calendar.YEAR]
            
            if (isSameDay) {
                prefs.updateFocusStats(settings.focusStreak, now, settings.focusTimeToday + duration)
            } else {
                // New day reset
                val isNextDay = (now - lastDate < 48 * 60 * 60 * 1000)
                val newStreak = if (isNextDay) settings.focusStreak + 1 else 1
                prefs.updateFocusStats(newStreak, now, duration, resetDrawerCount = true)
                // Force a full usage stats refresh immediately on day change
                _refreshTrigger.value = Unit
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        if (totalMinutes < 1) return "<1m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(mediaCallback)
        // Prefer the one that is currently playing
        activeController = controllers?.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()
        activeController?.registerCallback(mediaCallback)
        updateMediaInfo(activeController?.metadata, activeController?.playbackState)
    }

    private fun updateMediaInfo(metadata: MediaMetadata?, state: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?: if (activeController != null) {
                activeController?.packageName?.substringAfterLast('.')?.uppercase() ?: "ACTIVE SESSION"
            } else "Not Playing"
            
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: if (activeController != null) "Ready to play" else "Play something to see info"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        
        _nowPlaying.value = Triple(title, artist, album)
        
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        // Contextual Header: Auto-switch to music mode when playback starts
        if (isPlaying && !_playbackState.value.first && title != "Not Playing") {
            viewModelScope.launch {
                val currentSettings = prefs.settingsFlow.first()
                if (currentSettings.homeHeaderMode != "music") {
                    prefs.setHomeHeaderMode("music")
                }
            }
        }

        _nowPlaying.value = Triple(title, artist, album)
        _playbackState.value = Triple(isPlaying, position, duration)
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        try {
            app.unregisterReceiver(batteryReceiver)
            app.unregisterReceiver(systemReceiver)
            cameraManager.unregisterTorchCallback(torchCallback)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            activeController?.unregisterCallback(mediaCallback)
            nativeAd?.destroy()
        } catch (e: Exception) { e.printStackTrace() }

        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── System Toggles ────────────────────────────────────────────────────────

    fun toggleWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleWifiDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    fun toggleBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    @Suppress("DEPRECATION")
    fun toggleBluetoothDirect() {
        try {
            val isEnabled = bluetoothAdapter?.isEnabled == true
            if (isEnabled) {
                bluetoothAdapter?.disable()
            } else {
                bluetoothAdapter?.enable()
            }
        } catch (_: SecurityException) {
            // If permission is missing, opening settings is the only fallback
            toggleBluetooth()
        } catch (_: Exception) {
            toggleBluetooth()
        }
    }

    fun toggleSilentMode() {
        val app = getApplication<Application>()
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(app, "Please grant 'Do Not Disturb' access to toggle silent mode.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            return
        }

        val currentMode = audioManager.ringerMode
        val newMode = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        
        try {
            audioManager.ringerMode = newMode
            val modeName = when (newMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Normal"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Silent"
            }
            Toast.makeText(app, "Mode: $modeName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTorch() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, !_isTorchOn.value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleAirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleAirplaneModeDirect() {
        toggleAirplaneMode() // Always requires settings on modern Android
    }

    fun toggleDarkMode() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleDarkModeDirect() {
        setIsLightMode(!uiState.value.settings.isLightMode)
    }

    // ── Media Controls ────────────────────────────────────────────────────────

    fun mediaPlayPause() {
        if (_playbackState.value.component1()) activeController?.transportControls?.pause()
        else activeController?.transportControls?.play()
    }

    fun mediaSkipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun mediaSkipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun launchApp(packageName: String?) {
        if (packageName == null) return
        val app = getApplication<Application>()
        val intent = app.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } else {
            Toast.makeText(app, "Could not open app", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendReply(notificationKey: String, message: String) {
        DotzNotificationService.sendReply(notificationKey, message)
    }

    fun openMobileDataSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(fallback)
        }
    }

    fun toggleMobileDataDirect() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } else {
            openMobileDataSettings()
        }
    }

    fun openDefaultLauncherSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(fallback)
        }
    }

    fun openWallpaperPicker() {
        val app = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(app, "No wallpaper picker found", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWeatherApp() {
        refreshWeather()
        val app = getApplication<Application>()
        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("dynact://weather") }, // Google Weather
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("googleweather://") },
            Intent().apply { setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.staticpages.Paths") },
            pm.getLaunchIntentForPackage("com.google.android.apps.magellan"),
            pm.getLaunchIntentForPackage("com.accuweather.android"),
            pm.getLaunchIntentForPackage("com.weather.Weather"),
            pm.getLaunchIntentForPackage("com.samsung.android.weather"),
            pm.getLaunchIntentForPackage("com.miui.weather2"),
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://www.google.com/search?q=weather") } // Fallback to browser
        )

        for (intent in intents) {
            try {
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    fun openDigitalWellbeing() {
        val app = getApplication<Application>()
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.home.TopLevelSettingsActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            // Fallback for non-pixel/Google devices
            try {
                val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    fun refreshWeather(force: Boolean = false) {
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            if (!force && !settings.showWeatherInfo) return@launch
            
            val now = System.currentTimeMillis()
            if (!force && (now - settings.lastWeatherFetchTime < WEATHER_REFRESH_INTERVAL)) return@launch

            prefs.setLastWeatherFetchTime(now)
            storeBridge.getCurrentLocation(
                callback = { lat: Double, lon: Double -> fetchWeather(lat, lon) },
                fallback = { fetchWeather() }
            )
        }
    }

    private fun fetchWeather(lat: Double = 51.5074, lon: Double = 0.1278) {
        viewModelScope.launch {
            try {
                // Using Open-Meteo API - Free, reliable, and no API key required
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                
                val result = withContext(Dispatchers.IO) {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        throw Exception("HTTP $responseCode")
                    }
                }
                
                val json = Gson().fromJson(result, JsonObject::class.java)
                val current = json.getAsJsonObject("current_weather")
                
                if (current != null) {
                    val temp = current.get("temperature").asDouble
                    val weatherCode = current.get("weathercode").asInt
                    
                    _weatherTemp.value = "${temp.toInt()}°C"
                    _weatherCondition.value = mapWmoCode(weatherCode)
                }
            } catch (e: Exception) {
                Log.e("DotzWeather", "Failed to fetch weather: ${e.message}")
                // Clear the cache time so we can retry sooner on error
                prefs.setLastWeatherFetchTime(0L)
                if (_weatherTemp.value == null) {
                    _weatherTemp.value = "--°C"
                    _weatherCondition.value = "Offline"
                }
            }
        }
    }

    private fun mapWmoCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Cloudy"
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = pm.resolveActivity(intent, 0) // Check all, not just default
        if (res == null) return false
        
        val currentDefault = res.activityInfo.packageName
        val myPackage = getApplication<Application>().packageName
        
        // If it's the system resolver or settings, we are definitely NOT the default
        if (currentDefault == "android" || 
            currentDefault == "com.android.settings" || 
            currentDefault == "com.google.android.permissioncontroller" ||
            currentDefault == "com.android.internal.app.ResolverActivity") {
            return false
        }
        
        return currentDefault == myPackage
    }

    // ── Logic ────────────────────────────────────────────────────────────

    fun refreshState() {
        _refreshTrigger.value = Unit
        val app = getApplication<Application>()
        val componentName = ComponentName(app, DotzNotificationService::class.java)
        try {
            updateActiveController(mediaSessionManager.getActiveSessions(componentName))
        } catch (_: Exception) {}
    }

    fun refreshTimeline() {
        // Timeline is now computed reactively in the combine block
        refreshState()
    }

    // ── App Logic ─────────────────────────────────────────────────────────────

    private fun getAllAppStatsToday(): UsageStatsResult {
        val usm = getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = mutableMapOf<String, Long>()
        val countMap = mutableMapOf<String, Int>()
        
        var totalScreenTime = 0L
        var unlockCount = 0
        var notificationsReceived = 0
        val myPackage = getApplication<Application>().packageName

        try {
            // 1. Get Per-App Foreground Time
            val aggregateStats = usm.queryAndAggregateUsageStats(startTime, endTime)
            aggregateStats?.forEach { (pkg, stat) ->
                val time = stat.totalTimeInForeground
                if (time > 0) {
                    statsMap[pkg] = time
                }
            }

            // 2. Get Accurate Total Screen Time, Launch Counts, Unlocks & Notifications from Events
            val events = usm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            
            var currentForegroundPackage: String? = null
            var foregroundStartTime = 0L

            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (pm.getLaunchIntentForPackage(pkg) != null) {
                            countMap[pkg] = (countMap[pkg] ?: 0) + 1
                        }
                        
                        val isCountedApp = pkg != myPackage && 
                                          pkg != "com.android.systemui" && 
                                          pm.getLaunchIntentForPackage(pkg) != null

                        if (isCountedApp) {
                            if (currentForegroundPackage == null) {
                                foregroundStartTime = event.timeStamp
                            }
                            currentForegroundPackage = pkg
                        } else {
                            if (currentForegroundPackage != null) {
                                totalScreenTime += (event.timeStamp - foregroundStartTime)
                                currentForegroundPackage = null
                            }
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (currentForegroundPackage == pkg) {
                            totalScreenTime += (event.timeStamp - foregroundStartTime)
                            currentForegroundPackage = null
                        }
                    }
                    16 -> { // KEYGUARD_DISMISSED
                        unlockCount++
                    }
                    12 -> { // NOTIFICATION_INTERRUPTION
                        notificationsReceived++
                    }
                }
            }
            
            if (currentForegroundPackage != null && currentForegroundPackage != myPackage) {
                totalScreenTime += (endTime - foregroundStartTime)
            }

        } catch (_: Exception) {}

        val appStats = (statsMap.keys + countMap.keys).associateWith { pkg ->
            val totalMillis = statsMap[pkg] ?: 0L
            val timeStr = if (totalMillis > 60000) formatDuration(totalMillis) else null
            val count = countMap[pkg] ?: 0
            timeStr to count
        }
        
        val elapsedToday = endTime - startTime
        val finalTotal = totalScreenTime.coerceIn(0L, elapsedToday)
        
        return UsageStatsResult(appStats, finalTotal, unlockCount, notificationsReceived)
    }

    private fun buildTilesFast(
        defaults: List<AppTile>,
        settings: DotzSettings,
        notifCounts: Map<String, Int>,
        allUsage: Map<String, Pair<String?, Int>>
    ): List<AppTile> {
        return defaults.map { tile ->
            val pkg = settings.tileOverrides[tile.tileId] ?: resolvePackage(tile.packageName)
            val label = settings.tileLabels[tile.tileId] ?: tile.label
            val installed = isInstalled(pkg) || pkg == getApplication<Application>().packageName
            
            val stats = allUsage[pkg]
            val usageTime = stats?.component1()
            val launchCount = stats?.component2() ?: 0
            
            val count = if (settings.showNotificationDots) {
                val raw = notifCounts[pkg] ?: -1
                val isNumericAllowed = tile.tileId in 0..2 || DefaultApps.numericBadgePackages.contains(pkg)
                
                if (raw > 0 && settings.showNumericalCounts && isNumericAllowed) raw
                else if (raw >= 0) 0
                else -1
            } else {
                -1
            }
            tile.copy(
                packageName = pkg, 
                label = label, 
                badgeCount = count, 
                isInstalled = installed,
                usageTime = usageTime,
                launchCount = launchCount
            )
        }
    }

    private fun buildTiles(
        defaults: List<AppTile>,
        settings: DotzSettings,
        notifCounts: Map<String, Int>
    ): List<AppTile> {
        val usageResult = if (hasUsageStatsPermission()) getAllAppStatsToday() else UsageStatsResult(emptyMap(), 0L, 0, 0)
        val allStats = usageResult.appStats
        
        return defaults.map { tile ->
            val pkg = settings.tileOverrides[tile.tileId] ?: resolvePackage(tile.packageName)
            val label = settings.tileLabels[tile.tileId] ?: tile.label
            val installed = isInstalled(pkg) || pkg == getApplication<Application>().packageName
            
            val stats = allStats[pkg]
            val usageTime = stats?.component1()
            val launchCount = stats?.component2() ?: 0
            
            val count = if (settings.showNotificationDots) {
                val raw = notifCounts[pkg] ?: -1
                // Show numerical counts for Dialer (0), WhatsApp (1), and Messaging (2) tiles, 
                // OR if the package is in the explicit numeric list.
                val isNumericAllowed = tile.tileId in 0..2 || DefaultApps.numericBadgePackages.contains(pkg)
                
                if (raw > 0 && settings.showNumericalCounts && isNumericAllowed) {
                    raw
                } else if (raw >= 0) {
                    0
                } else {
                    -1
                }
            } else {
                -1
            }
            tile.copy(
                packageName = pkg, 
                label = label, 
                badgeCount = count, 
                isInstalled = installed,
                usageTime = usageTime,
                launchCount = launchCount
            )
        }
    }

    private fun resolvePackage(preferred: String): String {
        if (isInstalled(preferred)) return preferred
        DefaultApps.packageFallbacks[preferred]?.forEach { fallback ->
            if (isInstalled(fallback)) return fallback
        }
        return preferred
    }

    private fun isInstalled(pkg: String): Boolean {
        installedCache[pkg]?.let { return it }
        val result = try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
        installedCache[pkg] = result
        return result
    }

    fun moveTile(fromId: Int, toId: Int) {
        viewModelScope.launch {
            val currentOrder = uiState.value.settings.tileOrder.toMutableList()
            val fromIndex = currentOrder.indexOf(fromId)
            val toIndex = currentOrder.indexOf(toId)
            if (fromIndex != -1 && toIndex != -1) {
                val item = currentOrder.removeAt(fromIndex)
                currentOrder.add(toIndex, item)
                prefs.setTileOrder(currentOrder)
            }
        }
    }

    fun setShowNotificationDots(value: Boolean) = viewModelScope.launch {
        prefs.setShowNotificationDots(value)
    }

    fun setShowNumericalCounts(value: Boolean) = viewModelScope.launch {
        prefs.setShowNumericalCounts(value)
    }

    fun setNotificationFilterEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setNotificationFilterEnabled(value)
    }

    fun setIsLightMode(value: Boolean) = viewModelScope.launch {
        prefs.setIsLightMode(value)
        iconCache.clearCache() // Icons might need re-rendering for light mode if grayscale is on
    }

    fun setGrayscaleMode(value: Boolean) = viewModelScope.launch {
        prefs.setGrayscaleMode(value)
        iconCache.clearCache()
    }

    fun setAutoGrayscale(value: Boolean) = viewModelScope.launch {
        prefs.setAutoGrayscale(value)
        iconCache.clearCache()
    }

    fun setVerticalScrolling(value: Boolean) = viewModelScope.launch {
        prefs.setVerticalScrolling(value)
    }

    fun setEnableExtraPage(value: Boolean) = viewModelScope.launch {
        prefs.setEnableExtraPage(value)
    }

    fun setExtraTileCount(value: Int) = viewModelScope.launch {
        prefs.setExtraTileCount(value)
    }

    fun setShowWeatherInfo(value: Boolean) = viewModelScope.launch {
        prefs.setShowWeatherInfo(value)
        if (value) refreshWeather(force = true)
    }

    fun setShowMindfulUsage(value: Boolean) = viewModelScope.launch {
        prefs.setShowMindfulUsage(value)
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplication<Application>().packageName)
        } else {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getApplication<Application>().packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun setEnableFastlane(value: Boolean) = viewModelScope.launch {
        prefs.setEnableFastlane(value)
    }

    fun setHomeHeaderMode(value: String) = viewModelScope.launch {
        prefs.setHomeHeaderMode(value)
    }

    fun setTileTransparency(value: Float) = viewModelScope.launch {
        prefs.setTileTransparency(value)
    }

    fun setLayoutStyle(value: String) = viewModelScope.launch {
        prefs.setLayoutStyle(value)
    }

    fun incrementAppDrawerCount() = viewModelScope.launch {
        prefs.incrementAppDrawerOpenCount()
    }

    fun updateTileOverride(tileId: Int, pkg: String, label: String) = viewModelScope.launch {
        prefs.setTileOverride(tileId, pkg, label)
    }

    fun createProfile(name: String) = viewModelScope.launch {
        val newId = prefs.createProfile(name)
        prefs.switchProfile(newId)
    }

    fun deleteProfile(id: String) = viewModelScope.launch {
        prefs.deleteProfile(id)
    }

    fun switchProfile(id: String) = viewModelScope.launch {
        prefs.switchProfile(id)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        when (mode) {
                ThemeMode.LIGHT -> {
                    prefs.setIsLightMode(true)
                    prefs.setUseCircadianTheming(false)
                    prefs.setShowWallpaper(false)
                    prefs.setThemeId("default")
                }
                ThemeMode.DARK -> {
                    prefs.setIsLightMode(false)
                    prefs.setUseCircadianTheming(false)
                    prefs.setShowWallpaper(false)
                    prefs.setThemeId("default")
                }
            ThemeMode.CIRCADIAN -> {
                prefs.setIsLightMode(false)
                prefs.setUseCircadianTheming(true)
                prefs.setShowWallpaper(false)
            }
            ThemeMode.TRANSPARENT -> {
                prefs.setIsLightMode(false)
                prefs.setUseCircadianTheming(false)
                prefs.setShowWallpaper(true)
            }
        }
    }

    fun setUseLiquidGlass(value: Boolean) = viewModelScope.launch {
        prefs.setUseLiquidGlass(value)
    }

    fun setIconPackPackage(value: String?) = viewModelScope.launch {
        prefs.setIconPackPackage(value)
        iconCache.clearCache()
    }

    fun setPremium(value: Boolean) = viewModelScope.launch {
        prefs.setPremium(value)
    }

    val monthlyPrice = storeBridge.monthlyPrice
    val yearlyPrice = storeBridge.yearlyPrice
    val lifetimePrice = storeBridge.lifetimePrice

    fun buyProduct(activity: Activity, productId: String) {
        storeBridge.startBillingFlow(activity, productId)
    }

    fun acceptAppDisclosure() = viewModelScope.launch {
        prefs.setHasAcceptedAppDisclosure(true)
    }

    fun setOnboardingSeen() = viewModelScope.launch {
        prefs.setHasSeenOnboarding(true)
    }

    fun acknowledgeSponsoredAd() = viewModelScope.launch {
        prefs.setLastSponsoredShowTime(System.currentTimeMillis())
    }

    fun redeemPromoCode(code: String): Boolean {
        return if (code.trim().uppercase() == "DOTZPRO2026") {
            viewModelScope.launch {
                prefs.setPremium(true)
            }
            true
        } else {
            false
        }
    }

    suspend fun exportSettings(): String {
        return prefs.exportSettings()
    }

    suspend fun importSettings(json: String): Boolean {
        return try {
            val success = prefs.importSettings(json)
            if (success) {
                refreshState()
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    fun getInstalledApps(): List<DrawerApp> {
        val cached = _installedAppsCache.value
        if (cached.isNotEmpty()) return cached
        
        // Simple synchronous fallback (no usage stats) to ensure list isn't empty
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0)
            .map { 
                val pkg = it.activityInfo.packageName
                val label = it.loadLabel(pm).toString()
                DrawerApp(pkg, label, null, 0)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }

    fun getInstalledAppsForTile(tileId: Int, currentProfileId: String): List<DrawerApp> {
        val allApps = getInstalledApps()
        
        // If it's a custom profile, allow all apps for all tiles
        if (currentProfileId != "default") {
            return allApps
        }
        
        // Smart suggestions only for the Default profile
        val filtered = when (tileId) {
            0 -> filterByIntent(allApps, Intent(Intent.ACTION_DIAL)) + 
                 filterByIntent(allApps, Intent(Intent.ACTION_VIEW).apply { data = "tel:".toUri() }) +
                 filterByKeywords(allApps, listOf("phone", "dialer", "call", "contact"))
            1 -> filterByKeywords(allApps, listOf("chat", "whatsapp", "telegram", "signal", "discord", "viber", "messenger", "social", "facebook", "insta", "whatsapp"))
            2 -> filterByIntent(allApps, Intent(Intent.ACTION_SENDTO).apply { data = "smsto:".toUri() }) +
                 filterByKeywords(allApps, listOf("messag", "sms", "mms", "text"))
            3 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) }) +
                 filterByKeywords(allApps, listOf("map", "navig", "gps", "waze", "uber", "lyft", "traffic", "location"))
            4 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }) +
                 filterByIntent(allApps, Intent("android.intent.action.MUSIC_PLAYER")) +
                 filterByKeywords(allApps, listOf("music", "audio", "player", "spotify", "sound", "radio", "podcast", "yt music", "youtube music", "wynk", "jio saavn", "gaana"))
            5 -> filterByKeywords(allApps, listOf("pay", "wallet", "bank", "finance", "cash", "money", "card", "crypto", "binance", "paypal", "gpay", "phonepe", "phonepay", "paytm", "bhim", "yono", "hdfc", "icici", "sbi", "axis", "kotak", "pnb", "bob", "canara"))
            6 -> filterByIntent(allApps, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) +
                 filterByKeywords(allApps, listOf("camera", "cam", "lens", "gallery", "photo"))
            7 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }) +
                 filterByKeywords(allApps, listOf("calc"))
            8 -> filterByKeywords(allApps, listOf("clock", "alarm", "timer", "watch"))
            9 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }) +
                 filterByKeywords(allApps, listOf("calen"))
            10 -> filterByKeywords(allApps, listOf("note", "keep", "memo", "todo", "sticky", "journal", "list", "writ"))
            11 -> filterByKeywords(allApps, listOf("settings", "config", "manage", "setup", "launcher", "dotz"))
            else -> return allApps // Tiles 12+ (Extra Page) can select any app
        }.asSequence().distinctBy { it.packageName }.sortedBy { it.label }.toList()

        // If for some reason the filtered list is empty (e.g., no matching app), 
        // allow all apps so the user isn't stuck with an unassignable tile.
        return if (filtered.isEmpty()) allApps else filtered
    }

    private fun filterByIntent(apps: List<DrawerApp>, intent: Intent): List<DrawerApp> {
        val resolved = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
        return apps.filter { resolved.contains(it.packageName) }
    }

    private fun filterByKeywords(apps: List<DrawerApp>, keywords: List<String>): List<DrawerApp> {
        return apps.filter { app ->
            keywords.any { kw -> 
                app.label.contains(kw, ignoreCase = true) || app.packageName.contains(kw, ignoreCase = true)
            }
        }
    }

    fun getInstalledIconPacks(): List<Pair<String, String>> {
        val iconPacks = mutableListOf<Pair<String, String>>()
        val intent = Intent("com.novalauncher.THEME")
        val infos = pm.queryIntentActivities(intent, 0)
        for (info in infos) {
            iconPacks.add(info.activityInfo.packageName to info.loadLabel(pm).toString())
        }

        val adwIntent = Intent("org.adw.launcher.THEMES")
        val adwInfos = pm.queryIntentActivities(adwIntent, 0)
        for (info in adwInfos) {
            val pkg = info.activityInfo.packageName
            if (iconPacks.none { it.component1() == pkg }) {
                iconPacks.add(pkg to info.loadLabel(pm).toString())
            }
        }

        return iconPacks.sortedBy { it.second }
    }
}
