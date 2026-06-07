package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaticHeader(
    batteryLevel: Int,
    networkStatus: String,
    weatherTemp: String?,
    weatherCondition: String?,
    showWeatherInfo: Boolean,
    isWifiEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isSilentMode: Boolean,
    isTorchOn: Boolean,
    isAirplaneModeOn: Boolean,
    isDarkModeOn: Boolean,
    is24HourFormat: Boolean,
    onLauncherSettingsTap: () -> Unit,
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onDataClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeText by remember(is24HourFormat) { mutableStateOf(currentTime(is24HourFormat)) }
    var dateText by remember { mutableStateOf(currentDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            timeText = currentTime(is24HourFormat)
            dateText = currentDate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
    ) {
        // Weather Info (Top Right)
        if (showWeatherInfo && weatherTemp != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { onWeatherClick() },
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = weatherTemp,
                    style = DotzType.DateStyle.copy(
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    ),
                    color = DotzTheme.colors.text
                )
                Text(
                    text = weatherCondition ?: "",
                    style = DotzType.DateStyle.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.sp
                    ),
                    color = DotzTheme.colors.text.copy(alpha = 0.5f)
                )
            }
        }

        // Main Content (Centered)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Network Info
                Text(
                    text = networkStatus,
                    style = DotzType.DateStyle.copy(fontSize = 11.sp),
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End
                )
                
                Spacer(Modifier.width(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text      = timeText,
                        style     = DotzType.TimeStyle,
                        color     = DotzTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text      = dateText,
                        style     = DotzType.DateStyle,
                        color     = DotzTheme.colors.text.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Battery Info
                Text(
                    text = if (batteryLevel >= 0) "$batteryLevel%" else "--%",
                    style = DotzType.DateStyle.copy(fontSize = 11.sp),
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(16.dp))

            DetoxPanel(
                isWifiEnabled = isWifiEnabled,
                isBluetoothEnabled = isBluetoothEnabled,
                isSilentMode = isSilentMode,
                isTorchOn = isTorchOn,
                isAirplaneModeOn = isAirplaneModeOn,
                isDarkModeOn = isDarkModeOn,
                onWifiToggle = onWifiToggle,
                onBluetoothToggle = onBluetoothToggle,
                onSilentToggle = onSilentToggle,
                onTorchToggle = onTorchToggle,
                onAirplaneToggle = onAirplaneToggle,
                onDarkModeToggle = onDarkModeToggle,
                onSettingsClick = onLauncherSettingsTap,
                onDataClick = onDataClick,
                modifier = Modifier.padding(bottom = 0.dp)
            )
        }
    }
}

private fun currentTime(is24Hour: Boolean): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

private fun currentDate(): String =
    SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
