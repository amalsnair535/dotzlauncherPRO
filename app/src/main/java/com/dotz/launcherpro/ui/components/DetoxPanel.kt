package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                onClick = onWifiToggle
            )
            DetoxIcon(
                icon = if (isBluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                isActive = isBluetoothEnabled,
                onClick = onBluetoothToggle
            )
            DetoxIcon(
                icon = Icons.Rounded.SwapVert,
                isActive = true,
                onClick = onDataClick
            )
            DetoxIcon(
                icon = if (isAirplaneModeOn) Icons.Rounded.AirplanemodeActive else Icons.Rounded.AirplanemodeInactive,
                isActive = isAirplaneModeOn,
                onClick = onAirplaneToggle
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
                onClick = onSilentToggle
            )
            DetoxIcon(
                icon = if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                isActive = isTorchOn,
                onClick = onTorchToggle
            )
            DetoxIcon(
                icon = if (isDarkModeOn) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                isActive = isDarkModeOn,
                onClick = onDarkModeToggle
            )
            DetoxIcon(
                icon = Icons.Rounded.Settings,
                isActive = false, 
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun DetoxIcon(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp) // Slightly smaller for better fit in 2 rows
            .clip(CircleShape)
            .background(if (isActive) DotzTheme.colors.text.copy(alpha = 0.12f) else DotzTheme.colors.tile)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}
