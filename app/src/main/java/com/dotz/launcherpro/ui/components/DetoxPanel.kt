@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.R
import com.dotz.launcherpro.ui.theme.DotzTheme

@Composable
fun DetoxPanel(
    isWifiEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    ringerMode: Int, // 2 = NORMAL, 1 = VIBRATE, 0 = SILENT
    isTorchOn: Boolean,
    isAirplaneModeOn: Boolean,
    isDarkModeOn: Boolean,
    isMobileDataEnabled: Boolean = true,
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
    val isTransparentTheme = DotzTheme.colors.background == Color.Transparent
    val panelBackground = when {
        isGlass -> glassColor.copy(alpha = 0.05f)
        isTransparentTheme -> DotzTheme.colors.tile.copy(alpha = 0.10f)
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
                painter = painterResource(if (isWifiEnabled) R.drawable.ic_wifi_on else R.drawable.ic_wifi_off),
                label = if (isWifiEnabled) "WiFi On" else "WiFi Off",
                isActive = isWifiEnabled,
                transparency = transparency,
                onClick = onWifiToggle,
                onLongClick = onWifiLongClick
            )
            DetoxIcon(
                painter = painterResource(if (isBluetoothEnabled) R.drawable.ic_bluetooth_on else R.drawable.ic_bluetooth_off),
                label = if (isBluetoothEnabled) "Bluetooth On" else "Bluetooth Off",
                isActive = isBluetoothEnabled,
                transparency = transparency,
                onClick = onBluetoothToggle,
                onLongClick = onBluetoothLongClick
            )
            DetoxIcon(
                painter = painterResource(if (isMobileDataEnabled) R.drawable.ic_mobile_data else R.drawable.ic_mobile_data_off),
                label = if (isMobileDataEnabled) "Mobile Data On" else "Mobile Data Off",
                isActive = isMobileDataEnabled,
                transparency = transparency,
                onClick = onDataClick,
                onLongClick = onDataLongClick
            )
            DetoxIcon(
                painter = painterResource(if (isAirplaneModeOn) R.drawable.ic_airplane_on else R.drawable.ic_airplane_off),
                label = if (isAirplaneModeOn) "Airplane Mode On" else "Airplane Mode Off",
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
                painter = when (ringerMode) {
                    1 -> painterResource(R.drawable.ic_vibrate)
                    0 -> rememberVectorPainter(Icons.AutoMirrored.Rounded.VolumeOff)
                    else -> rememberVectorPainter(Icons.AutoMirrored.Rounded.VolumeUp)
                },
                label = when (ringerMode) {
                    1 -> "Vibrate Mode"
                    0 -> "Silent Mode"
                    else -> "Normal Mode"
                },
                isActive = ringerMode != 2,
                transparency = transparency,
                onClick = onSilentToggle,
                onLongClick = onSilentLongClick
            )
            DetoxIcon(
                painter = painterResource(if (isTorchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off),
                label = if (isTorchOn) "Flashlight On" else "Flashlight Off",
                isActive = isTorchOn,
                transparency = transparency,
                onClick = onTorchToggle,
                onLongClick = onTorchLongClick
            )
            DetoxIcon(
                painter = painterResource(if (isDarkModeOn) R.drawable.ic_dark_mode else R.drawable.ic_light_mode),
                label = if (isDarkModeOn) "Dark Mode On" else "Light Mode On",
                isActive = isDarkModeOn,
                transparency = transparency,
                onClick = onDarkModeToggle,
                onLongClick = onDarkModeLongClick
            )
            DetoxIcon(
                icon = Icons.Rounded.Settings,
                label = "Settings",
                isActive = false,
                transparency = transparency,
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DetoxIcon(
    icon: ImageVector? = null,
    painter: Painter? = null,
    label: String,
    isActive: Boolean,
    transparency: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val contentColor = DotzTheme.colors.text
    val accentColor = DotzTheme.colors.accent
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isActive) accentColor.copy(alpha = 0.2f * transparency) 
                else contentColor.copy(alpha = 0.08f * transparency)
            )
            .then(
                if (isActive) Modifier.border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val effectivePainter = painter ?: if (icon != null) rememberVectorPainter(icon) else null
        effectivePainter?.let {
            Icon(
                painter = it,
                contentDescription = label,
                tint = if (isActive) accentColor else contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
