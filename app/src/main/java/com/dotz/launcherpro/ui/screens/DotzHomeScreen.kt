@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.manager.PermissionManager
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import com.dotz.launcherpro.ui.theme.blend
import com.dotz.launcherpro.viewmodel.AppShortcut
import com.dotz.launcherpro.viewmodel.LauncherUiState
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun DotzHomeScreen(
    viewModel: LauncherViewModel,
    onLauncherSettingsTap: () -> Unit,
) {
    val context = LocalContext.current
    val iconCache = viewModel.iconCache

    // --- High-Performance Atomic State Observation ---
    val theme by viewModel.themeState.collectAsState()
    val system by viewModel.systemHeaderState.collectAsState()
    val weather by viewModel.weatherState.collectAsState()
    val media by viewModel.mediaState.collectAsState()
    val focus by viewModel.focusState.collectAsState()
    val tiles by viewModel.tilesState.collectAsState()
    val timeline by viewModel.timelineState.collectAsState()
    
    val settings = theme.settings

    // --- Migrated Fragment Logic (Dialogs & State) ---
    var showNotifPermDialog by remember { mutableStateOf(value = false) }
    var showDefaultLauncherDialog by remember { mutableStateOf(false) }
    var hasDismissedDefaultDialog by remember { mutableStateOf(false) }
    var showAppAccessDisclosure by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var tileToAssign by remember { mutableStateOf<AppTile?>(null) }
    var swapSourceTile by remember { mutableStateOf<AppTile?>(null) }
    var mindfulnessApp by remember { mutableStateOf<MindfulnessInfo?>(null) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }
    var showAppDrawerConfirmDialog by remember { mutableStateOf(false) }
    var hasAcceptedLocally by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showIntentionPause by remember { mutableStateOf<Pair<String, String?>?>(null) } // pkg to component
    var selectedShortcutTile by remember { mutableStateOf<AppTile?>(null) }
    var appShortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var editingNote by remember { mutableStateOf<Pair<String, String>?>(null) } // id to text
    var isClockEditMode by remember { mutableStateOf(false) }
    var isHeaderEditMode by remember { mutableStateOf(false) }

    val hapticPulse = {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(20L)
        }
    }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var tapCount by remember { mutableIntStateOf(0) }

    val handleHomeScreenTap = {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastTapTime) < 500) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTapTime = currentTime
        
        if (tapCount >= 3) {
            hapticPulse()
            viewModel.startUltraFocusSession(30) // Default 30 mins
            tapCount = 0
            Toast.makeText(context, "Unplugged: Ultra-Focus Mode Active", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Liquid Glass Theme ---
    val isGlass = DotzTheme.colors.isGlass

    val launchApp = { pkg: String, comp: String? ->
        val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
        if (isSocial && showIntentionPause == null) {
            showIntentionPause = pkg to comp
        } else {
            val success = viewModel.launchApp(pkg, comp)
            if (!success) {
                // Handle unassigned
                tileToAssign = (tiles.page0Tiles + tiles.page1Tiles + tiles.page2Tiles).find { 
                    it.packageName == pkg && it.componentName == comp 
                }
                if (tileToAssign == null && pkg.isNotBlank()) {
                    Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(tiles.isLoaded, settings.hasAcceptedAppDisclosure, hasAcceptedLocally, settings.hasSeenOnboarding) {
        showAppAccessDisclosure = tiles.isLoaded && settings.hasSeenOnboarding && !settings.hasAcceptedAppDisclosure && !hasAcceptedLocally
    }

    LaunchedEffect(settings.hasSeenOnboarding) {
        if (settings.hasSeenOnboarding) {
            val isNotifEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
            if (!isNotifEnabled) showNotifPermDialog = true
            if (settings.showMindfulUsage && !focus.hasUsageStatsPermission) showUsageStatsDialog = true
        }
    }

    LaunchedEffect(theme.isDefaultLauncher, settings.hasSeenOnboarding) {
        if (settings.hasSeenOnboarding && !theme.isDefaultLauncher && !hasDismissedDefaultDialog) {
            showDefaultLauncherDialog = true
        }
    }

    LaunchedEffect(tiles.isLoaded, settings.hasSeenOnboarding) {
        if (tiles.isLoaded && !settings.hasSeenOnboarding) {
            showOnboarding = true
        }
    }

    // --- Root Pager Setup (Timeline <-> Tiles) ---
    val rootPagerState = rememberPagerState(initialPage = 1) { 2 }
    
    val rootPagerDescription = if (rootPagerState.currentPage == 0) "Timeline view" else "Tiles view"

    // Sync viewmodel state for timeline visibility
    LaunchedEffect(rootPagerState.currentPage) {
        viewModel.setTimelineVisible(rootPagerState.currentPage == 0)
    }

    // Driving alpha directly from pager offset for 1:1 fluid tracking (removes lag)
    val headerAlpha by remember {
        derivedStateOf {
            if (rootPagerState.currentPage == 1) {
                (1f + rootPagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
            } else {
                rootPagerState.currentPageOffsetFraction.coerceIn(0f, 1f)
            }
        }
    }

    val backgroundModifier = when {
        settings.showWallpaper -> {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
                    endY = 400f
                )
            )
        }
        isGlass -> {
            val baseColor = DotzTheme.colors.background
            val isLightMode = baseColor.red > 0.5f && baseColor.green > 0.5f && baseColor.blue > 0.5f

            val color1 = if (isLightMode) baseColor.blend(Color.Black, 0.02f) else baseColor.blend(Color.White, 0.05f)
            val color2 = if (isLightMode) baseColor.blend(Color.Black, 0.08f) else baseColor.blend(Color.Black, 0.1f)

            Modifier.drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(baseColor, color1, baseColor, color2, baseColor),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 1000f),
                    tileMode = androidx.compose.ui.graphics.TileMode.Mirror
                )
                drawRect(brush = brush)
            }
        }
        else -> {
            Modifier.background(DotzTheme.colors.background)
        }
    }

    Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
        if (settings.layoutStyle == "ultra_focus") {
            // Static Ultra Focus Mode (No Horizontal Pager, No Scroll)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                // Simplified Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.25f),
                    contentAlignment = Alignment.Center
                ) {
                    ClockRenderer(
                        style = settings.clockStyle,
                        time = currentTime(context),
                        date = currentDate()
                    )
                }
                
                UltraFocusLayout(
                    tiles = tiles.ultraFocusTiles,
                    remainingMillis = focus.ultraFocusRemainingMillis,
                    onTileTap = { tile ->
                        if (tile.packageName == context.packageName) onLauncherSettingsTap()
                        else launchApp(tile.packageName, tile.componentName)
                    },
                    onSelectApps = {
                        context.startActivity(Intent(context, UltraFocusAppSelectionActivity::class.java))
                    },
                    onEndSession = viewModel::requestUltraFocusExit,
                    modifier = Modifier.weight(0.75f)
                )
            }
        } else {
            HorizontalPager(
                state = rootPagerState,
                modifier = Modifier.fillMaxSize().semantics { contentDescription = rootPagerDescription },
                userScrollEnabled = settings.enableTimeline && !isClockEditMode && !isHeaderEditMode
            ) { rootPageIndex ->
                if (rootPageIndex == 0) {
                    // Page 0: Timeline
                    TimelinePageContent(
                        timeline = timeline,
                        focus = focus,
                        theme = theme,
                        media = media,
                        viewModel = viewModel,
                        onEditJournal = { id, text -> editingNote = id to text },
                        onGoToPremium = onLauncherSettingsTap
                    )
                } else {
                    // Page 1: Tiles & Header
                    TilesPageContent(
                        tiles = tiles,
                        system = system,
                        weather = weather,
                        media = media,
                        theme = theme,
                        focus = focus,
                        timeline = timeline,
                        viewModel = viewModel,
                        iconCache = iconCache,
                        headerAlpha = headerAlpha,
                        swapSourceTile = swapSourceTile,
                        isClockEditMode = isClockEditMode,
                        onClockEditModeChange = { isClockEditMode = it },
                        isHeaderEditMode = isHeaderEditMode,
                        onHeaderEditModeChange = { isHeaderEditMode = it },
                        onTileTap = { tile ->
                            if (swapSourceTile != null) {
                                if (swapSourceTile!!.tileId != tile.tileId) {
                                    viewModel.moveTile(swapSourceTile!!.tileId, tile.tileId)
                                    hapticPulse()
                                }
                                swapSourceTile = null
                            } else {
                                val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(tile.packageName)
                                if (settings.showMindfulUsage && tile.launchCount >= 3 && isSocial) {
                                    mindfulnessApp = MindfulnessInfo(tile.packageName, tile.label, tile.usageTime, tile.launchCount, tile.componentName)
                                } else if (tile.packageName == context.packageName) {
                                    onLauncherSettingsTap()
                                } else {
                                    launchApp(tile.packageName, tile.componentName)
                                }
                            }
                        },
                        onTileLongPress = { tile ->
                            hapticPulse()
                            if (settings.editModeEnabled) {
                                swapSourceTile = tile
                            } else {
                                appShortcuts = viewModel.getShortcutsForApp(tile.packageName)
                                if (appShortcuts.isNotEmpty()) {
                                    selectedShortcutTile = tile
                                } else {
                                    // Fallback to Edit Mode or Show Toast
                                    Toast.makeText(context, "Long press to Edit enabled in Settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onLauncherSettingsTap = onLauncherSettingsTap,
                        onOpenDrawer = {
                            hapticPulse()
                            showAppDrawerConfirmDialog = true
                        },
                        onBackgroundTap = handleHomeScreenTap,
                        onSwipeDown = {
                            try {
                                val statusbarService = context.getSystemService("statusbar")
                                val statusbarManager = Class.forName("android.app.StatusBarManager")
                                val method = statusbarManager.getMethod("expandNotificationsPanel")
                                method.invoke(statusbarService)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }

        // --- Overlays (Drawer, Dialogs) ---
        if (showAppDrawer) {
            val installedApps = remember { viewModel.getInstalledApps() }
            AppDrawerSheet(
                apps = installedApps,
                suggestedApps = tiles.suggestedApps,
                onDismiss = { showAppDrawer = false },
                onLaunch = { pkg, comp ->
                    val app = installedApps.find { it.packageName == pkg && it.componentName == comp }
                    val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
                    if (app != null && settings.showMindfulUsage && app.launchCount >= 3 && isSocial) {
                        showAppDrawer = false
                        mindfulnessApp = MindfulnessInfo(pkg, app.label, app.usageTime, app.launchCount, comp)
                    } else {
                        showAppDrawer = false
                        launchApp(pkg, comp)
                    }
                }
            )
        }

        if (showAppDrawerConfirmDialog) {
            AppDrawerConfirmDialog(
                openCount = settings.appDrawerOpenCount,
                totalAppOpens = focus.totalAppOpens,
                onDismiss = { showAppDrawerConfirmDialog = false },
                onConfirm = {
                    showAppDrawerConfirmDialog = false
                    viewModel.incrementAppDrawerCount()
                    showAppDrawer = true
                },
                onEmergencyConfirm = {
                    showAppDrawerConfirmDialog = false
                    viewModel.emergencyOpenAppDrawer()
                    showAppDrawer = true
                }
            )
        }

        mindfulnessApp?.let { info ->
            MindfulnessDialog(
                label = info.label,
                usageTime = info.usageTime ?: "0m",
                launchCount = info.launchCount,
                onDismiss = { mindfulnessApp = null },
                onConfirm = {
                    val pkg = info.pkg
                    val comp = info.comp
                    mindfulnessApp = null
                    launchApp(pkg, comp)
                }
            )
        }

        if (showUsageStatsDialog) {
            UsageStatsPermissionDialog(
                onDismiss = { showUsageStatsDialog = false },
                onGoToSettings = {
                    showUsageStatsDialog = false
                    PermissionManager.openUsageAccessSettings(context)
                }
            )
        }

        if (showNotifPermDialog) {
            NotificationPermissionDialog(
                onDismiss = { showNotifPermDialog = false },
                onGoToSettings = {
                    showNotifPermDialog = false
                    PermissionManager.openNotificationListenerSettings(context)
                },
            )
        }

        if (showDefaultLauncherDialog) {
            DefaultLauncherDialog(
                onDismiss = { 
                    showDefaultLauncherDialog = false 
                    hasDismissedDefaultDialog = true
                },
                onGoToSettings = {
                    showDefaultLauncherDialog = false
                    hasDismissedDefaultDialog = true
                    PermissionManager.openDefaultLauncherSettings(context)
                }
            )
        }

        if (showAppAccessDisclosure) {
            AppAccessDisclosureDialog(
                onAccept = {
                    hasAcceptedLocally = true
                    viewModel.acceptAppDisclosure()
                }
            )
        }

        if (showOnboarding) {
            DotzOnboardingSheet(
                onDismiss = {
                    showOnboarding = false
                    viewModel.setOnboardingSeen()
                }
            )
        }

        tileToAssign?.let { tile ->
            UnassignedTileDialog(
                tileLabel = tile.label,
                onDismiss = { tileToAssign = null },
                onSelectApp = {
                    val tId = tileToAssign?.tileId
                    val tLabel = tileToAssign?.label
                    tileToAssign = null
                    context.startActivity(
                        Intent(context, AppSelectionActivity::class.java)
                            .putExtra("tileId", tId)
                            .putExtra("tileLabel", tLabel)
                    )
                },
            )
        }

        showIntentionPause?.let { (pkg, comp) ->
            IntentionPauseOverlay(
                useBiometric = settings.useBiometricPause && settings.isPremium,
                onFinished = {
                    showIntentionPause = null
                    viewModel.launchApp(pkg, comp)
                },
                onCancel = { showIntentionPause = null }
            )
        }

        selectedShortcutTile?.let { tile ->
            AppShortcutDialog(
                tile = tile,
                shortcuts = appShortcuts,
                onDismiss = { selectedShortcutTile = null },
                onShortcutClick = { shortcut ->
                    selectedShortcutTile = null
                    viewModel.launchShortcut(shortcut)
                }
            )
        }

        if (focus.showUltraFocusExitReason) {
            UltraFocusExitDialog(
                onDismiss = viewModel::cancelUltraFocusExit,
                onConfirm = { reason ->
                    viewModel.endUltraFocusSession(reason)
                }
            )
        }

        editingNote?.let { (id, text) ->
            EditNoteDialog(
                initialText = text,
                onDismiss = { editingNote = null },
                onSave = { newText ->
                    editingNote = null
                    viewModel.updateJournalEntry(id, newText)
                }
            )
        }

        focus.weeklyReflection?.let { reflection ->
            WeeklyReflectionDialog(
                reflection = reflection,
                onDismiss = viewModel::dismissWeeklyReflection
            )
        }
    }
}

@Composable
private fun TilesPageContent(
    tiles: com.dotz.launcherpro.viewmodel.TilesState,
    system: com.dotz.launcherpro.viewmodel.SystemHeaderState,
    weather: com.dotz.launcherpro.viewmodel.WeatherState,
    media: com.dotz.launcherpro.viewmodel.MediaState,
    theme: com.dotz.launcherpro.viewmodel.ThemeState,
    focus: com.dotz.launcherpro.viewmodel.FocusState,
    timeline: com.dotz.launcherpro.viewmodel.TimelineState,
    viewModel: LauncherViewModel,
    iconCache: IconCacheManager,
    headerAlpha: Float,
    swapSourceTile: AppTile?,
    isClockEditMode: Boolean,
    onClockEditModeChange: (Boolean) -> Unit,
    isHeaderEditMode: Boolean,
    onHeaderEditModeChange: (Boolean) -> Unit,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    onLauncherSettingsTap: () -> Unit,
    onOpenDrawer: () -> Unit,
    onBackgroundTap: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val settings = theme.settings
    val pages = listOfNotNull(
        tiles.page0Tiles,
        tiles.page1Tiles,
        tiles.page2Tiles.takeIf { it.isNotEmpty() }
    )
    val tilesPagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(tilesPagerState.currentPage) {
        viewModel.setInnerPage(tilesPagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onBackgroundTap() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        // Detect Swipe Down (threshold 100px, mostly vertical)
                        if (dragAmount.y > 20 && abs(dragAmount.y) > abs(dragAmount.x) * 2) {
                            onSwipeDown()
                            change.consume()
                        }
                    }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        // --- Static Header (with alpha for Timeline transition) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f) // Reduced slightly from 0.40
                .graphicsLayer { alpha = headerAlpha }
        ) {
            StaticHeader(
                batteryLevel  = system.battery,
                networkStatus = system.network,
                weatherTemp   = weather.temp,
                weatherCondition = weather.condition,
                weatherFeelsLike = weather.feelsLike,
                weatherAqi = weather.aqi,
                weatherAqiLabel = weather.aqiLabel,
                weatherLow = weather.low,
                weatherHigh = weather.high,
                showWeatherInfo = settings.showWeatherInfo,
                isWifiEnabled = system.wifi,
                isBluetoothEnabled = system.bluetooth,
                isSilentMode = system.silent,
                isTorchOn = system.torch,
                isAirplaneModeOn = system.airplane,
                isDarkModeOn = system.dark,
                ringerMode = system.ringer,
                isMobileDataEnabled = system.mobileData,
                transparency = settings.tileTransparency,
                headerMode = settings.homeHeaderMode,
                clockStyle = settings.clockStyle,
                isEditMode = isClockEditMode,
                onEditModeChange = onClockEditModeChange,
                onClockStyleChange = viewModel::setClockStyle,
                isHeaderEditMode = isHeaderEditMode,
                onHeaderEditModeChange = onHeaderEditModeChange,
                onHeaderModeChange = viewModel::setHomeHeaderMode,
                isPremium = settings.isPremium,
                nowPlayingTitle = media.title,
                nowPlayingArtist = media.artist,
                isPlaying = media.isPlaying,
                playbackPosition = media.position,
                playbackDuration = media.duration,
                unlockCount = focus.unlockCount,
                notificationsReceived = focus.notificationsReceivedToday,
                totalAppOpens = focus.totalAppOpens,
                focusScore = focus.focusScore,
                blockedCount = timeline.blockedNotificationsCount,
                onPlayPause = viewModel::mediaPlayPause,
                onSkipNext = viewModel::mediaSkipNext,
                onSkipPrevious = viewModel::mediaSkipPrevious,
                onMusicClick = { viewModel.launchApp(media.packageName) },
                onLauncherSettingsTap = onLauncherSettingsTap,
                onWifiToggle = viewModel::toggleWifiDirect,
                onBluetoothToggle = viewModel::toggleBluetoothDirect,
                onSilentToggle = viewModel::toggleSilentMode,
                onTorchToggle = viewModel::toggleTorch,
                onAirplaneToggle = viewModel::toggleAirplaneModeDirect,
                onDarkModeToggle = viewModel::toggleDarkModeDirect,
                onDataClick = viewModel::toggleMobileDataDirect,
                onWeatherClick = viewModel::openWeatherApp,
                onWifiLongClick = viewModel::toggleWifi,
                onBluetoothLongClick = viewModel::toggleBluetooth,
                onDataLongClick = viewModel::openMobileDataSettings,
                onAirplaneLongClick = viewModel::toggleAirplaneMode,
                onSilentLongClick = viewModel::toggleSilentMode,
                onTorchLongClick = viewModel::toggleTorch,
                onDarkModeLongClick = viewModel::toggleDarkMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- Inner Tile Pager ---
        Box(
            modifier = Modifier
                .weight(0.62f) // Increased from 0.60
                .pointerInput(settings.verticalScrolling) {
                    // Logic to handle App Drawer gestures
                    var totalX = 0f
                    var totalY = 0f
                    var triggered = false
                    var startInDeadZone = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalX = 0f; totalY = 0f; triggered = false
                            // Ignore gestures starting in the bottom 15% of the screen
                            startInDeadZone = offset.y > size.height * 0.85f
                        },
                        onDrag = { change, dragAmount ->
                            if (!triggered && !startInDeadZone) {
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                
                                val absX = abs(totalX)
                                val absY = abs(totalY)

                                if (settings.verticalScrolling) {
                                    // Swipe LEFT for drawer (threshold 180px, tight slope)
                                    if (absX > 180 && absX > absY * 2.5f && totalX < -180) {
                                        triggered = true; onOpenDrawer(); change.consume()
                                    }
                                } else {
                                    // Swipe UP for drawer (Increased to 220px for Recents safety, tight slope)
                                    if (absY > 220 && absY > absX * 2.5f && totalY < -220) {
                                        triggered = true; onOpenDrawer(); change.consume()
                                    }
                                }
                            }
                            if (triggered) change.consume()
                        }
                    )
                }
        ) {
            if (settings.verticalScrolling) {
                VerticalPager(
                    state = tilesPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(settings, pages[pageIndex], iconCache, swapSourceTile?.tileId, onTileTap, onTileLongPress)
                }
            } else {
                HorizontalPager(
                    state = tilesPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(settings, pages[pageIndex], iconCache, swapSourceTile?.tileId, onTileTap, onTileLongPress)
                }
            }
        }
    }
}

@Composable
private fun TimelinePageContent(
    timeline: com.dotz.launcherpro.viewmodel.TimelineState,
    focus: com.dotz.launcherpro.viewmodel.FocusState,
    theme: com.dotz.launcherpro.viewmodel.ThemeState,
    media: com.dotz.launcherpro.viewmodel.MediaState,
    viewModel: LauncherViewModel,
    onEditJournal: (String, String) -> Unit,
    onGoToPremium: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val textColor = DotzTheme.colors.text
    val settings = theme.settings
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "TIMELINE",
                style = DotzType.dateStyle().copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = textColor
            )
            
            Text(
                text = focus.focusTimeToday.uppercase(),
                style = DotzType.dateStyle().copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = textColor.copy(alpha = 0.4f)
            )
        }

        // --- Quick Capture ---
        QuickCaptureHeader(onSave = viewModel::addJournalEntry)

        // --- Focus History ---
        FocusHistoryChart(
            history = focus.focusScoreHistory,
            hasPermission = focus.hasUsageStatsPermission,
            onEnablePermission = {
                PermissionManager.openUsageAccessSettings(context)
            }
        )

        // --- PRO Sale Banner ---
        if (!theme.isPremium && theme.isUpgradeAvailable) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onGoToPremium() },
                color = DotzTheme.colors.accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DotzTheme.colors.accent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star, 
                        null, 
                        tint = DotzTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Limited Flash Sale", 
                            color = textColor, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Get Dotz PRO at 50% OFF", 
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        "50% OFF", 
                        color = DotzTheme.colors.accent, 
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // --- Update Banner ---
        if (theme.isUpdateAvailable) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { 
                        (context as? androidx.activity.ComponentActivity)?.let {
                            viewModel.startUpdateFlow(it)
                        }
                    },
                color = DotzTheme.colors.accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DotzTheme.colors.accent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Update, 
                        null, 
                        tint = DotzTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Update Available", 
                            color = textColor, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "A new version of Dotz is ready.", 
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        "UPDATE", 
                        color = DotzTheme.colors.accent, 
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // --- Content with Vertical Line ---
        Box(modifier = Modifier.fillMaxWidth()) {
            // The Vertical Timeline Line
            if (timeline.timelineItems.isNotEmpty() || timeline.upcomingEvents.isNotEmpty() || timeline.nextAlarm != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .align(Alignment.TopStart)
                        .drawBehind {
                            drawLine(
                                color = textColor.copy(alpha = 0.15f),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // --- Upcoming Section ---
                if (timeline.upcomingEvents.isNotEmpty() || timeline.nextAlarm != null) {
                    TimelineSectionWrapper(textColor) {
                        Text(
                            text = "UPCOMING",
                            style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textColor.copy(alpha = 0.3f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        timeline.nextAlarm?.let { alarm ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                Icon(Icons.Default.NotificationsActive, null, tint = DotzTheme.colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = "Next Alarm: $alarm", style = MaterialTheme.typography.bodyLarge, color = textColor)
                            }
                        }

                        timeline.upcomingEvents.take(3).forEach { event ->
                            UpcomingEventCard(event = event)
                        }
                    }
                }

                // --- Notifications ---
                if (timeline.timelineItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No active notifications.\nYour digital space is clear.",
                            color = textColor.copy(alpha = 0.2f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    timeline.timelineItems.forEach { item ->
                        TimelineSectionWrapper(textColor) {
                            TimelineCard(
                                item = item,
                                onItemClick = { pkg, comp -> pkg?.let { viewModel.launchApp(it, comp) } },
                                onPlayPause = viewModel::mediaPlayPause,
                                onSkipNext = viewModel::mediaSkipNext,
                                onSkipPrevious = viewModel::mediaSkipPrevious,
                                onReply = viewModel::sendReply,
                                onDeleteJournal = viewModel::deleteJournalEntry,
                                onEditJournal = onEditJournal,
                                isPlaying = media.isPlaying
                            )
                        }
                    }
                }
            }
        }
    }

    // Weekly Reflection Dialog
    if (focus.weeklyReflection != null) {
        WeeklyReflectionDialog(
            reflection = focus.weeklyReflection,
            onDismiss = viewModel::dismissWeeklyReflection
        )
    }
}

@Composable
private fun TimelineSectionWrapper(
    lineColor: Color,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Dot on the line
        Box(
            modifier = Modifier
                .width(24.dp)
                .padding(top = 28.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(lineColor.copy(alpha = 0.3f))
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun PagerContent(
    settings: com.dotz.launcherpro.data.DotzSettings,
    tiles: List<AppTile>,
    iconCache: IconCacheManager,
    highlightedTileId: Int?,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit
) {
    if (settings.layoutStyle == "list") {
        AppList(
            tiles = tiles,
            iconCache = iconCache,
            grayscale = settings.grayscaleMode,
            iconPackPackage = settings.iconPackPackage,
            showBadges = settings.showNotificationDots,
            transparency = settings.tileTransparency,
            highlightedTileId = highlightedTileId,
            onTileTap = onTileTap,
            onTileLongPress = onTileLongPress,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        AppGrid(
            tiles = tiles,
            iconCache = iconCache,
            grayscale = settings.grayscaleMode,
            iconPackPackage = settings.iconPackPackage,
            showBadges = settings.showNotificationDots,
            transparency = settings.tileTransparency,
            highlightedTileId = highlightedTileId,
            onTileTap = onTileTap,
            onTileLongPress = onTileLongPress,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun QuickCaptureHeader(onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val textColor = DotzTheme.colors.text
    
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Quick note...", color = textColor.copy(alpha = 0.3f), fontSize = 14.sp) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onSave(text)
                        text = ""
                    }
                }
            ),
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSave(text)
                            text = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, null, tint = textColor)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = textColor.copy(alpha = 0.3f),
                unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                cursorColor = textColor
            )
        )
    }
}

@Composable
private fun AppShortcutDialog(
    tile: AppTile,
    shortcuts: List<AppShortcut>,
    onDismiss: () -> Unit,
    onShortcutClick: (AppShortcut) -> Unit
) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = tile.label,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shortcuts.forEach { shortcut ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onShortcutClick(shortcut) },
                        color = textColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Launch,
                                null,
                                tint = textColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(shortcut.label, color = textColor, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButtonText = "CLOSE",
        onConfirm = onDismiss
    )
}

@Composable
private fun EditNoteDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val textColor = DotzTheme.colors.text
    
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Edit Note",
        content = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Write something...", fontSize = 14.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textColor.copy(alpha = 0.3f),
                    unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                    cursorColor = textColor
                )
            )
        },
        confirmButtonText = "SAVE",
        confirmButtonEnabled = text.isNotBlank(),
        onConfirm = { onSave(text) },
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}
