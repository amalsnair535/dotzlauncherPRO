package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.data.DotzSettings
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
    transparency: Float = 1.0f,
    headerMode: String = "toggles",
    nowPlayingTitle: String = "Not Playing",
    nowPlayingArtist: String = "",
    isPlaying: Boolean = false,
    playbackPosition: Long = 0,
    playbackDuration: Long = 0,
    unlockCount: Int = 0,
    focusScore: Int = 100,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
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
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var timeText by remember { mutableStateOf(currentTime(context)) }
    var dateText by remember { mutableStateOf(currentDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            timeText = currentTime(context)
            dateText = currentDate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
    ) {
        // Weather Info (Top Right)
        if (showWeatherInfo) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { onWeatherClick() },
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = weatherTemp ?: "...",
                    style = DotzType.dateStyle().copy(
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    ),
                    color = DotzTheme.colors.text
                )
                Text(
                    text = weatherCondition ?: "Searching",
                    style = DotzType.dateStyle().copy(
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
                    style = DotzType.dateStyle().copy(fontSize = 11.sp),
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
                        style     = DotzType.timeStyle(),
                        color     = DotzTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text      = dateText,
                        style     = DotzType.dateStyle(),
                        color     = DotzTheme.colors.text.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Battery Info
                Text(
                    text = if (batteryLevel >= 0) "$batteryLevel%" else "--%",
                    style = DotzType.dateStyle().copy(fontSize = 11.sp),
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(16.dp))

            if (headerMode == "music") {
                NowPlayingWidget(
                    title = nowPlayingTitle,
                    artist = nowPlayingArtist,
                    isPlaying = isPlaying,
                    position = playbackPosition,
                    duration = playbackDuration,
                    onPlayPause = onPlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onSettingsClick = onLauncherSettingsTap
                )
            } else if (headerMode == "stats") {
                FocusStatsWidget(
                    unlockCount = unlockCount,
                    focusScore = focusScore,
                    onSettingsClick = onLauncherSettingsTap
                )
            } else {
                DetoxPanel(
                    isWifiEnabled = isWifiEnabled,
                    isBluetoothEnabled = isBluetoothEnabled,
                    isSilentMode = isSilentMode,
                    isTorchOn = isTorchOn,
                    isAirplaneModeOn = isAirplaneModeOn,
                    isDarkModeOn = isDarkModeOn,
                    transparency = transparency,
                    onWifiToggle = onWifiToggle,
                    onBluetoothToggle = onBluetoothToggle,
                    onSilentToggle = onSilentToggle,
                    onTorchToggle = onTorchToggle,
                    onAirplaneToggle = onAirplaneToggle,
                    onDarkModeToggle = onDarkModeToggle,
                    onSettingsClick = onLauncherSettingsTap,
                    onDataClick = onDataClick,
                    onWifiLongClick = onWifiLongClick,
                    onBluetoothLongClick = onBluetoothLongClick,
                    onDataLongClick = onDataLongClick,
                    onAirplaneLongClick = onAirplaneLongClick,
                    onSilentLongClick = onSilentLongClick,
                    onTorchLongClick = onTorchLongClick,
                    onDarkModeLongClick = onDarkModeLongClick,
                    modifier = Modifier.padding(bottom = 0.dp)
                )
            }
        }
    }
}

@Composable
fun NowPlayingWidget(
    title: String,
    artist: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = DotzTheme.colors.tile.copy(alpha = 0.8f) // Theme-aware background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DotzTheme.colors.text.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Settings, null, tint = DotzTheme.colors.text.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (title == "Not Playing") "Nothing Playing" else title,
                        color = DotzTheme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (artist.isBlank()) "Play something to see info" else artist,
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSkipPrevious, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DotzTheme.colors.accent)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DotzTheme.colors.solidBackground, // Contrasts with accent
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (title != "Not Playing" && duration > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = DotzTheme.colors.accent,
                    trackColor = DotzTheme.colors.text.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun FocusStatsWidget(
    unlockCount: Int,
    focusScore: Int,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = DotzTheme.colors.tile.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings button (consistent with music widget)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DotzTheme.colors.text.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Settings, null, tint = DotzTheme.colors.text.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DOTZ",
                    color = DotzTheme.colors.text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Unlocked: $unlockCount times",
                    color = DotzTheme.colors.text.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Keep going.",
                    color = DotzTheme.colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Focus Score",
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$focusScore",
                        color = DotzTheme.colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/100",
                        color = DotzTheme.colors.text.copy(alpha = 0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun currentTime(context: android.content.Context): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

private fun currentDate(): String =
    SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
