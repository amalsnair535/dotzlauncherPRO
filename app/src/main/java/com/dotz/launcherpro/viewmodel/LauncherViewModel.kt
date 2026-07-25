package com.dotz.launcherpro.viewmodel

import android.app.Activity
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotz.launcherpro.data.*
import com.dotz.launcherpro.manager.SponsoredContentManager
import com.dotz.launcherpro.manager.UsageManager
import com.dotz.launcherpro.manager.UsageStatsResult
import com.dotz.launcherpro.services.DotzNotificationService
import com.dotz.launcherpro.services.NotificationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

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
    val ringerMode: Int = 2,
    val isMobileDataEnabled: Boolean = true,
    val isDefaultLauncher: Boolean = false,
    val weatherTemp: String? = null,
    val weatherCondition: String? = null,
    val weatherFeelsLike: String? = null,
    val weatherSummary: String? = null,
    val weatherAqi: String? = null,
    val weatherAqiLabel: String? = null,
    val weatherLow: String? = null,
    val weatherHigh: String? = null,
    val activeNotifications: List<NotificationItem> = emptyList(),
    val blockedNotificationsCount: Int = 0,
    val nowPlayingTitle: String = "Not Playing",
    val nowPlayingArtist: String = "",
    val nowPlayingAlbum: String = "",
    val nowPlayingPackage: String? = null,
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
    val isTimelineVisible: Boolean = false,
    val unlockCount: Int = 0,
    val notificationsReceivedToday: Int = 0,
    val totalAppOpens: Int = 0,
    val focusScore: Int = 100,
    val focusScoreHistory: List<Pair<String, Int>> = emptyList(),
    val focusSoundPlaying: String? = null,
    val allApps: List<DrawerApp> = emptyList(),
    val topApps: List<DrawerApp> = emptyList(),
    val timelineItems: List<TimelineItem> = emptyList(),
    val upcomingEvents: List<com.dotz.launcherpro.manager.CalendarEvent> = emptyList(),
    val nextAlarm: String? = null,
    val nativeAd: com.google.android.gms.ads.nativead.NativeAd? = null,
    val isAdLoading: Boolean = false,
    val isStoreConnected: Boolean = false,
    val isLoaded: Boolean = false,
    val ultraFocusRemainingMillis: Long = 0,
    val hasUsageStatsPermission: Boolean = false,
    val currentThemeMode: ThemeMode = ThemeMode.DARK,
    val installedIconPacks: List<Pair<String, String>> = emptyList(),
    val showUltraFocusExitReason: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, CIRCADIAN, TRANSPARENT }

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val dotzApp = application as com.dotz.launcherpro.DotzApp
    private val prefs = dotzApp.prefsRepository
    private val storeBridge = dotzApp.storeBridge
    private val usageManager = UsageManager(application)
    private val calendarManager = com.dotz.launcherpro.manager.CalendarManager(application)
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    private val systemStateManager = dotzApp.systemStateManager
    private val weatherManager = dotzApp.weatherManager
    private val mediaManager = dotzApp.mediaManager
    private val adsManager = dotzApp.adsManager
    val iconCache = IconCacheManager(application)
    private val pm: PackageManager = application.packageManager
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var musicRevertJob: Job? = null
    private var preMusicHeaderMode: String? = null

    private val installedCache = mutableMapOf<String, Boolean>()
    private var cachedIsDefault = false
    private var sessionStartTime = System.currentTimeMillis()
    
    private var lastTiles: List<AppTile>? = null
    private var lastTilesDeps: Any? = null
    
    private var lastTimeline: List<TimelineItem>? = null
    private var lastTimelineDeps: Any? = null
    
    private var lastTopApps: List<DrawerApp>? = null
    private var lastTopAppsDeps: Any? = null
    
    fun loadRewardedAd(onAdLoaded: () -> Unit = {}) {
        adsManager.loadRewardedAd(onAdLoaded)
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        adsManager.showRewardedAd(activity, onRewardEarned)
    }

    fun loadNativeAd() {
        adsManager.loadNativeAd()
    }

    fun grant24HourPremium() = viewModelScope.launch {
        val expiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        prefs.setPremiumExpiry(expiry)
    }

    private val _uiState = MutableStateFlow(LauncherUiState(isDefaultLauncher = isDefaultLauncher()))
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _timerTicker = MutableStateFlow(System.currentTimeMillis())
    private val _usageStats = MutableStateFlow(UsageStatsResult(emptyMap(), 0L, 0, 0))
    private val _installedAppsCache = MutableStateFlow<List<DrawerApp>>(emptyList())
    private val _installedIconPacks = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private val _upcomingEvents = MutableStateFlow<List<com.dotz.launcherpro.manager.CalendarEvent>>(emptyList())
    private val _nextAlarm = MutableStateFlow<String?>(null)
    private val _showUltraFocusExitReason = MutableStateFlow(false)

    private val _isTimelineVisible = MutableStateFlow(false)
    fun setTimelineVisible(visible: Boolean) {
        _isTimelineVisible.value = visible
    }

    private val _isUiVisible = MutableStateFlow(true)
    fun setUiVisible(visible: Boolean) {
        _isUiVisible.value = visible
        if (visible) {
            _timerTicker.value = System.currentTimeMillis()
            refreshState()
        }
    }

    private val _currentInnerPage = MutableStateFlow(0)
    val currentInnerPage: StateFlow<Int> = _currentInnerPage.asStateFlow()
    fun setInnerPage(index: Int) {
        _currentInnerPage.value = index
    }

    fun startUltraFocusSession(minutes: Int) = viewModelScope.launch {
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.setUltraFocusEndTime(endTime)
        
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (_: Exception) {}
    }

    fun endUltraFocusSession(reason: String? = null) = viewModelScope.launch {
        prefs.setUltraFocusEndTime(0L)
        _showUltraFocusExitReason.value = false
        
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } catch (_: Exception) {}
    }

    fun setUltraFocusApps(packages: List<String>) = viewModelScope.launch {
        prefs.setUltraFocusApps(packages)
    }

    fun requestUltraFocusExit() {
        _showUltraFocusExitReason.value = true
    }

    fun cancelUltraFocusExit() {
        _showUltraFocusExitReason.value = false
    }

    private fun refreshIsDefault() {
        cachedIsDefault = isDefaultLauncher()
    }

    init {
        // Update streak and reset focus time if it's a new day
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val now = System.currentTimeMillis()
            val lastDate = settings.lastUsedDate
            
            val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
            val calendarLast = Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = calendarNow[Calendar.DAY_OF_YEAR] == calendarLast[Calendar.DAY_OF_YEAR] &&
                           calendarNow[Calendar.YEAR] == calendarLast[Calendar.YEAR]
            
            val isNextDay = !isSameDay && (now - lastDate < 48 * 60 * 60 * 1000)
            
            val newStreak = if (isNextDay) settings.focusStreak + 1 else if (isSameDay) settings.focusStreak else 1
            val newFocusTime = if (isSameDay) settings.focusTimeToday else 0L
            
            prefs.updateFocusStats(newStreak, now, newFocusTime, resetDrawerCount = !isSameDay)
            if (!isSameDay) {
                _refreshTrigger.emit(Unit)
            }
        }

        // Periodic update of focus time
        viewModelScope.launch {
            while (true) {
                delay(60000)
                updateSessionTime()
            }
        }

        // Automatic Header Switching (Music <-> Focus)
        viewModelScope.launch {
            mediaManager.playbackState.collectLatest { (isPlaying, _, _) ->
                val settings = prefs.settingsFlow.first()
                if (isPlaying) {
                    musicRevertJob?.cancel()
                    if (settings.homeHeaderMode != "music") {
                        preMusicHeaderMode = settings.homeHeaderMode
                        prefs.setHomeHeaderMode("music")
                    }
                } else {
                    // If music was playing (mode is music) and we have a mode to revert to
                    if (settings.homeHeaderMode == "music" && preMusicHeaderMode != null) {
                        musicRevertJob?.cancel()
                        musicRevertJob = launch {
                            delay(30000) // 30 seconds wait
                            preMusicHeaderMode?.let { revertMode ->
                                prefs.setHomeHeaderMode(revertMode)
                                preMusicHeaderMode = null
                            }
                        }
                    }
                }
            }
        }

        refreshIsDefault()
        
        // Periodic background checks
        viewModelScope.launch {
            while (true) {
                delay(500)
                val settings = prefs.settingsFlow.first()
                val ultraFocusActive = settings.ultraFocusEndTime > System.currentTimeMillis()
                
                if (_isUiVisible.value || ultraFocusActive) {
                    _timerTicker.value = System.currentTimeMillis()
                }
                
                if (System.currentTimeMillis() % 60000 < 1000) {
                    refreshIsDefault()
                }
            }
        }

        // Heavy data updates (Apps & Usage)
        viewModelScope.launch(Dispatchers.IO) {
            _refreshTrigger.collect {
                installedCache.clear()
                val usage = if (usageManager.hasUsageStatsPermission()) usageManager.getAllAppStatsToday() else UsageStatsResult(emptyMap(), 0L, 0, 0)
                _usageStats.value = usage

                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val apps = pm.queryIntentActivities(intent, 0)
                    .map {
                        val pkg = it.activityInfo.packageName
                        val label = it.loadLabel(pm).toString()
                        val stats = usage.appStats[pkg]
                        DrawerApp(pkg, label, stats?.component1(), stats?.component2() ?: 0)
                    }
                    .distinctBy { it.packageName }
                _installedAppsCache.value = apps
                
                // Auto-resolve tiles for OEM apps on first run or when apps change
                autoResolveTiles()
            }
        }

        // Events & Alarm Updates
        viewModelScope.launch(Dispatchers.IO) {
            _refreshTrigger.collect {
                _upcomingEvents.value = calendarManager.getUpcomingEvents()
                _nextAlarm.value = Settings.System.getString(getApplication<Application>().contentResolver, Settings.System.NEXT_ALARM_FORMATTED)
            }
        }

        // Focus Score History tracking
        viewModelScope.launch {
            uiState.map { it.focusScore }.distinctUntilChanged().collect { calculatedScore ->
                val settings = prefs.settingsFlow.first()
                val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (settings.focusScoreHistory[isoDate] != calculatedScore) {
                    prefs.updateFocusScoreHistory(isoDate, calculatedScore)
                }
            }
        }

        // Main UI State combination
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                DotzNotificationService.notificationCounts,
                systemStateManager.batteryLevel,
                systemStateManager.networkStatus,
                systemStateManager.isWifiEnabled,
                systemStateManager.isBluetoothEnabled,
                systemStateManager.isSilentMode,
                systemStateManager.isTorchOn,
                systemStateManager.isAirplaneModeOn,
                systemStateManager.isDarkModeOn,
                systemStateManager.ringerMode,
                systemStateManager.isMobileDataEnabled,
                weatherManager.weatherTemp,
                weatherManager.weatherCondition,
                DotzNotificationService.notifications,
                DotzNotificationService.blockedCount,
                mediaManager.nowPlaying,
                mediaManager.playbackState,
                mediaManager.nowPlayingPackage,
                mediaManager.lastPositionUpdateTime,
                storeBridge.isPremium,
                _usageStats,
                _installedAppsCache,
                _isTimelineVisible,
                adsManager.nativeAdFlow,
                adsManager.isAdLoading,
                _timerTicker,
                _installedIconPacks,
                _upcomingEvents,
                _nextAlarm,
                weatherManager.weatherFeelsLike,
                weatherManager.weatherSummary,
                weatherManager.weatherAqi,
                weatherManager.weatherAqiLabel,
                weatherManager.weatherLow,
                weatherManager.weatherHigh,
                storeBridge.isStoreConnected,
                _showUltraFocusExitReason
            ) { args: Array<Any?> ->
                val settings = args[0] as DotzSettings
                val notifCounts = args[1] as Map<String, Int>
                val battery = args[2] as Int
                val network = args[3] as String
                val wifi = args[4] as Boolean
                val bt = args[5] as Boolean
                val silent = args[6] as Boolean
                val torch = args[7] as Boolean
                val airplane = args[8] as Boolean
                val dark = args[9] as Boolean
                val ringer = args[10] as Int
                val mobileData = args[11] as Boolean
                val temp = args[12] as String?
                val condition = args[13] as String?
                val notifications = args[14] as List<NotificationItem>
                val blocked = args[15] as Int
                val nowPlaying = args[16] as Triple<String, String, String>
                val playback = args[17] as Triple<Boolean, Long, Long>
                val nowPlayingPackage = args[18] as String?
                val lastPositionUpdate = args[19] as Long
                val isPremiumStatus = args[20] as Boolean
                val usageResult = args[21] as UsageStatsResult
                val allApps = args[22] as List<DrawerApp>
                val timelineVisible = args[23] as Boolean
                val nativeAd = args[24] as com.google.android.gms.ads.nativead.NativeAd?
                val isAdLoading = args[25] as Boolean
                val tickTime = args[26] as Long
                val iconPacks = args[27] as List<Pair<String, String>>
                val upcomingEvents = args[28] as List<com.dotz.launcherpro.manager.CalendarEvent>
                val alarm = args[29] as String?
                val feelsLike = args[30] as String?
                val summary = args[31] as String?
                val aqi = args[32] as String?
                val aqiLabel = args[33] as String?
                val low = args[34] as String?
                val high = args[35] as String?
                val storeConnected = args[36] as Boolean
                val showExitReason = args[37] as Boolean

                val isPlaying = playback.first
                val basePosition = playback.second
                val duration = playback.third
                val interpolatedPosition = if (isPlaying && duration > 0) {
                    val elapsed = tickTime - lastPositionUpdate
                    (basePosition + elapsed.coerceAtLeast(0L)).coerceAtMost(duration)
                } else basePosition

                val allUsage = usageResult.appStats
                val totalTimeMillis = usageResult.totalScreenTime
                
                val startOfDayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val elapsedToday = System.currentTimeMillis() - startOfDayCalendar.timeInMillis
                val focusTimeTodayMillis = (elapsedToday - totalTimeMillis).coerceAtLeast(0L)

                val isCurrentlyPremium = settings.isPremium || isPremiumStatus || (settings.premiumExpiry > System.currentTimeMillis())

                val currentTilesDeps = Triple(settings.tileOverrides, notifCounts, allUsage)
                val allTilesUnordered = if (currentTilesDeps == lastTilesDeps && lastTiles != null) {
                    lastTiles!!
                } else {
                    val built = buildTilesFast(DefaultApps.allDefaults, settings, notifCounts, allUsage)
                    lastTiles = built
                    lastTilesDeps = currentTilesDeps
                    built
                }
                
                val allTiles = settings.tileOrder.mapNotNull { id ->
                    allTilesUnordered.find { it.tileId == id }
                }

                val unlockPenalty = ((usageResult.unlockCount - 15).coerceAtLeast(0)) * 2
                val minutesUsed = totalTimeMillis / 60000
                val screenTimePenalty = (minutesUsed / 4).toInt()
                val emergencyPenalty = settings.emergencyDrawerOpens * 15
                val calculatedScore = (100 - unlockPenalty - screenTimePenalty - emergencyPenalty).coerceIn(0, 100)
                
                val historyList = settings.focusScoreHistory.toList().sortedBy { it.first }

                val p0 = allTiles.take(6)
                val p1 = allTiles.drop(6).take(6)
                val p2 = if (settings.enableExtraPage) allTiles.drop(12).take(settings.extraTileCount) else emptyList()

                val ultraFocusTiles = settings.ultraFocusAppPackages.map { pkg ->
                    val label = allApps.find { it.packageName == pkg }?.label ?: pkg.substringAfterLast('.').uppercase()
                    AppTile(-1, pkg, label, Icons.Default.Apps)
                }

                val ultraFocusRemaining = (settings.ultraFocusEndTime - System.currentTimeMillis()).coerceAtLeast(0L)

                val themeMode = when {
                    settings.showWallpaper -> ThemeMode.TRANSPARENT
                    settings.useCircadianTheming -> ThemeMode.CIRCADIAN
                    settings.isLightMode -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isNightTime = hour >= 22 || hour < 6
                val effectiveGrayscale = settings.grayscaleMode || (settings.autoGrayscale && isNightTime)

                val currentLayoutStyle = if (settings.ultraFocusEndTime > System.currentTimeMillis()) "ultra_focus" else settings.layoutStyle

                val effectiveSettings = if (isCurrentlyPremium) {
                    settings.copy(grayscaleMode = effectiveGrayscale, isPremium = true, layoutStyle = currentLayoutStyle)
                } else {
                    settings.copy(
                        tileTransparency = 1.0f,
                        layoutStyle = if (settings.ultraFocusEndTime > System.currentTimeMillis()) "ultra_focus" else "classic",
                        showWallpaper = false,
                        useCircadianTheming = false,
                        grayscaleMode = effectiveGrayscale,
                        isPremium = false
                    )
                }

                // Timeline logic (Memoized)
                val currentTimelineDeps = Pair(notifications, nowPlaying.first)
                val finalTimeline = if (currentTimelineDeps == lastTimelineDeps && lastTimeline != null) {
                    lastTimeline!!
                } else {
                    val timeline = mutableListOf<TimelineItem>()
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

                    if (nowPlaying.first != "Not Playing" && nowPlaying.first.isNotBlank()) {
                        timeline.add(TimelineItem(
                            id = "music_current",
                            type = TimelineType.MUSIC,
                            title = nowPlaying.first,
                            subtitle = nowPlaying.second,
                            timestamp = System.currentTimeMillis(), // Keep it at the top while playing/active
                            packageName = nowPlayingPackage
                        ))
                    }
                    val result = timeline.sortedByDescending { it.timestamp }
                    lastTimeline = result
                    lastTimelineDeps = currentTimelineDeps
                    result
                }

                val currentTopAppsDeps = allApps
                val topApps = if (currentTopAppsDeps == lastTopAppsDeps && lastTopApps != null) {
                    lastTopApps!!
                } else {
                    val result = allApps.sortedByDescending { it.launchCount }.take(5)
                    lastTopApps = result
                    lastTopAppsDeps = currentTopAppsDeps
                    result
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
                    ringerMode = ringer,
                    isMobileDataEnabled = mobileData,
                    isDefaultLauncher = cachedIsDefault,
                    weatherTemp = temp,
                    weatherCondition = condition,
                    weatherFeelsLike = feelsLike,
                    weatherSummary = summary,
                    weatherAqi = aqi,
                    weatherAqiLabel = aqiLabel,
                    weatherLow = low,
                    weatherHigh = high,
                    activeNotifications = notifications,
                    blockedNotificationsCount = blocked,
                    nowPlayingTitle = nowPlaying.first,
                    nowPlayingArtist = nowPlaying.second,
                    nowPlayingAlbum = nowPlaying.third,
                    nowPlayingPackage = nowPlayingPackage,
                    isPlaying = isPlaying,
                    playbackPosition = interpolatedPosition,
                    playbackDuration = duration,
                    focusTimeToday = formatDuration(focusTimeTodayMillis),
                    focusTimeMillis = focusTimeTodayMillis,
                    focusStreak = settings.focusStreak,
                    isUpdateAvailable = false,
                    isPremium = isCurrentlyPremium,
                    isUpgradeAvailable = storeBridge.isUpgradeAvailable,
                    isLiteVersion = storeBridge.isLiteVersion,
                    isTimelineVisible = timelineVisible,
                    unlockCount = usageResult.unlockCount,
                    notificationsReceivedToday = usageResult.notificationsReceived,
                    totalAppOpens = usageResult.totalAppOpens,
                    focusScore = calculatedScore,
                    focusScoreHistory = historyList,
                    allApps = allApps,
                    topApps = topApps,
                    timelineItems = finalTimeline,
                    upcomingEvents = upcomingEvents,
                    nextAlarm = alarm,
                    nativeAd = nativeAd,
                    isAdLoading = isAdLoading,
                    isStoreConnected = storeConnected,
                    isLoaded = true,
                    ultraFocusRemainingMillis = ultraFocusRemaining,
                    hasUsageStatsPermission = usageManager.hasUsageStatsPermission(),
                    currentThemeMode = themeMode,
                    installedIconPacks = iconPacks,
                    showUltraFocusExitReason = showExitReason
                )
            }.collect { 
                _uiState.value = it
            }
        }

        refreshState()
    }

    fun downloadUpdate(url: String) {
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

    private fun updateSessionTime() {
        val now = System.currentTimeMillis()
        val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val lastDate = settings.lastUsedDate
            val calendarLast = Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = calendarNow[Calendar.DAY_OF_YEAR] == calendarLast[Calendar.DAY_OF_YEAR] &&
                           calendarNow[Calendar.YEAR] == calendarLast[Calendar.YEAR]
            
            if (isSameDay) {
                prefs.updateFocusStats(settings.focusStreak, now, settings.focusTimeToday)
            } else {
                val isNextDay = (now - lastDate < 48 * 60 * 60 * 1000)
                val newStreak = if (isNextDay) settings.focusStreak + 1 else 1
                prefs.updateFocusStats(newStreak, now, 0, resetDrawerCount = true)
                _refreshTrigger.emit(Unit)
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

    override fun onCleared() {
        super.onCleared()
        systemStateManager.stop()
        mediaManager.stop()
        adsManager.destroy()
    }

    // ── System Toggles ────────────────────────────────────────────────────────

    fun toggleWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleWifiDirect() {
        systemStateManager.isWifiEnabled.value.let { isEnabled ->
             // Direct toggle logic? SystemStateManager doesn't have it yet.
             // I'll keep it in VM for now and use wifiManager if possible.
        }
        // Actually, just open settings for safety on modern Android
        toggleWifi()
    }

    fun toggleBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleBluetoothDirect() {
        toggleBluetooth()
    }

    fun toggleSilentMode() {
        val app = getApplication<Application>()
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (!notificationManager.isNotificationPolicyAccessGranted) {
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
        } catch (_: Exception) {}
    }

    fun toggleTorch() {
        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isNotEmpty()) {
                cameraManager.setTorchMode(cameraIds[0], !systemStateManager.isTorchOn.value)
            }
        } catch (_: Exception) {}
    }

    fun toggleAirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleAirplaneModeDirect() = toggleAirplaneMode()

    fun toggleDarkMode() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleDarkModeDirect() {
        setIsLightMode(!uiState.value.settings.isLightMode)
    }

    // ── Media Controls ────────────────────────────────────────────────────────

    fun mediaPlayPause() = mediaManager.playPause()
    fun mediaSkipNext() = mediaManager.skipNext()
    fun mediaSkipPrevious() = mediaManager.skipPrevious()

    fun launchApp(packageName: String?): Boolean {
        android.util.Log.d("LauncherViewModel", "launchApp requested for: $packageName")
        if (packageName == null || packageName.isBlank()) return false
        val app = getApplication<Application>()
        
        if (!packageName.contains(".") && packageName.any { it.isDigit() }) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            return true
        }

        val intent = app.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            return true
        }
        return false
    }

    fun sendReply(notificationKey: String, message: String) {
        DotzNotificationService.sendReply(notificationKey, message)
    }

    fun openMobileDataSettings() {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { getApplication<Application>().startActivity(intent) } catch (_: Exception) {}
    }

    fun toggleMobileDataDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } else {
            openMobileDataSettings()
        }
    }

    fun openDefaultLauncherSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { getApplication<Application>().startActivity(intent) } catch (_: Exception) {}
    }

    fun openWallpaperPicker() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { getApplication<Application>().startActivity(intent) } catch (_: Exception) {}
    }

    fun openWeatherApp() {
        weatherManager.refreshWeather()
        val app = getApplication<Application>()
        val intents = listOfNotNull(
            Intent(Intent.ACTION_VIEW, Uri.parse("dynact://weather")),
            pm.getLaunchIntentForPackage("com.google.android.apps.magellan"),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    fun openDigitalWellbeing() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.google.android.apps.wellbeing", "com.google.android.apps.wellbeing.home.TopLevelSettingsActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { getApplication<Application>().startActivity(intent) } catch (_: Exception) {}
    }

    fun refreshWeather(force: Boolean = false) {
        weatherManager.refreshWeather(force)
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val res = pm.resolveActivity(intent, 0)
        return res?.activityInfo?.packageName == getApplication<Application>().packageName
    }

    fun refreshState() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun refreshTimeline() = refreshState()

    private fun autoResolveTiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = prefs.settingsFlow.first()
            val allApps = _installedAppsCache.value
            if (allApps.isEmpty()) return@launch

            DefaultApps.allDefaults.take(12).forEach { defaultTile ->
                val tileId = defaultTile.tileId
                // Only auto-resolve if no manual override exists
                if (!settings.tileOverrides.containsKey(tileId)) {
                    val smartMatches = getInstalledAppsForTile(tileId, "default")
                    if (smartMatches.isNotEmpty()) {
                        val bestMatch = smartMatches.first()
                        // If the best match is different from the hardcoded default, auto-select it
                        if (bestMatch.packageName != defaultTile.packageName) {
                            Log.d("LauncherViewModel", "Auto-resolving tile $tileId to ${bestMatch.packageName}")
                            updateTileOverride(tileId, bestMatch.packageName, bestMatch.label.uppercase())
                        }
                    }
                }
            }
        }
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
            val installed = isInstalled(pkg) || pkg == dotzApp.packageName
            
            val stats = allUsage[pkg]
            val usageTime = stats?.component1()
            val launchCount = stats?.component2() ?: 0
            
            val count = if (settings.showNotificationDots) {
                val raw = notifCounts[pkg] ?: -1
                val isNumericAllowed = tile.tileId in 0..2 || DefaultApps.numericBadgePackages.contains(pkg)
                if (raw > 0 && settings.showNumericalCounts && isNumericAllowed) raw
                else if (raw >= 0) 0
                else -1
            } else -1

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

    fun setShowNotificationDots(value: Boolean) = viewModelScope.launch { prefs.setShowNotificationDots(value) }
    fun setShowNumericalCounts(value: Boolean) = viewModelScope.launch { prefs.setShowNumericalCounts(value) }
    fun setNotificationFilterEnabled(value: Boolean) = viewModelScope.launch { prefs.setNotificationFilterEnabled(value) }
    fun setIsLightMode(value: Boolean) = viewModelScope.launch { 
        prefs.setIsLightMode(value)
        iconCache.clearCache()
    }
    fun setGrayscaleMode(value: Boolean) = viewModelScope.launch { 
        prefs.setGrayscaleMode(value)
        iconCache.clearCache()
    }
    fun setAutoGrayscale(value: Boolean) = viewModelScope.launch { 
        prefs.setAutoGrayscale(value)
        iconCache.clearCache()
    }
    fun setVerticalScrolling(value: Boolean) = viewModelScope.launch { prefs.setVerticalScrolling(value) }
    fun setEnableExtraPage(value: Boolean) = viewModelScope.launch { prefs.setEnableExtraPage(value) }
    fun setExtraTileCount(value: Int) = viewModelScope.launch { prefs.setExtraTileCount(value) }
    fun setShowWeatherInfo(value: Boolean) = viewModelScope.launch {
        prefs.setShowWeatherInfo(value)
        if (value) refreshWeather(force = true)
    }
    fun setShowMindfulUsage(value: Boolean) = viewModelScope.launch { prefs.setShowMindfulUsage(value) }
    fun setEnableTimeline(value: Boolean) = viewModelScope.launch { prefs.setEnableTimeline(value) }
    fun setHomeHeaderMode(value: String) = viewModelScope.launch {
        musicRevertJob?.cancel()
        if (value != "music") preMusicHeaderMode = null
        prefs.setHomeHeaderMode(value)
    }
    fun setBatchNotifications(value: Boolean) = viewModelScope.launch { prefs.setBatchNotifications(value) }
    fun deliverBatch() = viewModelScope.launch {
        prefs.setLastBatchTime(System.currentTimeMillis())
        refreshState()
    }
    fun setTileTransparency(value: Float) = viewModelScope.launch { prefs.setTileTransparency(value) }
    fun setLayoutStyle(value: String) = viewModelScope.launch { prefs.setLayoutStyle(value) }
    fun incrementAppDrawerCount() = viewModelScope.launch { prefs.incrementAppDrawerOpenCount() }
    fun emergencyOpenAppDrawer() = viewModelScope.launch { prefs.incrementEmergencyDrawerOpens() }
    fun updateTileOverride(tileId: Int, pkg: String, label: String) = viewModelScope.launch { prefs.setTileOverride(tileId, pkg, label) }
    
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        when (mode) {
            ThemeMode.LIGHT -> { prefs.setIsLightMode(true); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(false) }
            ThemeMode.DARK -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(false) }
            ThemeMode.CIRCADIAN -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(true); prefs.setShowWallpaper(false) }
            ThemeMode.TRANSPARENT -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(true) }
        }
    }

    fun setUseLiquidGlass(value: Boolean) = viewModelScope.launch { prefs.setUseLiquidGlass(value) }
    fun setIconPackPackage(value: String?) = viewModelScope.launch { 
        prefs.setIconPackPackage(value)
        iconCache.clearCache()
    }

    val monthlyPrice = storeBridge.monthlyPrice
    val yearlyPrice = storeBridge.yearlyPrice
    val lifetimePrice = storeBridge.lifetimePrice
    val isStoreConnected = storeBridge.isStoreConnected

    fun buyProduct(activity: Activity, productId: String) = storeBridge.startBillingFlow(activity, productId)
    fun retryStoreConnection() = storeBridge.refreshPremiumStatus()
    fun acceptAppDisclosure() = viewModelScope.launch { prefs.setHasAcceptedAppDisclosure(true) }
    fun setOnboardingSeen() = viewModelScope.launch { prefs.setHasSeenOnboarding(true) }

    fun setPremium(value: Boolean) = viewModelScope.launch { prefs.setPremium(value) }
    fun acknowledgeSponsoredAd() = viewModelScope.launch { prefs.setLastSponsoredShowTime(System.currentTimeMillis()) }

    fun createProfile(name: String) = viewModelScope.launch {
        val newId = prefs.createProfile(name)
        prefs.switchProfile(newId)
    }
    fun deleteProfile(id: String) = viewModelScope.launch { prefs.deleteProfile(id) }
    fun switchProfile(id: String) = viewModelScope.launch { prefs.switchProfile(id) }

    suspend fun exportSettings(): String = prefs.exportSettings()
    suspend fun importSettings(json: String): Boolean {
        return try {
            val success = prefs.importSettings(json)
            if (success) refreshState()
            success
        } catch (_: Exception) { false }
    }

    fun getInstalledApps(): List<DrawerApp> {
        return _installedAppsCache.value.ifEmpty {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            pm.queryIntentActivities(intent, 0).map { 
                DrawerApp(it.activityInfo.packageName, it.loadLabel(pm).toString())
            }.distinctBy { it.packageName }.sortedBy { it.label }
        }
    }

    fun getInstalledAppsForTile(tileId: Int, currentProfileId: String): List<DrawerApp> {
        val allApps = getInstalledApps()
        
        // Always show all apps for the 3rd page (tiles 12+) or non-default profiles
        if (tileId >= 12 || currentProfileId != "default") return allApps

        val intentFilter = when (tileId) {
            0 -> getAppsByIntent(Intent(Intent.ACTION_DIAL)) + 
                 getAppsByIntent(Intent(Intent.ACTION_VIEW).setData(Uri.parse("tel:"))) +
                 getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CONTACTS) })
            2 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) })
            3 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) })
            4 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) })
            6 -> getAppsByIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            7 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) })
            8 -> getAppsByIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)) + 
                 getAppsByIntent(Intent(AlarmClock.ACTION_SET_ALARM))
            9 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) })
            else -> emptyList()
        }.map { it.packageName }.toSet()

        val keywordFiltered = when (tileId) {
            0 -> filterByKeywords(allApps, listOf("phone", "dialer", "call", "contact", "telephon"))
            1 -> {
                val matches = filterByKeywords(allApps, listOf("chat", "whatsapp", "telegram", "messenger", "signal", "viber", "line"))
                // Prioritize WhatsApp variants at the top
                matches.sortedByDescending { it.packageName.contains("whatsapp", ignoreCase = true) }
            }
            2 -> filterByKeywords(allApps, listOf("messag", "sms", "mms"))
            3 -> filterByKeywords(allApps, listOf("map", "navigat", "waze", "uber", "lyft"))
            4 -> filterByKeywords(allApps, listOf("music", "spotify", "player", "audio", "yt music", "soundcloud"))
            5 -> filterByKeywords(allApps, listOf("pay", "wallet", "bank", "card", "gpay"))
            6 -> filterByKeywords(allApps, listOf("camera", "lens", "snap", "shot"))
            7 -> filterByKeywords(allApps, listOf("calculator", "calc"))
            8 -> filterByKeywords(allApps, listOf("clock", "alarm", "timer", "watch"))
            9 -> filterByKeywords(allApps, listOf("calendar", "event", "schedule"))
            10 -> filterByKeywords(allApps, listOf("note", "keep", "memo", "todo", "task"))
            11 -> filterByKeywords(allApps, listOf("settings", "config", "control", "launcher"))
            else -> allApps
        }
        
        val intentMatches = allApps.filter { it.packageName in intentFilter }
        
        return (intentMatches + keywordFiltered).distinctBy { it.packageName }.ifEmpty { allApps }
    }

    private fun getAppsByIntent(intent: Intent): List<DrawerApp> {
        return try {
            pm.queryIntentActivities(intent, 0).map {
                DrawerApp(it.activityInfo.packageName, it.loadLabel(pm).toString())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun filterByKeywords(apps: List<DrawerApp>, keywords: List<String>): List<DrawerApp> {
        return apps.filter { app -> 
            keywords.any { kw -> 
                app.label.contains(kw, ignoreCase = true) || 
                app.packageName.contains(kw, ignoreCase = true) 
            } 
        }
    }
}
