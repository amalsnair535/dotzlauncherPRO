@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.components.AppGrid
import com.dotz.launcherpro.ui.components.AppList
import com.dotz.launcherpro.ui.components.StaticHeader
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherUiState
import kotlin.math.abs

@Composable
fun DotzHomeScreen(
    uiState: LauncherUiState,
    iconCache: IconCacheManager,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    onLauncherSettingsTap: () -> Unit,
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onDataClick: () -> Unit,
    onWeatherClick: () -> Unit,
    onWifiLongClick: () -> Unit = {},
    onBluetoothLongClick: () -> Unit = {},
    onDataLongClick: () -> Unit = {},
    onAirplaneLongClick: () -> Unit = {},
    onSilentLongClick: () -> Unit = {},
    onTorchLongClick: () -> Unit = {},
    onDarkModeLongClick: () -> Unit = {},
    onPageChanged: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    highlightedTileId: Int? = null
) {
    val pages = listOfNotNull(
        uiState.page0Tiles,
        uiState.page1Tiles,
        uiState.page2Tiles.takeIf { it.isNotEmpty() }
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(backgroundModifier)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(uiState.settings.verticalScrolling, uiState.isFastlaneVisible) {
                if (uiState.isFastlaneVisible) return@pointerInput

                var totalDragX = 0f
                var totalDragY = 0f
                var hasTriggered = false
                var isStartInDeadZone = false
                
                detectDragGestures(
                    onDragStart = { offset ->
                        totalDragX = 0f
                        totalDragY = 0f
                        hasTriggered = false
                        // Avoid system gesture conflict
                        isStartInDeadZone = offset.y > size.height * 0.85f
                    },
                    onDrag = { change, dragAmount ->
                        if (!hasTriggered && !isStartInDeadZone) {
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (uiState.settings.verticalScrolling) {
                                // Vertical mode: Swipe LEFT (negative X) for drawer
                                // Use a high slope check to ensure it's primarily horizontal
                                if (totalDragX < -150 && abs(totalDragX) > abs(totalDragY) * 2f) {
                                    hasTriggered = true
                                    onOpenDrawer()
                                    change.consume()
                                }
                            } else {
                                // Horizontal mode: Swipe UP (negative Y) for drawer
                                if (totalDragY < -150 && abs(totalDragY) > abs(totalDragX) * 2f) {
                                    hasTriggered = true
                                    onOpenDrawer()
                                    change.consume()
                                }
                            }
                        }
                        
                        // We ONLY consume the touch if we've successfully triggered the drawer action.
                        // This allows natural horizontal swiping (for Dashboard) to be handled 
                        // by the parent ViewPager2 when we aren't opening the drawer.
                        if (hasTriggered) {
                            change.consume()
                        }
                    }
                )
            }
            .padding(bottom = 24.dp)
    ) {
        // Fixed Header
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
            focusScore = uiState.focusScore,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onLauncherSettingsTap = onLauncherSettingsTap,
            onWifiToggle = onWifiToggle,
            onBluetoothToggle = onBluetoothToggle,
            onSilentToggle = onSilentToggle,
            onTorchToggle = onTorchToggle,
            onAirplaneToggle = onAirplaneToggle,
            onDarkModeToggle = onDarkModeToggle,
            onDataClick = onDataClick,
            onWeatherClick = onWeatherClick,
            onWifiLongClick = onWifiLongClick,
            onBluetoothLongClick = onBluetoothLongClick,
            onDataLongClick = onDataLongClick,
            onAirplaneLongClick = onAirplaneLongClick,
            onSilentLongClick = onSilentLongClick,
            onTorchLongClick = onTorchLongClick,
            onDarkModeLongClick = onDarkModeLongClick,
            modifier      = Modifier
                .fillMaxWidth()
                .weight(0.40f)
        )

        // Pager
        Box(modifier = Modifier.weight(0.60f)) {
            if (uiState.settings.verticalScrolling) {
                VerticalPager(
                    state    = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(uiState, pages[pageIndex], iconCache, highlightedTileId, onTileTap, onTileLongPress)
                }
            } else {
                HorizontalPager(
                    state    = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PagerContent(uiState, pages[pageIndex], iconCache, highlightedTileId, onTileTap, onTileLongPress)
                }
            }
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
