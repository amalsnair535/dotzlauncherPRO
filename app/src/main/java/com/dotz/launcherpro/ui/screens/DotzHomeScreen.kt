@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.manager.PermissionManager
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import com.dotz.launcherpro.ui.theme.blend
import com.dotz.launcherpro.viewmodel.LauncherUiState
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun DotzHomeScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    onLauncherSettingsTap: () -> Unit
) {
    val context = LocalContext.current
    val iconCache = viewModel.iconCache

    // --- Migrated Fragment Logic (Dialogs & State) ---
    var showNotifPermDialog by remember { mutableStateOf(false) }
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
    var showIntentionPause by remember { mutableStateOf<String?>(null) }

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
        if (currentTime - lastTapTime < 500) {
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

    // --- Liquid Glass Animation (Optimized) ---
    val isGlass = DotzTheme.colors.isGlass
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlass")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Offset"
    )

    val launchApp = { pkg: String ->
        val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
        if (isSocial && showIntentionPause == null) {
            showIntentionPause = pkg
        } else {
            val success = viewModel.launchApp(pkg)
            if (!success) {
                // Handle unassigned
                tileToAssign = (uiState.page0Tiles + uiState.page1Tiles + uiState.page2Tiles).find { it.packageName == pkg }
                if (tileToAssign == null && pkg.isNotBlank()) {
                    Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(uiState.isLoaded, uiState.settings.hasAcceptedAppDisclosure, hasAcceptedLocally, uiState.settings.hasSeenOnboarding) {
        if (uiState.isLoaded && uiState.settings.hasSeenOnboarding && !uiState.settings.hasAcceptedAppDisclosure && !hasAcceptedLocally) {
            showAppAccessDisclosure = true
        } else {
            showAppAccessDisclosure = false
        }
    }

    LaunchedEffect(uiState.settings.hasSeenOnboarding) {
        if (uiState.settings.hasSeenOnboarding) {
            val isNotifEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
            if (!isNotifEnabled) showNotifPermDialog = true
            if (uiState.settings.showMindfulUsage && !uiState.hasUsageStatsPermission) showUsageStatsDialog = true
        }
    }

    LaunchedEffect(uiState.isDefaultLauncher, uiState.settings.hasSeenOnboarding) {
        if (uiState.settings.hasSeenOnboarding && !uiState.isDefaultLauncher && !hasDismissedDefaultDialog) {
            showDefaultLauncherDialog = true
        }
    }

    LaunchedEffect(uiState.isLoaded, uiState.settings.hasSeenOnboarding) {
        if (uiState.isLoaded && !uiState.settings.hasSeenOnboarding) {
            showOnboarding = true
        }
    }

    // --- Root Pager Setup (Timeline <-> Tiles) ---
    val rootPagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    
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
        uiState.settings.showWallpaper -> {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
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
                    start = androidx.compose.ui.geometry.Offset(animOffset, animOffset),
                    end = androidx.compose.ui.geometry.Offset(animOffset + 1000f, animOffset + 1000f),
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
        if (uiState.settings.layoutStyle == "ultra_focus") {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTime(context),
                            style = DotzType.timeStyle().copy(fontSize = 36.sp, fontWeight = FontWeight.Light),
                            color = DotzTheme.colors.text
                        )
                        Text(
                            text = currentDate(),
                            style = DotzType.dateStyle().copy(fontSize = 11.sp, letterSpacing = 2.sp),
                            color = DotzTheme.colors.text.copy(alpha = 0.5f)
                        )
                    }
                }
                
                UltraFocusLayout(
                    tiles = uiState.ultraFocusTiles,
                    remainingMillis = uiState.ultraFocusRemainingMillis,
                    onTileTap = { tile ->
                        if (tile.packageName == context.packageName) onLauncherSettingsTap()
                        else launchApp(tile.packageName)
                    },
                    onSelectApps = {
                        context.startActivity(Intent(context, com.dotz.launcherpro.ui.screens.UltraFocusAppSelectionActivity::class.java))
                    },
                    onEndSession = viewModel::requestUltraFocusExit,
                    modifier = Modifier.weight(0.75f)
                )
            }
        } else {
            HorizontalPager(
                state = rootPagerState,
                modifier = Modifier.fillMaxSize().semantics { contentDescription = rootPagerDescription },
                userScrollEnabled = uiState.settings.enableTimeline
            ) { rootPageIndex ->
                if (rootPageIndex == 0) {
                    // Page 0: Timeline
                    TimelinePageContent(uiState, viewModel) { mindfulnessApp = it }
                } else {
                    // Page 1: Tiles & Header
                    TilesPageContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        iconCache = iconCache,
                        headerAlpha = headerAlpha,
                        swapSourceTile = swapSourceTile,
                        onTileTap = { tile ->
                            if (swapSourceTile != null) {
                                if (swapSourceTile!!.tileId != tile.tileId) {
                                    viewModel.moveTile(swapSourceTile!!.tileId, tile.tileId)
                                    hapticPulse()
                                }
                                swapSourceTile = null
                            } else {
                                val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(tile.packageName)
                                if (uiState.settings.showMindfulUsage && tile.launchCount >= 3 && isSocial) {
                                    mindfulnessApp = MindfulnessInfo(tile.packageName, tile.label, tile.usageTime, tile.launchCount)
                                } else if (tile.packageName == context.packageName) {
                                    onLauncherSettingsTap()
                                } else {
                                    launchApp(tile.packageName)
                                }
                            }
                        },
                        onTileLongPress = { tile ->
                            hapticPulse()
                            swapSourceTile = tile
                        },
                        onLauncherSettingsTap = onLauncherSettingsTap,
                        onOpenDrawer = {
                            hapticPulse()
                            showAppDrawerConfirmDialog = true
                        },
                        onBackgroundTap = handleHomeScreenTap
                    )
                }
            }
        }

        // --- Overlays (Drawer, Dialogs) ---
        if (showAppDrawer) {
            val installedApps = remember { viewModel.getInstalledApps() }
            AppDrawerSheet(
                apps = installedApps,
                onDismiss = { showAppDrawer = false },
                onLaunch = { pkg ->
                    val app = installedApps.find { it.packageName == pkg }
                    val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
                    if (app != null && uiState.settings.showMindfulUsage && app.launchCount >= 3 && isSocial) {
                        showAppDrawer = false
                        mindfulnessApp = MindfulnessInfo(pkg, app.label, app.usageTime, app.launchCount)
                    } else {
                        showAppDrawer = false
                        launchApp(pkg)
                    }
                }
            )
        }

        if (showAppDrawerConfirmDialog) {
            AppDrawerConfirmDialog(
                openCount = uiState.settings.appDrawerOpenCount,
                totalAppOpens = uiState.totalAppOpens,
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
                    mindfulnessApp = null
                    launchApp(pkg)
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
                        Intent(context, com.dotz.launcherpro.ui.screens.AppSelectionActivity::class.java)
                            .putExtra("tileId", tId)
                            .putExtra("tileLabel", tLabel)
                    )
                },
            )
        }

        showIntentionPause?.let { pkg ->
            IntentionPauseOverlay(
                onFinished = {
                    showIntentionPause = null
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                onCancel = { showIntentionPause = null }
            )
        }

        if (uiState.showUltraFocusExitReason) {
            UltraFocusExitDialog(
                onDismiss = viewModel::cancelUltraFocusExit,
                onConfirm = { reason ->
                    viewModel.endUltraFocusSession(reason)
                }
            )
        }
    }
}

