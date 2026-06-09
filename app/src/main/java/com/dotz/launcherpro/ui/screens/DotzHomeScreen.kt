@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.components.AppGrid
import com.dotz.launcherpro.ui.components.AppList
import com.dotz.launcherpro.ui.components.StaticHeader
import com.dotz.launcherpro.ui.theme.DotzColors
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
    onWeatherClick: () -> Unit
) {
    val pages = listOfNotNull(
        uiState.page0Tiles,
        uiState.page1Tiles,
        uiState.page2Tiles.takeIf { it.isNotEmpty() }
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

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
            .padding(bottom = 24.dp)
    ) {
        // ── Fixed Header (Integrated with Detox Panel) ────────────────────
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
            is24HourFormat = uiState.settings.is24HourFormat,
            transparency = uiState.settings.tileTransparency,
            onLauncherSettingsTap = onLauncherSettingsTap,
            onWifiToggle = onWifiToggle,
            onBluetoothToggle = onBluetoothToggle,
            onSilentToggle = onSilentToggle,
            onTorchToggle = onTorchToggle,
            onAirplaneToggle = onAirplaneToggle,
            onDarkModeToggle = onDarkModeToggle,
            onDataClick = onDataClick,
            onWeatherClick = onWeatherClick,
            modifier      = Modifier
                .fillMaxWidth()
                .weight(0.40f)
        )

        // ── Pager (remaining area) ─────────────────────────────
        if (uiState.settings.verticalScrolling) {
            VerticalPager(
                state    = pagerState,
                modifier = Modifier.weight(0.60f)
            ) { pageIndex ->
                if (uiState.settings.layoutStyle == "list") {
                    AppList(
                        tiles = pages[pageIndex],
                        iconCache = iconCache,
                        grayscale = uiState.settings.grayscaleMode,
                        iconPackPackage = uiState.settings.iconPackPackage,
                        showBadges = uiState.settings.showNotificationDots,
                        transparency = uiState.settings.tileTransparency,
                        onTileTap = onTileTap,
                        onTileLongPress = onTileLongPress,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppGrid(
                        tiles = pages[pageIndex],
                        iconCache = iconCache,
                        grayscale = uiState.settings.grayscaleMode,
                        iconPackPackage = uiState.settings.iconPackPackage,
                        showBadges = uiState.settings.showNotificationDots,
                        transparency = uiState.settings.tileTransparency,
                        onTileTap = onTileTap,
                        onTileLongPress = onTileLongPress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(0.60f)
            ) { pageIndex ->
                if (uiState.settings.layoutStyle == "list") {
                    AppList(
                        tiles = pages[pageIndex],
                        iconCache = iconCache,
                        grayscale = uiState.settings.grayscaleMode,
                        iconPackPackage = uiState.settings.iconPackPackage,
                        showBadges = uiState.settings.showNotificationDots,
                        transparency = uiState.settings.tileTransparency,
                        onTileTap = onTileTap,
                        onTileLongPress = onTileLongPress,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppGrid(
                        tiles = pages[pageIndex],
                        iconCache = iconCache,
                        grayscale = uiState.settings.grayscaleMode,
                        iconPackPackage = uiState.settings.iconPackPackage,
                        showBadges = uiState.settings.showNotificationDots,
                        transparency = uiState.settings.tileTransparency,
                        onTileTap = onTileTap,
                        onTileLongPress = onTileLongPress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
