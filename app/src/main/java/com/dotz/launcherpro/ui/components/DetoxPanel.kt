@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme

@Composable
fun DetoxPanel(
    isWifiEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isSilentMode: Boolean,
    isTorchOn: Boolean,
    isAirplaneModeOn: Boolean,
    isDarkModeOn: Boolean,
    transparency: Float = 1.0f,
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onDataClick: () -> Unit,
    onWifiLongClick: () -> Unit = {},
    onBluetoothLongClick: () -> Unit = {},
    onDataLongClick: () -> Unit = {},
    onAirplaneLongClick: () -> Unit = {},
    onSilentLongClick: () -> Unit = {},
    onTorchLongClick: () -> Unit = {},
    onDarkModeLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val panelBackground = when {
        isGlass -> glassColor.copy(alpha = 0.05f)
        else -> DotzTheme.colors.tile.copy(alpha = transparency.coerceAtLeast(0.3f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .then(
                if (isGlass) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(glassColor.copy(alpha = 0.3f), Color.Transparent, glassColor.copy(alpha = 0.1f)),
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset.Infinite
                            ),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            )
            .background(panelBackground)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: WiFi, BT, Mobile Data, Airplane
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetoxIcon(
                icon = if (isWifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                isActive = isWifiEnabled,
                transparency = transparency,
                onClick = onWifiToggle,
                onLongClick = onWifiLongClick
            )
            DetoxIcon(
                icon = if (isBluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                isActive = isBluetoothEnabled,
                transparency = transparency,
                onClick = onBluetoothToggle,
                onLongClick = onBluetoothLongClick
            )
            DetoxIcon(
                icon = Icons.Rounded.SwapVert,
                isActive = true,
                transparency = transparency,
                onClick = onDataClick,
                onLongClick = onDataLongClick
            )
            DetoxIcon(
                icon = if (isAirplaneModeOn) Icons.Rounded.AirplanemodeActive else Icons.Rounded.AirplanemodeInactive,
                isActive = isAirplaneModeOn,
                transparency = transparency,
                onClick = onAirplaneToggle,
                onLongClick = onAirplaneLongClick
            )
        }

        // Row 2: Silent, Torch, Dark Mode, Settings (Gear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetoxIcon(
                icon = if (isSilentMode) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                isActive = !isSilentMode,
                transparency = transparency,
                onClick = onSilentToggle,
                onLongClick = onSilentLongClick
            )
            DetoxIcon(
                icon = if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                isActive = isTorchOn,
                transparency = transparency,
                onClick = onTorchToggle,
                onLongClick = onTorchLongClick
            )
            DetoxIcon(
                icon = if (isDarkModeOn) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                isActive = isDarkModeOn,
                transparency = transparency,
                onClick = onDarkModeToggle,
                onLongClick = onDarkModeLongClick
            )
            DetoxIcon(
                icon = Icons.Rounded.Settings,
                isActive = false,
                transparency = transparency,
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DetoxIcon(
    icon: ImageVector,
    isActive: Boolean,
    transparency: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val contentColor = DotzTheme.colors.text
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) contentColor.copy(alpha = 0.25f * transparency) else contentColor.copy(alpha = 0.1f * transparency))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