@Composable
private fun TilesPageContent(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    iconCache: IconCacheManager,
    headerAlpha: Float,
    swapSourceTile: AppTile?,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    onLauncherSettingsTap: () -> Unit,
    onOpenDrawer: () -> Unit,
    onBackgroundTap: () -> Unit
) {
    val pages = listOfNotNull(
        uiState.page0Tiles,
        uiState.page1Tiles,
        uiState.page2Tiles.takeIf { it.isNotEmpty() }
    )
    val tilesPagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(tilesPagerState.currentPage) {
        viewModel.setInnerPage(tilesPagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onBackgroundTap() })
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
                batteryLevel  = uiState.batteryLevel,
                networkStatus = uiState.networkStatus,
                weatherTemp   = uiState.weatherTemp,
                weatherCondition = uiState.weatherCondition,
                weatherFeelsLike = uiState.weatherFeelsLike,
                weatherSummary = uiState.weatherSummary,
                weatherAqi = uiState.weatherAqi,
                weatherAqiLabel = uiState.weatherAqiLabel,
                weatherLow = uiState.weatherLow,
                weatherHigh = uiState.weatherHigh,
                showWeatherInfo = uiState.settings.showWeatherInfo,
                isWifiEnabled = uiState.isWifiEnabled,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isSilentMode = uiState.isSilentMode,
                isTorchOn = uiState.isTorchOn,
                isAirplaneModeOn = uiState.isAirplaneModeOn,
                isDarkModeOn = uiState.isDarkModeOn,
                ringerMode = uiState.ringerMode,
                isMobileDataEnabled = uiState.isMobileDataEnabled,
                transparency = uiState.settings.tileTransparency,
                headerMode = uiState.settings.homeHeaderMode,
                nowPlayingTitle = uiState.nowPlayingTitle,
                nowPlayingArtist = uiState.nowPlayingArtist,
                isPlaying = uiState.isPlaying,
                playbackPosition = uiState.playbackPosition,
                playbackDuration = uiState.playbackDuration,
                unlockCount = uiState.unlockCount,
                notificationsReceived = uiState.notificationsReceivedToday,
                totalAppOpens = uiState.totalAppOpens,
                focusScore = uiState.focusScore,
                onPlayPause = viewModel::mediaPlayPause,
                onSkipNext = viewModel::mediaSkipNext,
                onSkipPrevious = viewModel::mediaSkipPrevious,
                onMusicClick = { viewModel.launchApp(uiState.nowPlayingPackage) },
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
                .pointerInput(uiState.settings.verticalScrolling) {
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

                                if (uiState.settings.verticalScrolling) {
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
            if (uiState.settings.verticalScrolling) {
                VerticalPager(
                    state = tilesPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(uiState, pages[pageIndex], iconCache, swapSourceTile?.tileId, onTileTap, onTileLongPress)
                }
            } else {
                HorizontalPager(
                    state = tilesPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(uiState, pages[pageIndex], iconCache, swapSourceTile?.tileId, onTileTap, onTileLongPress)
                }
            }
        }
    }
}

