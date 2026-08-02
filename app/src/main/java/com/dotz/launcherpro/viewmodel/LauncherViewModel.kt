package com.dotz.launcherpro.viewmodel

import android.app.Activity
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
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
import com.dotz.launcherpro.manager.*
import com.dotz.launcherpro.manager.SponsoredContentManager
import com.dotz.launcherpro.manager.UsageManager
import com.dotz.launcherpro.manager.UsageStatsResult
import com.dotz.launcherpro.services.DotzNotificationService
import com.dotz.launcherpro.services.NotificationItem
import com.google.gson.Gson
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class ThemeState(
    val settings: DotzSettings = DotzSettings(),
    val isPremium: Boolean = false,
    val isUpgradeAvailable: Boolean = true,
    val isLiteVersion: Boolean = false,
    val currentThemeMode: ThemeMode = ThemeMode.DARK,
    val installedIconPacks: List<Pair<String, String>> = emptyList(),
    val isUpdateAvailable: Boolean = false,
    val isStoreConnected: Boolean = false,
    val isDefaultLauncher: Boolean = false
)

data class FocusState(
    val focusTimeToday: String = "0h 0m",
    val focusTimeMillis: Long = 0,
    val focusStreak: Int = 0,
    val focusScore: Int = 100,
    val focusScoreHistory: List<Pair<String, Int>> = emptyList(),
    val focusSoundPlaying: String? = null,
    val ultraFocusRemainingMillis: Long = 0,
    val showUltraFocusExitReason: Boolean = false,
    val weeklyReflection: WeeklyReflection? = null,
    val hasUsageStatsPermission: Boolean = false,
    val unlockCount: Int = 0,
    val notificationsReceivedToday: Int = 0,
    val totalAppOpens: Int = 0
)

data class TilesState(
    val page0Tiles: List<AppTile> = emptyList(),
    val page1Tiles: List<AppTile> = emptyList(),
    val page2Tiles: List<AppTile> = emptyList(),
    val ultraFocusTiles: List<AppTile> = emptyList(),
    val allApps: List<DrawerApp> = emptyList(),
    val topApps: List<DrawerApp> = emptyList(),
    val suggestedApps: List<DrawerApp> = emptyList(),
    val notificationCounts: Map<String, Int> = emptyMap(),
    val isLoaded: Boolean = false
)

data class TimelineState(
    val timelineItems: List<TimelineItem> = emptyList(),
    val upcomingEvents: List<com.dotz.launcherpro.manager.CalendarEvent> = emptyList(),
    val nextAlarm: String? = null,
    val blockedNotificationsCount: Int = 0,
    val isTimelineVisible: Boolean = false
)

data class AdsState(
    val nativeAd: com.google.android.gms.ads.nativead.NativeAd? = null,
    val isAdLoading: Boolean = false
)

data class LauncherUiState(
    val theme: ThemeState = ThemeState(),
    val system: SystemHeaderState = SystemHeaderState(0, "None", false, false, false, false, false, false, 0, false),
    val weather: WeatherState = WeatherState(null, null, null, null, null, null, null, null, null),
    val media: MediaState = MediaState("Not Playing", "", "", null, false, 0, 0, 0),
    val focus: FocusState = FocusState(),
    val tiles: TilesState = TilesState(),
    val timeline: TimelineState = TimelineState(),
    val ads: AdsState = AdsState()
)

data class WeeklyReflection(
    val focusScore: Int,
    val focusScoreDelta: Int,
    val unlocks: Int,
    val unlocksDeltaPercent: Int,
    val notifications: Int,
    val ignored: Int,
    val longestFocus: String,
    val mostProductiveDay: String,
    val wellnessRating: String
)

data class AppShortcut(
    val id: String,
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Icon? = null
)

data class SystemHeaderState(
    val battery: Int,
    val network: String,
    val wifi: Boolean,
    val bluetooth: Boolean,
    val silent: Boolean,
    val torch: Boolean,
    val airplane: Boolean,
    val dark: Boolean,
    val ringer: Int,
    val mobileData: Boolean
)

data class WeatherState(
    val temp: String?,
    val condition: String?,
    val feelsLike: String?,
    val summary: String?,
    val aqi: String?,
    val aqiLabel: String?,
    val low: String?,
    val high: String?,
    val location: String?
)

data class MediaState(
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val interpolatedPosition: Long
)

