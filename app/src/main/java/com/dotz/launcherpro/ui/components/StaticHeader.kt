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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
    weatherFeelsLike: String? = null,
    weatherSummary: String? = null,
    weatherAqi: String? = null,
    weatherAqiLabel: String? = null,
    weatherLow: String? = null,
    weatherHigh: String? = null,
    showWeatherInfo: Boolean,
    isWifiEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isSilentMode: Boolean,
    isTorchOn: Boolean,
    isAirplaneModeOn: Boolean,
    isDarkModeOn: Boolean,
    ringerMode: Int = 2,
    isMobileDataEnabled: Boolean = true,
    transparency: Float = 1.0f,
    headerMode: String = "toggles",
    nowPlayingTitle: String = "Not Playing",
    nowPlayingArtist: String = "",
    isPlaying: Boolean = false,
    playbackPosition: Long = 0,
    playbackDuration: Long = 0,
    unlockCount: Int = 0,
    notificationsReceived: Int = 0,
    totalAppOpens: Int = 0,
    focusScore: Int = 100,
    focusSoundPlaying: String? = null,
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
    onFocusSoundClick: (String) -> Unit = {},
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
                    notificationsReceived = notificationsReceived,
                    totalAppOpens = totalAppOpens,
                    focusScore = focusScore,
                    onSettingsClick = onLauncherSettingsTap
                )
            } else if (headerMode == "weather") {
                WeatherHeaderWidget(
                    temp = weatherTemp ?: "--",
                    condition = weatherCondition ?: "Unknown",
                    feelsLike = weatherFeelsLike ?: "",
                    summary = weatherSummary ?: "",
                    aqi = weatherAqi,
                    aqiLabel = weatherAqiLabel,
                    onWeatherClick = onWeatherClick,
                    onSettingsClick = onLauncherSettingsTap
                )
            } else if (headerMode == "sounds") {
                FocusSoundsWidget(
                    currentSound = focusSoundPlaying,
                    onSoundClick = onFocusSoundClick,
                    onSettingsClick = onLauncherSettingsTap
                )
            } else {
                DetoxPanel(
                    isWifiEnabled = isWifiEnabled,
                    isBluetoothEnabled = isBluetoothEnabled,
                    ringerMode = ringerMode,
                    isTorchOn = isTorchOn,
                    isAirplaneModeOn = isAirplaneModeOn,
                    isDarkModeOn = isDarkModeOn,
                    isMobileDataEnabled = isMobileDataEnabled,
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
    
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) glassColor.copy(alpha = 0.05f) else DotzTheme.colors.tile.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Music Icon / Album Art Placeholder
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DotzTheme.colors.text.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = DotzTheme.colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (title == "Not Playing") "Nothing Playing" else title,
                        color = DotzTheme.colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (artist.isBlank()) "Play something to see info" else artist,
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(Icons.Default.Settings, "Launcher Settings", tint = DotzTheme.colors.text.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (title != "Not Playing") {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, "Previous Track", tint = DotzTheme.colors.text, modifier = Modifier.size(28.dp))
                    }
                    
                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = DotzTheme.colors.text,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DotzTheme.colors.solidBackground,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, "Next Track", tint = DotzTheme.colors.text, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FocusStatsWidget(
    unlockCount: Int,
    notificationsReceived: Int,
    totalAppOpens: Int,
    focusScore: Int,
    onSettingsClick: () -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) glassColor.copy(alpha = 0.05f) else DotzTheme.colors.tile.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY INSIGHTS",
                    color = DotzTheme.colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, null, tint = DotzTheme.colors.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$unlockCount Unlocks",
                        color = DotzTheme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = DotzTheme.colors.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$notificationsReceived Notifications",
                        color = DotzTheme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apps, null, tint = DotzTheme.colors.text.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$totalAppOpens App Launches",
                        color = DotzTheme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(24.dp).offset(x = (-4).dp, y = 4.dp)
                ) {
                    Icon(Icons.Default.Settings, "Launcher Settings", tint = DotzTheme.colors.text.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                }
            }

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { focusScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = DotzTheme.colors.accent,
                    strokeWidth = 8.dp,
                    trackColor = DotzTheme.colors.text.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$focusScore",
                        color = DotzTheme.colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "FOCUS",
                        color = DotzTheme.colors.text.copy(alpha = 0.3f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherHeaderWidget(
    temp: String,
    condition: String,
    feelsLike: String,
    summary: String,
    aqi: String?,
    aqiLabel: String?,
    onWeatherClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) glassColor.copy(alpha = 0.05f) else DotzTheme.colors.tile.copy(alpha = 0.7f)
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
            .clickable { onWeatherClick() }
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current weather",
                        color = DotzTheme.colors.text.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTime(context),
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = DotzTheme.colors.text.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        condition.contains("Clear") -> Icons.Default.WbSunny
                        condition.contains("Cloud") -> Icons.Default.Cloud
                        condition.contains("Rain") || condition.contains("Drizzle") -> Icons.Default.WaterDrop
                        condition.contains("Thunder") -> Icons.Default.Thunderstorm
                        condition.contains("Snow") -> Icons.Default.AcUnit
                        else -> Icons.Default.Cloud
                    },
                    contentDescription = null,
                    tint = DotzTheme.colors.accent,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    text = temp,
                    color = DotzTheme.colors.text,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light
                )

                Spacer(Modifier.width(24.dp))

                Column {
                    Text(
                        text = condition,
                        color = DotzTheme.colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (feelsLike.isNotBlank()) {
                        Text(
                            text = feelsLike,
                            color = DotzTheme.colors.text.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    if (!aqi.isNullOrBlank()) {
                        Text(
                            text = "AQI: $aqi ($aqiLabel)",
                            color = DotzTheme.colors.accent.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = summary,
                color = DotzTheme.colors.text.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FocusSoundsWidget(
    currentSound: String?,
    onSoundClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) glassColor.copy(alpha = 0.05f) else DotzTheme.colors.tile.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp)
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
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(28.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCUS SOUNDS",
                    color = DotzTheme.colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = DotzTheme.colors.text.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "Rain" to Icons.Default.WaterDrop,
                    "Brown Noise" to Icons.Default.GraphicEq,
                    "Lo-Fi" to Icons.Default.MusicNote
                ).forEach { (name, icon) ->
                    val isPlaying = currentSound == name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSoundClick(name) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isPlaying) DotzTheme.colors.accent else DotzTheme.colors.text.copy(alpha = 0.05f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isPlaying) DotzTheme.colors.solidBackground else DotzTheme.colors.text,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = name,
                            color = DotzTheme.colors.text,
                            fontSize = 10.sp,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

fun currentTime(context: android.content.Context): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

fun currentDate(): String =
    SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