@Composable
private fun TimelinePageContent(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    onMindfulLaunch: (MindfulnessInfo) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val textColor = DotzTheme.colors.text
    
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "TIMELINE",
                style = DotzType.dateStyle().copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = textColor
            )
            
            Text(
                text = uiState.focusTimeToday.uppercase(),
                style = DotzType.dateStyle().copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = textColor.copy(alpha = 0.4f)
            )
        }

        // --- Focus History ---
        FocusHistoryChart(
            history = uiState.focusScoreHistory,
            hasPermission = uiState.hasUsageStatsPermission,
            onEnablePermission = {
                PermissionManager.openUsageAccessSettings(context)
            }
        )

        // --- Content with Vertical Line ---
        Box(modifier = Modifier.fillMaxWidth()) {
            // The Vertical Timeline Line
            if (uiState.timelineItems.isNotEmpty() || uiState.upcomingEvents.isNotEmpty() || uiState.nextAlarm != null) {
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
                if (uiState.upcomingEvents.isNotEmpty() || uiState.nextAlarm != null) {
                    TimelineSectionWrapper(textColor) {
                        Text(
                            text = "UPCOMING",
                            style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = textColor.copy(alpha = 0.3f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        uiState.nextAlarm?.let { alarm ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                Icon(Icons.Default.NotificationsActive, null, tint = DotzTheme.colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = "Next Alarm: $alarm", style = MaterialTheme.typography.bodyLarge, color = textColor)
                            }
                        }

                        uiState.upcomingEvents.take(3).forEach { event ->
                            UpcomingEventCard(event = event)
                        }
                    }
                }

                // --- Notifications ---
                if (uiState.timelineItems.isEmpty()) {
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
                    uiState.timelineItems.forEach { item ->
                        TimelineSectionWrapper(textColor) {
                            TimelineCard(
                                item = item,
                                onItemClick = { pkg -> if (pkg != null) viewModel.launchApp(pkg) },
                                onPlayPause = viewModel::mediaPlayPause,
                                onSkipNext = viewModel::mediaSkipNext,
                                onSkipPrevious = viewModel::mediaSkipPrevious,
                                onReply = viewModel::sendReply,
                                isPlaying = uiState.isPlaying
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(80.dp))
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
    uiState: LauncherUiState,
    tiles: List<AppTile>,
    iconCache: IconCacheManager,
    highlightedTileId: Int?,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit
) {
    if (uiState.settings.layoutStyle == "list") {
        AppList(
            tiles = tiles,
            iconCache = iconCache,
            grayscale = uiState.settings.grayscaleMode,
            iconPackPackage = uiState.settings.iconPackPackage,
            showBadges = uiState.settings.showNotificationDots,
            transparency = uiState.settings.tileTransparency,
            highlightedTileId = highlightedTileId,
            onTileTap = onTileTap,
            onTileLongPress = onTileLongPress,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        AppGrid(
            tiles = tiles,
            iconCache = iconCache,
            grayscale = uiState.settings.grayscaleMode,
            iconPackPackage = uiState.settings.iconPackPackage,
            showBadges = uiState.settings.showNotificationDots,
            transparency = uiState.settings.tileTransparency,
            highlightedTileId = highlightedTileId,
            onTileTap = onTileTap,
            onTileLongPress = onTileLongPress,
            modifier = Modifier.fillMaxSize()
        )
    }
}
