@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.components.AppGrid
import com.dotz.launcherpro.ui.components.DynamicBackground
import com.dotz.launcherpro.ui.components.StaticHeader
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
    onDataClick: () -> Unit
) {
    val pages = listOfNotNull(
        uiState.page0Tiles.takeIf { it.isNotEmpty() },
        uiState.page1Tiles.takeIf { it.isNotEmpty() }
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DynamicBackground(enabled = uiState.settings.dynamicBackgroundEnabled)
            
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Fixed Header (Integrated with Detox Panel) ────────────────────
                StaticHeader(
                    batteryLevel  = uiState.batteryLevel,
                    networkStatus = uiState.networkStatus,
                    isWifiEnabled = uiState.isWifiEnabled,
                    isBluetoothEnabled = uiState.isBluetoothEnabled,
                    isSilentMode = uiState.isSilentMode,
                    isTorchOn = uiState.isTorchOn,
                    isAirplaneModeOn = uiState.isAirplaneModeOn,
                    isDarkModeOn = uiState.isDarkModeOn,
                    onLauncherSettingsTap = onLauncherSettingsTap,
                    onWifiToggle = onWifiToggle,
                    onBluetoothToggle = onBluetoothToggle,
                    onSilentToggle = onSilentToggle,
                    onTorchToggle = onTorchToggle,
                    onAirplaneToggle = onAirplaneToggle,
                    onDarkModeToggle = onDarkModeToggle,
                    onDataClick = onDataClick,
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
                        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        val scaleFactor = lerp(0.95f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))

                        AppGrid(
                            tiles         = pages[pageIndex],
                            iconCache     = iconCache,
                            tileOpacity   = uiState.settings.tileOpacity,
                            grayscale     = uiState.settings.grayscaleMode,
                            iconPackPackage = uiState.settings.iconPackPackage,
                            showBadges    = uiState.settings.showNotificationDots,
                            onTileTap     = onTileTap,
                            onTileLongPress = onTileLongPress,
                            modifier      = Modifier
                                .fillMaxSize()
                                .scale(scaleFactor)
                        )
                    }
                } else {
                    HorizontalPager(
                        state    = pagerState,
                        modifier = Modifier.weight(0.60f)
                    ) { pageIndex ->
                        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        val scaleFactor = lerp(0.95f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))

                        AppGrid(
                            tiles         = pages[pageIndex],
                            iconCache     = iconCache,
                            tileOpacity   = uiState.settings.tileOpacity,
                            grayscale     = uiState.settings.grayscaleMode,
                            iconPackPackage = uiState.settings.iconPackPackage,
                            showBadges    = uiState.settings.showNotificationDots,
                            onTileTap     = onTileTap,
                            onTileLongPress = onTileLongPress,
                            modifier      = Modifier
                                .fillMaxSize()
                                .scale(scaleFactor)
                        )
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + fraction * (stop - start)