enum class ThemeMode { LIGHT, DARK, CIRCADIAN, TRANSPARENT, CUSTOM }

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val dotzApp = application as com.dotz.launcherpro.DotzApp
    private val prefs = dotzApp.prefsRepository
    private val storeBridge = dotzApp.storeBridge
    private val usageManager = UsageManager(application)
    private val calendarManager = CalendarManager(application)
    private val systemStateManager = dotzApp.systemStateManager
    private val weatherManager = dotzApp.weatherManager
    private val mediaManager = dotzApp.mediaManager
    private val adsManager = dotzApp.adsManager
    val iconCache = IconCacheManager(application)
    private val pm: PackageManager = application.packageManager
    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val appUpdateManager = AppUpdateManagerFactory.create(application)

    private var musicRevertJob: Job? = null
    private var preMusicHeaderMode: String? = null

    private val _weeklyReflection = MutableStateFlow<WeeklyReflection?>(null)
    private val _journalEntries = MutableStateFlow<List<TimelineItem>>(emptyList())

    private val installedCache = mutableMapOf<String, Boolean>()
    private var cachedIsDefault = false
    
    private var lastTiles: List<AppTile>? = null
    private var lastTilesDeps: Any? = null
    
    private var lastTimeline: List<TimelineItem>? = null
    private var lastTimelineDeps: Any? = null
    
    private var lastTopApps: List<DrawerApp>? = null
    private var lastTopAppsDeps: Any? = null
    
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        adsManager.showRewardedAd(activity, onRewardEarned)
    }

    fun grant24HourPremium() = viewModelScope.launch {
        val expiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        prefs.setPremiumExpiry(expiry)
    }

    // Targeted State Flows for Performance
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private val _systemHeaderState = MutableStateFlow(SystemHeaderState(0, "None", false, false, false, false, false, false, 0, false))
    val systemHeaderState: StateFlow<SystemHeaderState> = _systemHeaderState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState(null, null, null, null, null, null, null, null, null))
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState("Not Playing", "", "", null, false, 0, 0, 0))
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val _focusState = MutableStateFlow(FocusState())
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    private val _tilesState = MutableStateFlow(TilesState())
    val tilesState: StateFlow<TilesState> = _tilesState.asStateFlow()

    private val _timelineState = MutableStateFlow(TimelineState())
    val timelineState: StateFlow<TimelineState> = _timelineState.asStateFlow()

    private val _adsState = MutableStateFlow(AdsState())
    val adsState: StateFlow<AdsState> = _adsState.asStateFlow()

    // Combined UI State (Legacy Support & Global Observation)
    val uiState: StateFlow<LauncherUiState> = combine(
        themeState, systemHeaderState, weatherState, mediaState, focusState, tilesState, timelineState, adsState
    ) { args ->
        LauncherUiState(
            theme = args[0] as ThemeState,
            system = args[1] as SystemHeaderState,
            weather = args[2] as WeatherState,
            media = args[3] as MediaState,
            focus = args[4] as FocusState,
            tiles = args[5] as TilesState,
            timeline = args[6] as TimelineState,
            ads = args[7] as AdsState
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _timerTicker = MutableStateFlow(System.currentTimeMillis())
    private val _usageStats = MutableStateFlow(UsageStatsResult(emptyMap(), 0L, 0, 0))
    private val _installedAppsCache = MutableStateFlow<List<DrawerApp>>(emptyList())
    private val _isUpdateAvailable = MutableStateFlow(false)
    private val _installedIconPacks = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private val _upcomingEvents = MutableStateFlow<List<com.dotz.launcherpro.manager.CalendarEvent>>(emptyList())
    private val _nextAlarm = MutableStateFlow<String?>(null)
    private val _showUltraFocusExitReason = MutableStateFlow(value = false)

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

    private fun checkInAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                _isUpdateAvailable.value = true
            }
        }
    }

    fun startUpdateFlow(activity: Activity) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    @Suppress("DEPRECATION")
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        activity,
                        999 // REQUEST_CODE_UPDATE
                    )
                } catch (e: Exception) {
                    Log.e("LauncherViewModel", "Update flow failed", e)
                    // Fallback to Play Store
                    downloadUpdate(activity.packageName)
                }
            } else {
                downloadUpdate(activity.packageName)
            }
        }
    }

    private fun refreshIsDefault() {
        cachedIsDefault = isDefaultLauncher()
    }

    fun startBackgroundTasks() {
        checkInAppUpdate()
        
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
    }

    init {
        // Update streak and reset focus time if it's a new day
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val now = System.currentTimeMillis()
            val lastDate = settings.lastUsedDate
            
            val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
            val calendarLast = Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = (calendarNow[Calendar.DAY_OF_YEAR] == calendarLast[Calendar.DAY_OF_YEAR]) &&
                           (calendarNow[Calendar.YEAR] == calendarLast[Calendar.YEAR])
            
            val isNextDay = !isSameDay && (now - lastDate < 48 * 60 * 60 * 1000)
            
            val newStreak = if (isNextDay) settings.focusStreak + 1 else if (isSameDay) settings.focusStreak else 1
            val newFocusTime = if (isSameDay) settings.focusTimeToday else 0L
            
            prefs.updateFocusStats(newStreak, now, newFocusTime, resetDrawerCount = !isSameDay)
            if (!isSameDay) {
                // Record final stats for the day that just ended
                val yesterday = Calendar.getInstance().apply { timeInMillis = lastDate }
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday.time)
                val usage = if (usageManager.hasUsageStatsPermission()) usageManager.getAllAppStatsToday() else null
                usage?.let {
                    prefs.updateDailyStats(dateStr, DailyStats(it.unlockCount, it.notificationsReceived, it.totalScreenTime, 0)) // Blocked count needs wiring
                }
                _refreshTrigger.emit(Unit)
            }
            
            // Check for Monday Morning Weekly Reflection
            val calendar = Calendar.getInstance()
            if (calendar[Calendar.DAY_OF_WEEK] == Calendar.MONDAY) {
                val year = calendar[Calendar.YEAR]
                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                val weekId = "$year-$week"
                
                if (settings.lastWeeklyReflectionDate != weekId) {
                    calculateWeeklyReflection()
                }
            }
        }

        // Periodic update of focus time
        viewModelScope.launch {
            while (true) {
                delay(60.seconds)
                updateSessionTime()
            }
        }

        // Automatic Header Switching (Music <-> Focus)
        viewModelScope.launch {
            combine(
                mediaManager.playbackState.map { it.first }, // isPlaying
                mediaManager.nowPlayingPackage
            ) { playing, pkg -> playing to pkg }
                .distinctUntilChanged()
                .collectLatest { (isPlaying, pkg) ->
                    val currentSettings = prefs.settingsFlow.first()
                    if (isPlaying) {
                        musicRevertJob?.cancel()
                        if (currentSettings.homeHeaderMode != "music") {
                            preMusicHeaderMode = currentSettings.homeHeaderMode
                            prefs.setHomeHeaderMode("music")
                            android.util.Log.d("LauncherViewModel", "Auto-switch to Music. Previous: $preMusicHeaderMode")
                        }
                    } else {
                        // Revert if music is stopped and we are currently in music mode
                        if (currentSettings.homeHeaderMode == "music") {
                            musicRevertJob?.cancel()
                            musicRevertJob = launch {
                                delay(30000) // 30s Revert
                                val revertTo = preMusicHeaderMode ?: "stats"
                                prefs.setHomeHeaderMode(revertTo)
                                preMusicHeaderMode = null
                            }
                        }
                    }
                }
        }

        // Hybrid Polling Loop (Low Frequency, Battery Safe)
        viewModelScope.launch {
            while (true) {
                if (_isUiVisible.value) {
                    mediaManager.refresh()
                }
                delay(3000) // 3s poll
            }
        }

        // Dedicated Journal Parsing (Background)
        viewModelScope.launch(Dispatchers.IO) {
            prefs.settingsFlow
                .map { it.journalEntriesJson }
                .distinctUntilChanged()
                .collect { json ->
                    val typeToken = object : com.google.gson.reflect.TypeToken<List<TimelineItem>>() {}.type
                    val journals: List<TimelineItem> = try {
                        Gson().fromJson(json, typeToken) ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                    _journalEntries.value = journals
                }
        }

        refreshIsDefault()
        checkInAppUpdate()
        
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
                        val component = it.activityInfo.name
                        val label = it.loadLabel(pm).toString()
                        val stats = usage.appStats[pkg]
                        DrawerApp(pkg, label, stats?.component1(), stats?.component2() ?: 0, component)
                    }
                _installedAppsCache.value = apps
                
                // Auto-resolve tiles for OEM apps on first run or when apps change
                autoResolveTiles()
            }
        }

        // Events & Alarm Updates
        viewModelScope.launch(Dispatchers.IO) {
            _refreshTrigger.collect {
                _upcomingEvents.value = calendarManager.getUpcomingEvents()
                val alarmClock = (getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).nextAlarmClock
                _nextAlarm.value = alarmClock?.let {
                    val sdf = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                    sdf.format(Date(it.triggerTime))
                }
            }
        }

        // Focus Score History tracking
        viewModelScope.launch {
            uiState.map { it.focus.focusScore }.distinctUntilChanged().collect { calculatedScore ->
                val settings = prefs.settingsFlow.first()
                val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (settings.focusScoreHistory[isoDate] != calculatedScore) {
                    prefs.updateFocusScoreHistory(isoDate, calculatedScore)
                }
            }
        }

        // 1. System Header State
        viewModelScope.launch {
            combine(
                systemStateManager.batteryLevel,
                systemStateManager.networkStatus,
                systemStateManager.isWifiEnabled,
                systemStateManager.isBluetoothEnabled,
                systemStateManager.isSilentMode,
                systemStateManager.isTorchOn,
                systemStateManager.isAirplaneModeOn,
                systemStateManager.isDarkModeOn,
                systemStateManager.ringerMode,
                systemStateManager.isMobileDataEnabled
            ) { args ->
                SystemHeaderState(
                    battery = args[0] as Int,
                    network = args[1] as String,
                    wifi = args[2] as Boolean,
                    bluetooth = args[3] as Boolean,
                    silent = args[4] as Boolean,
                    torch = args[5] as Boolean,
                    airplane = args[6] as Boolean,
                    dark = args[7] as Boolean,
                    ringer = args[8] as Int,
                    mobileData = args[9] as Boolean
                )
            }.collect { _systemHeaderState.value = it }
        }

        // 2. Weather State
        viewModelScope.launch {
            combine(
                weatherManager.weatherTemp,
                weatherManager.weatherCondition,
                weatherManager.weatherFeelsLike,
                weatherManager.weatherSummary,
                weatherManager.weatherAqi,
                weatherManager.weatherAqiLabel,
                weatherManager.weatherLow,
                weatherManager.weatherHigh,
                weatherManager.locationName
            ) { args ->
                WeatherState(
                    temp = args[0] as String?,
                    condition = args[1] as String?,
                    feelsLike = args[2] as String?,
                    summary = args[3] as String?,
                    aqi = args[4] as String?,
                    aqiLabel = args[5] as String?,
                    low = args[6] as String?,
                    high = args[7] as String?,
                    location = args[8] as String?
                )
            }.collect { _weatherState.value = it }
        }

        // 3. Media State
        viewModelScope.launch {
            combine(
                mediaManager.nowPlaying,
                mediaManager.playbackState,
                mediaManager.nowPlayingPackage,
                mediaManager.lastPositionUpdateTime,
                _timerTicker
            ) { nowPlaying, playback, pkg, lastUpdate, tick ->
                val isPlaying = playback.first
                val basePosition = playback.second
                val duration = playback.third
                val interpolated = if (isPlaying && duration > 0) {
                    val elapsed = tick - lastUpdate
                    (basePosition + elapsed.coerceAtLeast(0L)).coerceAtMost(duration)
                } else basePosition

                MediaState(
                    title = nowPlaying.first,
                    artist = nowPlaying.second,
                    album = nowPlaying.third,
                    packageName = pkg,
                    isPlaying = isPlaying,
                    position = basePosition,
                    duration = duration,
                    interpolatedPosition = interpolated
                )
            }.collect { _mediaState.value = it }
        }

        // 4. Theme State
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                storeBridge.isPremium,
                _isUpdateAvailable,
                storeBridge.isStoreConnected,
                _installedIconPacks
            ) { args ->
                val settings = args[0] as DotzSettings
                val isPremiumStatus = args[1] as Boolean
                val isUpdateAvailable = args[2] as Boolean
                val storeConnected = args[3] as Boolean
                @Suppress("UNCHECKED_CAST")
                val iconPacks = args[4] as List<Pair<String, String>>

                val isCurrentlyPremium = settings.isPremium || isPremiumStatus || (settings.premiumExpiry > System.currentTimeMillis())
                
                val themeMode = when {
                    settings.themeId == "custom" -> ThemeMode.CUSTOM
                    settings.showWallpaper -> ThemeMode.TRANSPARENT
                    settings.useCircadianTheming -> ThemeMode.CIRCADIAN
                    settings.isLightMode -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }

                ThemeState(
                    settings = settings,
                    isPremium = isCurrentlyPremium,
                    isUpgradeAvailable = storeBridge.isUpgradeAvailable,
                    isLiteVersion = storeBridge.isLiteVersion,
                    currentThemeMode = themeMode,
                    installedIconPacks = iconPacks,
                    isUpdateAvailable = isUpdateAvailable,
                    isStoreConnected = storeConnected,
                    isDefaultLauncher = cachedIsDefault
                )
            }.collect { _themeState.value = it }
        }

        // 5. Focus State
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                _usageStats,
                _weeklyReflection,
                _showUltraFocusExitReason
            ) { settings, usageResult, weeklyReflection, showExitReason ->
                val totalTimeMillis = usageResult.totalScreenTime
                val startOfDayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val elapsedToday = System.currentTimeMillis() - startOfDayCalendar.timeInMillis
                val focusTimeTodayMillis = (elapsedToday - totalTimeMillis).coerceAtLeast(0L)

                val unlockPenalty = ((usageResult.unlockCount - 15).coerceAtLeast(0)) * 2
                val minutesUsed = totalTimeMillis / 60000
                val screenTimePenalty = (minutesUsed / 4).toInt()
                val emergencyPenalty = settings.emergencyDrawerOpens * 15
                val calculatedScore = (100 - unlockPenalty - screenTimePenalty - emergencyPenalty).coerceIn(0, 100)
                
                val historyList = settings.focusScoreHistory.toList().sortedBy { it.first }
                val ultraFocusRemaining = (settings.ultraFocusEndTime - System.currentTimeMillis()).coerceAtLeast(0L)

                FocusState(
                    focusTimeToday = formatDuration(focusTimeTodayMillis),
                    focusTimeMillis = focusTimeTodayMillis,
                    focusStreak = settings.focusStreak,
                    focusScore = calculatedScore,
                    focusScoreHistory = historyList,
                    ultraFocusRemainingMillis = ultraFocusRemaining,
                    showUltraFocusExitReason = showExitReason,
                    weeklyReflection = weeklyReflection,
                    hasUsageStatsPermission = usageManager.hasUsageStatsPermission(),
                    unlockCount = usageResult.unlockCount,
                    notificationsReceivedToday = usageResult.notificationsReceived,
                    totalAppOpens = usageResult.totalAppOpens
                )
            }.collect { _focusState.value = it }
        }

        // 6. Tiles State
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                DotzNotificationService.notificationCounts,
                _usageStats,
                _installedAppsCache
            ) { settings, notifCounts, usageResult, allApps ->
                val currentTilesDeps = Triple(settings.tileOverrides, notifCounts, usageResult.appStats)
                val allTilesUnordered = if (currentTilesDeps == lastTilesDeps && lastTiles != null) {
                    lastTiles!!
                } else {
                    val built = buildTilesFast(DefaultApps.allDefaults, settings, notifCounts, usageResult.appStats)
                    lastTiles = built
                    lastTilesDeps = currentTilesDeps
                    built
                }
                
                val allTiles = settings.tileOrder.mapNotNull { id ->
                    allTilesUnordered.find { it.tileId == id }
                }

                val p0 = allTiles.take(6)
                val p1 = allTiles.drop(6).take(6)
                val p2 = if (settings.enableExtraPage) allTiles.drop(12).take(settings.extraTileCount) else emptyList()

                val ultraFocusTiles = settings.ultraFocusAppPackages.map { appStr ->
                    val pkg: String
                    val comp: String?
                    if (appStr.contains("|")) {
                        val parts = appStr.split("|")
                        pkg = parts[0]
                        comp = parts.getOrNull(1)
                    } else {
                        pkg = appStr
                        comp = null
                    }
                    val label = allApps.find { it.packageName == pkg && it.componentName == comp }?.label 
                        ?: pkg.substringAfterLast('.').uppercase()
                    AppTile(-1, pkg, label, Icons.Default.Apps, componentName = comp)
                }

                val topApps = if (allApps == lastTopAppsDeps && lastTopApps != null) {
                    lastTopApps!!
                } else {
                    val result = allApps.sortedByDescending { it.launchCount }.take(5)
                    lastTopApps = result
                    lastTopAppsDeps = allApps
                    result
                }

                val homePackages = (p0 + p1 + p2).map { it.packageName }.toSet()
                val suggestedApps = allApps
                    .filter { it.packageName !in homePackages }
                    .sortedByDescending { it.launchCount }
                    .take(3)

                TilesState(
                    page0Tiles = p0,
                    page1Tiles = p1,
                    page2Tiles = p2,
                    ultraFocusTiles = ultraFocusTiles,
                    allApps = allApps,
                    topApps = topApps,
                    suggestedApps = suggestedApps,
                    notificationCounts = notifCounts,
                    isLoaded = true
                )
            }.collect { _tilesState.value = it }
        }

        // 7. Timeline State
        viewModelScope.launch {
            combine(
                DotzNotificationService.notifications,
                _mediaState, // Use the already calculated media state
                _journalEntries,
                _upcomingEvents,
                _nextAlarm,
                _isTimelineVisible,
                DotzNotificationService.blockedCount
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val notifications = args[0] as List<NotificationItem>
                val media = args[1] as MediaState
                @Suppress("UNCHECKED_CAST")
                val journalItems = args[2] as List<TimelineItem>
                @Suppress("UNCHECKED_CAST")
                val upcomingEvents = args[3] as List<CalendarEvent>
                val alarm = args[4] as String?
                val timelineVisible = args[5] as Boolean
                val blocked = args[6] as Int

                val currentTimelineDeps = Pair(notifications, media.title)
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

                    if (media.title != "Not Playing" && media.title.isNotBlank()) {
                        timeline.add(TimelineItem(
                            id = "music_current",
                            type = TimelineType.MUSIC,
                            title = media.title,
                            subtitle = media.artist,
                            timestamp = System.currentTimeMillis(),
                            packageName = media.packageName
                        ))
                    }

                    timeline.addAll(journalItems)
                    val result = timeline.sortedByDescending { it.timestamp }
                    lastTimeline = result
                    lastTimelineDeps = currentTimelineDeps
                    result
                }

                TimelineState(
                    timelineItems = finalTimeline,
                    upcomingEvents = upcomingEvents,
                    nextAlarm = alarm,
                    blockedNotificationsCount = blocked,
                    isTimelineVisible = timelineVisible
                )
            }.collect { _timelineState.value = it }
        }

        // 8. Ads State
        viewModelScope.launch {
            combine(
                adsManager.nativeAdFlow,
                adsManager.isAdLoading
            ) { ad, loading ->
                AdsState(ad, loading)
            }.collect { _adsState.value = it }
        }

        refreshState()
    }

    fun downloadUpdate(url: String) {
        val app = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${app.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${app.packageName}".toUri())
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
        
        val currentMode = audioManager.ringerMode
        val nextMode = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    AudioManager.RINGER_MODE_SILENT
                } else {
                    Toast.makeText(app, "Grant DND access to use Silent Mode", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(intent)
                    return
                }
            }
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        
        try {
            // Optimistic Update
            systemStateManager.setRingerMode(nextMode)
            audioManager.ringerMode = nextMode
        } catch (e: Exception) {
            Log.e("LauncherViewModel", "Failed to set ringer mode", e)
            systemStateManager.setRingerMode(currentMode)
        }
    }

    fun toggleTorch() {
        val app = getApplication<Application>()
        val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val currentState = systemStateManager.isTorchOn.value
        val newState = !currentState

        try {
            // Target the first camera with a flash (usually "0")
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"

            // Optimistic Update
            systemStateManager.setTorchState(newState)
            
            cameraManager.setTorchMode(cameraId, newState)
        } catch (e: Exception) {
            Log.e("LauncherViewModel", "Flashlight toggle failed", e)
            // Revert on error
            systemStateManager.setTorchState(currentState)
            Toast.makeText(app, "Flashlight not available", Toast.LENGTH_SHORT).show()
        }
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
        setIsLightMode(!uiState.value.theme.settings.isLightMode)
    }

    // ── Media Controls ────────────────────────────────────────────────────────

    fun mediaPlayPause() = mediaManager.playPause()
    fun mediaSkipNext() = mediaManager.skipNext()
    fun mediaSkipPrevious() = mediaManager.skipPrevious()

    fun launchApp(packageName: String?, componentName: String? = null): Boolean {
        Log.d("LauncherViewModel", "launchApp requested for: $packageName / $componentName")
        if (packageName.isNullOrBlank()) return false
        
        val app = getApplication<Application>()
        
        if (!packageName.contains(".") && packageName.any { it.isDigit() }) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            return true
        }

        val intent = if (componentName != null) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, componentName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            app.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        if (intent != null) {
            try {
                app.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Failed to launch $packageName", e)
            }
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
                            updateTileOverride(tileId, bestMatch.packageName, bestMatch.componentName, bestMatch.label.uppercase())
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
            val override = settings.tileOverrides[tile.tileId]
            val pkg: String
            val component: String?
            
            if (override != null) {
                if (override.contains("|")) {
                    val parts = override.split("|")
                    pkg = parts[0]
                    component = parts.getOrNull(1)
                } else {
                    pkg = override
                    component = null
                }
            } else {
                pkg = resolvePackage(tile.packageName)
                component = null
            }

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
                componentName = component,
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
            val currentOrder = uiState.value.theme.settings.tileOrder.toMutableList()
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
    fun setWeatherUnit(value: String) = viewModelScope.launch {
        prefs.setWeatherUnit(value)
        refreshWeather(force = true)
    }
    fun setShowMindfulUsage(value: Boolean) = viewModelScope.launch { prefs.setShowMindfulUsage(value) }
    fun setEnableTimeline(value: Boolean) = viewModelScope.launch { prefs.setEnableTimeline(value) }
    fun setHomeHeaderMode(value: String) = viewModelScope.launch {
        musicRevertJob?.cancel()
        if (value != "music") preMusicHeaderMode = null
        prefs.setHomeHeaderMode(value)
    }
    fun setBatchNotifications(value: Boolean) = viewModelScope.launch { prefs.setBatchNotifications(value) }
    fun setNotificationBatchInterval(value: Int) = viewModelScope.launch { prefs.setNotificationBatchInterval(value) }
    
    fun setEditModeEnabled(value: Boolean) = viewModelScope.launch { prefs.setEditModeEnabled(value) }
    
    fun setFontId(value: String) = viewModelScope.launch { prefs.setFontId(value) }
    fun setThemeId(value: String) = viewModelScope.launch { prefs.setThemeId(value) }
    fun setClockStyle(value: String) = viewModelScope.launch { prefs.setClockStyle(value) }
    fun setCustomAccentColor(color: Int?) = viewModelScope.launch { prefs.setCustomAccentColor(color) }
    fun setUseBiometricPause(value: Boolean) = viewModelScope.launch { prefs.setUseBiometricPause(value) }

    fun getShortcutsForApp(packageName: String): List<AppShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        
        return try {
            val query = android.content.pm.LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or 
                             android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or 
                             android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            launcherApps.getShortcuts(query, android.os.Process.myUserHandle())?.map { 
                AppShortcut(it.id, (it.shortLabel ?: it.longLabel ?: "Shortcut").toString(), packageName)
            }?.take(4) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun launchShortcut(shortcut: AppShortcut) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, android.os.Process.myUserHandle())
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to app launch
            launchApp(shortcut.packageName)
        }
    }

    fun deliverBatch() = viewModelScope.launch {
        prefs.setLastBatchTime(System.currentTimeMillis())
        refreshState()
    }
    fun setTileTransparency(value: Float) = viewModelScope.launch { prefs.setTileTransparency(value) }

    fun addJournalEntry(text: String) = viewModelScope.launch(Dispatchers.Default) {
        if (text.isBlank()) return@launch
        
        val currentJournals = _journalEntries.value.toMutableList()
        val newItem = TimelineItem(
            id = UUID.randomUUID().toString(),
            type = TimelineType.JOURNAL,
            title = "Note",
            subtitle = text,
            timestamp = System.currentTimeMillis()
        )
        
        // Optimistic Update
        currentJournals.add(0, newItem)
        _journalEntries.value = currentJournals.sortedByDescending { it.timestamp }
        
        // Background Save
        prefs.setJournalEntries(Gson().toJson(_journalEntries.value))
    }

    fun deleteJournalEntry(id: String) = viewModelScope.launch(Dispatchers.Default) {
        val currentJournals = _journalEntries.value.toMutableList()
        if (currentJournals.removeAll { it.id == id }) {
            // Optimistic Update
            _journalEntries.value = currentJournals
            
            // Background Save
            prefs.setJournalEntries(Gson().toJson(currentJournals))
        }
    }

    fun updateJournalEntry(id: String, newText: String) = viewModelScope.launch(Dispatchers.Default) {
        if (newText.isBlank()) return@launch
        
        val currentJournals = _journalEntries.value.toMutableList()
        val index = currentJournals.indexOfFirst { it.id == id }
        if (index != -1) {
            // Optimistic Update
            currentJournals[index] = currentJournals[index].copy(subtitle = newText)
            _journalEntries.value = currentJournals
            
            // Background Save
            prefs.setJournalEntries(Gson().toJson(currentJournals))
        }
    }

    private fun calculateWeeklyReflection() = viewModelScope.launch {
        val settings = prefs.settingsFlow.first()
        val typeToken = object : com.google.gson.reflect.TypeToken<Map<String, DailyStats>>() {}.type
        val dailyStats: Map<String, DailyStats> = Gson().fromJson(settings.dailyStatsJson, typeToken) ?: emptyMap()
        
        // Simple logic for MVP
        val last7Days = (1..7).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DATE, -i) }
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        }
        
        val totalUnlocks = last7Days.sumOf { dailyStats[it]?.unlocks ?: 0 }
        val totalNotifs = last7Days.sumOf { dailyStats[it]?.notifications ?: 0 }
        
        val scores = settings.focusScoreHistory.values.toList()
        val last7 = if (scores.size >= 7) scores.takeLast(7) else scores
        val avgFocus = if (last7.isNotEmpty()) last7.average().toInt().coerceIn(0, 100) else 100
        
        _weeklyReflection.value = WeeklyReflection(
            focusScore = avgFocus,
            focusScoreDelta = 5, // Mock
            unlocks = totalUnlocks,
            unlocksDeltaPercent = -10, // Mock
            notifications = totalNotifs,
            ignored = 150, // Mock
            longestFocus = "2h 15m",
            mostProductiveDay = "Wednesday",
            wellnessRating = "Excellent"
        )
    }

    fun dismissWeeklyReflection() = viewModelScope.launch {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        prefs.setLastWeeklyReflectionDate("$year-$week")
        _weeklyReflection.value = null
    }

    fun setLayoutStyle(value: String) = viewModelScope.launch { prefs.setLayoutStyle(value) }
    fun incrementAppDrawerCount() = viewModelScope.launch { prefs.incrementAppDrawerOpenCount() }
    fun emergencyOpenAppDrawer() = viewModelScope.launch { prefs.incrementEmergencyDrawerOpens() }
    fun updateTileOverride(tileId: Int, pkg: String, componentName: String?, label: String) = viewModelScope.launch { 
        prefs.setTileOverride(tileId, pkg, componentName, label) 
    }
    
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        when (mode) {
            ThemeMode.LIGHT -> { prefs.setIsLightMode(true); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(false); prefs.setThemeId("default") }
            ThemeMode.DARK -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(false); prefs.setThemeId("default") }
            ThemeMode.CIRCADIAN -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(true); prefs.setShowWallpaper(false); prefs.setThemeId("default") }
            ThemeMode.TRANSPARENT -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(true); prefs.setThemeId("default") }
            ThemeMode.CUSTOM -> { prefs.setIsLightMode(false); prefs.setUseCircadianTheming(false); prefs.setShowWallpaper(false); prefs.setThemeId("custom") }
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
                DrawerApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), componentName = it.activityInfo.name)
            }.sortedBy { it.label }
        }
    }

    fun getInstalledAppsForTile(tileId: Int, currentProfileId: String): List<DrawerApp> {
        val allApps = getInstalledApps()
        
        // Always show all apps for the 3rd page (tiles 12+) or non-default profiles
        if (tileId >= 12 || currentProfileId != "default") return allApps

        val intentMatches = when (tileId) {
            0 -> {
                // For phone, prioritize ACTION_DIAL over generic contacts
                val dialers = getAppsByIntent(Intent(Intent.ACTION_DIAL)) + 
                             getAppsByIntent(Intent(Intent.ACTION_VIEW).setData(Uri.parse("tel:")))
                val contacts = getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CONTACTS) })
                dialers + contacts
            }
            2 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) })
            3 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) })
            4 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) })
            6 -> getAppsByIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            7 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) })
            8 -> getAppsByIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)) + 
                 getAppsByIntent(Intent(AlarmClock.ACTION_SET_ALARM))
            9 -> getAppsByIntent(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) })
            else -> emptyList()
        }

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
        
        // Merge and remove duplicates based on Package + Component
        // This ensures Dialer and Contacts (same package) remain separate
        return (intentMatches + keywordFiltered)
            .distinctBy { "${it.packageName}|${it.componentName}" }
            .ifEmpty { allApps }
    }

    private fun getAppsByIntent(intent: Intent): List<DrawerApp> {
        return try {
            pm.queryIntentActivities(intent, 0).map {
                DrawerApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), componentName = it.activityInfo.name)
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
