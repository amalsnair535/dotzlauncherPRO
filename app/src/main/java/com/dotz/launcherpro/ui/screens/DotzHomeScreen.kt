@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherUiState
import com.dotz.launcherpro.viewmodel.LauncherViewModel
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

    val hapticPulse = {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    val launchApp = { pkg: String ->
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            // Handle unassigned
            tileToAssign = (uiState.page0Tiles + uiState.page1Tiles + uiState.page2Tiles).find { it.packageName == pkg }
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
            if (uiState.settings.showMindfulUsage && !viewModel.hasUsageStatsPermission()) showUsageStatsDialog = true
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

    // --- Root Pager Setup (Fastlane <-> Tiles) ---
    val rootPagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    
    // Sync viewmodel state for fastlane visibility
    LaunchedEffect(rootPagerState.currentPage) {
        viewModel.setFastlaneVisible(rootPagerState.currentPage == 0)
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

    val backgroundModifier = if (uiState.settings.showWallpaper) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                endY = 400f
            )
        )
    } else {
        Modifier.background(DotzTheme.colors.background)
    }

    Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
        HorizontalPager(
            state = rootPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = uiState.settings.enableFastlane
        ) { rootPageIndex ->
            if (rootPageIndex == 0) {
                // Page 0: Fastlane
                FastlanePageContent(uiState, viewModel) { mindfulnessApp = it }
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
                onDismiss = { showAppDrawerConfirmDialog = false },
                onConfirm = {
                    showAppDrawerConfirmDialog = false
                    if (uiState.settings.appDrawerOpenCount < 5) {
                        viewModel.incrementAppDrawerCount()
                        showAppDrawer = true
                    } else {
                        Toast.makeText(context, "Daily limit reached (5/5).", Toast.LENGTH_SHORT).show()
                    }
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

        if (showNotifPermDialog) {
            NotificationPermissionDialog(
                onDismiss = { showNotifPermDialog = false },
                onGoToSettings = {
                    showNotifPermDialog = false
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
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
                    viewModel.openDefaultLauncherSettings()
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
        // --- Static Header (with alpha for Fastlane transition) ---
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
                showWeatherInfo = uiState.settings.showWeatherInfo,
                isWifiEnabled = uiState.isWifiEnabled,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isSilentMode = uiState.isSilentMode,
                isTorchOn = uiState.isTorchOn,
                isAirplaneModeOn = uiState.isAirplaneModeOn,
                isDarkModeOn = uiState.isDarkModeOn,
                transparency = uiState.settings.tileTransparency,
                headerMode = uiState.settings.homeHeaderMode,
                nowPlayingTitle = uiState.nowPlayingTitle,
                nowPlayingArtist = uiState.nowPlayingArtist,
                isPlaying = uiState.isPlaying,
                playbackPosition = uiState.playbackPosition,
                playbackDuration = uiState.playbackDuration,
                unlockCount = uiState.unlockCount,
                notificationsReceived = uiState.notificationsReceivedToday,
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
private fun FastlanePageContent(
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
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "FASTLANE",
                color = DotzTheme.colors.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }

        if (uiState.timelineItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recent activity to show in Fastlane.",
                    color = DotzTheme.colors.text.copy(alpha = 0.2f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            uiState.timelineItems.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .weight(1f)
                                .background(if (index == 0) Color.Transparent else DotzTheme.colors.text.copy(alpha = 0.1f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DotzTheme.colors.text.copy(alpha = 0.2f))
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .weight(1f)
                                .background(if (index == uiState.timelineItems.size - 1) Color.Transparent else DotzTheme.colors.text.copy(alpha = 0.1f))
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TimelineCard(
                        item = item,
                        onItemClick = { pkg -> 
                            if (item.type == com.dotz.launcherpro.data.TimelineType.SPONSORED) {
                                viewModel.acknowledgeSponsoredAd()
                                viewModel.launchApp(pkg) // Open the sponsored app/link
                                return@TimelineCard
                            }
                            if (pkg != null) {
                                val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
                                val app = uiState.topApps.find { it.packageName == pkg }
                                if (uiState.settings.showMindfulUsage && (app?.launchCount ?: 0) >= 3 && isSocial) {
                                    onMindfulLaunch(MindfulnessInfo(pkg, app?.label ?: pkg, app?.usageTime, app?.launchCount ?: 0))
                                } else {
                                    viewModel.launchApp(pkg)
                                }
                            }
                        },
                        onPlayPause = viewModel::mediaPlayPause,
                        onSkipNext = viewModel::mediaSkipNext,
                        onSkipPrevious = viewModel::mediaSkipPrevious,
                        onReply = viewModel::sendReply,
                        isPlaying = uiState.isPlaying,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(48.dp))
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
private fun AppDrawerConfirmDialog(openCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val remaining = (5 - openCount).coerceAtLeast(0)
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Open All Apps?",
        content = {
            Column {
                Text("Are you sure you want to open all apps?", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("This gesture is for emergency access only.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("REMAINING TODAY: $remaining/5", color = if (remaining > 0) Color.White else Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        confirmButtonText = if (remaining > 0) "OPEN" else "LOCKED",
        onConfirm = { if (remaining > 0) onConfirm() else onDismiss() },
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}

@Composable
private fun UsageStatsPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mindful Usage Disclosure",
        content = { 
            Text(
                "Dotz Launcher uses anonymized usage statistics to track your screen time and device unlocks. " +
                "This information is processed only on your device to calculate your Focus Score and enable app usage limits. " +
                "No usage data is ever collected or transmitted.", 
                color = Color.White.copy(alpha = 0.7f)
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
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Enable Notifications",
        content = { Text("Allow Dotz to read notifications.", color = Color.White.copy(alpha = 0.7f)) },
        confirmButtonText = "ENABLE",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
private fun DefaultLauncherDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Set as Default Launcher",
        content = { Text("Use Dotz as your main home screen.", color = Color.White.copy(alpha = 0.7f)) },
        confirmButtonText = "SET DEFAULT",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
private fun AppAccessDisclosureDialog(onAccept: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = { },
        title = "App Visibility Disclosure",
        content = { 
            Text(
                "To function as a home screen, Dotz Launcher requires access to your list of installed applications. " +
                "This allows you to assign apps to tiles and use the App Drawer. " +
                "This data is used only to provide core launcher functionality and is never collected or shared.", 
                color = Color.White.copy(alpha = 0.7f)
            ) 
        },
        confirmButtonText = "I UNDERSTAND",
        onConfirm = onAccept
    )
}

@Composable
private fun UnassignedTileDialog(tileLabel: String, onDismiss: () -> Unit, onSelectApp: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Unassigned Tile",
        content = { Text("Assign an app to $tileLabel?", color = Color.White.copy(alpha = 0.7f)) },
        confirmButtonText = "SELECT APP",
        onConfirm = onSelectApp,
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}
