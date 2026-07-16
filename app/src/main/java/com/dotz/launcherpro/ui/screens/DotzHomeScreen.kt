@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.dotz.launcherpro.data.FastlaneEvent
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
        vibrator?.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
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
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Handle unassigned
                tileToAssign = (uiState.page0Tiles + uiState.page1Tiles + uiState.page2Tiles).find { it.packageName == pkg }
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
                    tiles = uiState.page0Tiles + uiState.page1Tiles + uiState.page2Tiles,
                    remainingMillis = uiState.ultraFocusRemainingMillis,
                    onTileTap = { tile ->
                        if (tile.packageName == context.packageName) onLauncherSettingsTap()
                        else launchApp(tile.packageName)
                    },
                    onEndSession = viewModel::endUltraFocusSession,
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

        if (showUsageStatsDialog) {
            UsageStatsPermissionDialog(
                onDismiss = { showUsageStatsDialog = false },
                onGoToSettings = {
                    showUsageStatsDialog = false
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
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
    }
}

@Composable
fun IntentionPauseOverlay(onFinished: () -> Unit, onCancel: () -> Unit) {
    var timeLeft by remember { mutableStateOf(3) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Wait a moment...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Do you really need to open this app right now?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            Text(
                "$timeLeft",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(64.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = CircleShape
            ) {
                Text("NEVER MIND", color = Color.White)
            }
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
    onOpenDrawer: () -> Unit
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 24.dp) // Added breathing room at the bottom
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
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // --- Header Section ---
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = currentDate(),
                style = DotzType.dateStyle().copy(fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black),
                color = DotzTheme.colors.accent.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TODAY",
                style = DotzType.timeStyle().copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
                color = DotzTheme.colors.text
            )
        }

        // --- AI Daily Summary ---
        AISummaryCard(summary = uiState.dailySummary)

        // --- Smart Suggestions ---
        if (uiState.suggestedApps.isNotEmpty()) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "SUGGESTED",
                    style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = DotzTheme.colors.text.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.suggestedApps.take(3).forEach { app ->
                        AssistChip(
                            onClick = { viewModel.launchApp(app.packageName) },
                            label = { Text(app.label.uppercase(), fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(12.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = DotzTheme.colors.text
                            )
                        )
                    }
                }
            }
        }

        // --- Search & Filters ---
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { 
                    searchText = it
                    viewModel.setTimelineSearchQuery(it) 
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search history...", color = DotzTheme.colors.text.copy(alpha = 0.3f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = DotzTheme.colors.text.copy(alpha = 0.3f)) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.2f),
                    unfocusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.05f)
                )
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.dotz.launcherpro.data.FastlaneType.values().take(6).forEach { type ->
                    AssistChip(
                        onClick = { viewModel.setTimelineFilter(type) },
                        label = { Text(type.name.replace("_", " "), fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = DotzTheme.colors.text.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, DotzTheme.colors.text.copy(alpha = 0.1f))
                    )
                }
            }
        }

        // --- Upcoming Section ---
        if (uiState.upcomingEvents.isNotEmpty() || uiState.nextAlarm != null) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "UPCOMING",
                    style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = DotzTheme.colors.text.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                uiState.nextAlarm?.let { alarm ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(Icons.Default.NotificationsActive, null, tint = DotzTheme.colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Next Alarm: $alarm", style = MaterialTheme.typography.bodyLarge, color = DotzTheme.colors.text)
                    }
                }

                uiState.upcomingEvents.take(3).forEach { event ->
                    UpcomingEventCard(event = event)
                }
            }
        }

        // --- Active Widgets (Optional Peek) ---
        if (uiState.isPlaying) {
            Text(
                "NOW PLAYING", 
                style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = DotzTheme.colors.text.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TimelineCard(
                item = com.dotz.launcherpro.data.TimelineItem(
                    id = "current_music",
                    type = com.dotz.launcherpro.data.TimelineType.MUSIC,
                    title = uiState.nowPlayingTitle,
                    subtitle = uiState.nowPlayingArtist,
                    timestamp = System.currentTimeMillis(),
                    packageName = ""
                ),
                onItemClick = {},
                onPlayPause = viewModel::mediaPlayPause,
                onSkipNext = viewModel::mediaSkipNext,
                onSkipPrevious = viewModel::mediaSkipPrevious,
                isPlaying = uiState.isPlaying,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // --- The Stream ---
        Text(
            "ACTIVITY", 
            style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = DotzTheme.colors.text.copy(alpha = 0.3f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.fastlaneStream.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your digital journey starts here.\nActivity will appear as you use your device.",
                    color = DotzTheme.colors.text.copy(alpha = 0.2f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val sdf = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
            var lastDateLabel = ""

            uiState.fastlaneStream.forEach { event ->
                val dateLabel = sdf.format(Date(event.timestamp))
                if (dateLabel != lastDateLabel) {
                    Text(
                        text = dateLabel.uppercase(),
                        style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = DotzTheme.colors.text.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                    )
                    lastDateLabel = dateLabel
                }

                FastlaneTypographyCard(
                    event = event,
                    onAction = {
                        if (event.packageName != null) {
                            viewModel.launchApp(event.packageName)
                        }
                    }
                )
            }
        }
        
        Spacer(Modifier.height(80.dp))
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

@Composable
private fun AppDrawerConfirmDialog(
    openCount: Int, 
    totalAppOpens: Int,
    onDismiss: () -> Unit, 
    onConfirm: () -> Unit,
    onEmergencyConfirm: () -> Unit
) {
    val remaining = (5 - openCount).coerceAtLeast(0)
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = if (remaining > 0) "Open All Apps?" else "App Drawer Locked",
        content = {
            Column {
                if (remaining > 0) {
                    Text("Are you sure you want to open all apps?", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("This gesture is for emergency access only.", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("You have used all 5 daily app drawer opens.", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Opening it now will penalize your Focus Score by 10 points.", color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("REMAINING TODAY: $remaining/5", color = if (remaining > 0) textColor else Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (remaining == 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("TOTAL APP OPENS: $totalAppOpens", color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButtonText = if (remaining > 0) "OPEN" else "EMERGENCY ACCESS",
        onConfirm = { if (remaining > 0) onConfirm() else onEmergencyConfirm() },
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}

@Composable
private fun UsageStatsPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mindful Usage Disclosure",
        content = { 
            Text(
                "Dotz Launcher uses anonymized usage statistics to track your screen time and device unlocks. " +
                "This information is processed only on your device to calculate your Focus Score and enable app usage limits. " +
                "No usage data is ever collected or transmitted.", 
                color = textColor.copy(alpha = 0.7f)
            ) 
        },
        confirmButtonText = "ENABLE",
        onConfirm = onGoToSettings,
        dismissButtonText = "NOT NOW",
        onDismiss = onDismiss
    )
}

@Composable
private fun NotificationPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Enable Notifications",
        content = { Text("Allow Dotz to read notifications.", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "ENABLE",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
private fun DefaultLauncherDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Set as Default Launcher",
        content = { Text("Use Dotz as your main home screen.", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "SET DEFAULT",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
private fun AppAccessDisclosureDialog(onAccept: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = { },
        title = "App Visibility Disclosure",
        content = { 
            Text(
                "To function as a home screen, Dotz Launcher requires access to your list of installed applications. " +
                "This allows you to assign apps to tiles and use the App Drawer. " +
                "This data is used only to provide core launcher functionality and is never collected or shared.", 
                color = textColor.copy(alpha = 0.7f)
            ) 
        },
        confirmButtonText = "I UNDERSTAND",
        onConfirm = onAccept
    )
}

@Composable
private fun UnassignedTileDialog(tileLabel: String, onDismiss: () -> Unit, onSelectApp: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Unassigned Tile",
        content = { Text("Assign an app to $tileLabel?", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "SELECT APP",
        onConfirm = onSelectApp,
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}
