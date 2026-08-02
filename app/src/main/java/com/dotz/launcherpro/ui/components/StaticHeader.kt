package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import android.os.Vibrator
import android.os.VibrationEffect

@Composable
fun StaticHeader(
    batteryLevel: Int,
    networkStatus: String,
    weatherTemp: String?,
    weatherCondition: String?,
    weatherFeelsLike: String? = null,
    weatherAqi: String? = null,
    weatherAqiLabel: String? = null,
    weatherLow: String? = null,
    weatherHigh: String? = null,
    weatherLocation: String? = null,
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
    clockStyle: String = "classic",
    isEditMode: Boolean = false,
    onEditModeChange: (Boolean) -> Unit = {},
    onClockStyleChange: (String) -> Unit = {},
    isHeaderEditMode: Boolean = false,
    onHeaderEditModeChange: (Boolean) -> Unit = {},
    onHeaderModeChange: (String) -> Unit = {},
    isPremium: Boolean = false,
    nowPlayingTitle: String = "Not Playing",
    nowPlayingArtist: String = "",
    isPlaying: Boolean = false,
    playbackPosition: Long = 0,
    playbackDuration: Long = 0,
    unlockCount: Int = 0,
    notificationsReceived: Int = 0,
    totalAppOpens: Int = 0,
    focusScore: Int = 100,
    blockedCount: Int = 0,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onMusicClick: () -> Unit = {},
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

    val styles = remember { listOf("classic", "modern", "minimalist", "analog", "textual") }
    val headerModes = remember { listOf("toggles", "stats", "weather", "music") }
    
    val hapticPulse: () -> Unit = {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20L)
            }
        }
    }

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
                if (clockStyle != "modern") {
                    Text(
                        text = networkStatus,
                        style = DotzType.dateStyle().copy(fontSize = 11.sp),
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        modifier = Modifier.width(50.dp),
                        textAlign = TextAlign.End
                    )
                    
                    Spacer(Modifier.width(16.dp))
                }

                val editScale by animateFloatAsState(if (isEditMode) 1.05f else 1f, label = "editScale")
                
                Box(
                    modifier = Modifier
                        .scale(editScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isEditMode) DotzTheme.colors.text.copy(alpha = 0.05f) else Color.Transparent)
                        .then(
                            if (isEditMode) Modifier.border(1.dp, DotzTheme.colors.accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .pointerInput(isEditMode, clockStyle) {
                            detectTapGestures(
                                onLongPress = {
                                    hapticPulse()
                                    onEditModeChange(!isEditMode)
                                },
                                onTap = {
                                    if (isEditMode) onEditModeChange(false)
                                    else onLauncherSettingsTap()
                                }
                            )
                        }
                        .pointerInput(isEditMode, clockStyle) {
                            if (isEditMode) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount ->
                                        totalDrag += dragAmount
                                        if (kotlin.math.abs(totalDrag) > 150f) {
                                            if (!isPremium) {
                                                hapticPulse()
                                                onEditModeChange(false)
                                                totalDrag = 0f
                                                return@detectHorizontalDragGestures
                                            }
                                            val currentIndex = styles.indexOf(clockStyle)
                                            val nextIndex = if (totalDrag > 0) {
                                                (currentIndex - 1 + styles.size) % styles.size
                                            } else {
                                                (currentIndex + 1) % styles.size
                                            }
                                            onClockStyleChange(styles[nextIndex])
                                            hapticPulse()
                                            totalDrag = 0f
                                        }
                                    }
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClockRenderer(
                        style = clockStyle,
                        time = timeText,
                        date = dateText
                    )
                }

                if (clockStyle != "modern") {
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
            }

            Spacer(Modifier.height(16.dp))

            HeaderWidgetContainer(
                isEditMode = isHeaderEditMode,
                currentMode = headerMode,
                modes = headerModes,
                isPremium = isPremium,
                onEditModeChange = onHeaderEditModeChange,
                onModeChange = onHeaderModeChange,
                hapticPulse = hapticPulse
            ) {
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
                        blockedCount = blockedCount,
                        onSettingsClick = onLauncherSettingsTap
                    )
                } else if (headerMode == "weather") {
                    WeatherHeaderWidget(
                        temp = weatherTemp ?: "--",
                        condition = weatherCondition ?: "Unknown",
                        location = weatherLocation ?: "Current weather",
                        feelsLike = weatherFeelsLike ?: "",
                        low = weatherLow ?: "--",
                        high = weatherHigh ?: "--",
                        aqi = weatherAqi,
                        aqiLabel = weatherAqiLabel,
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
}

@Composable
private fun HeaderWidgetContainer(
    isEditMode: Boolean,
    currentMode: String,
    modes: List<String>,
    isPremium: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onModeChange: (String) -> Unit,
    hapticPulse: () -> Unit,
    content: @Composable () -> Unit
) {
    val editScale by animateFloatAsState(if (isEditMode) 1.02f else 1f, label = "headerEditScale")

    Box(
        modifier = Modifier
            .scale(editScale)
            .clip(RoundedCornerShape(32.dp))
            .background(if (isEditMode) DotzTheme.colors.text.copy(alpha = 0.03f) else Color.Transparent)
            .then(
                if (isEditMode) Modifier.border(1.dp, DotzTheme.colors.accent.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                else Modifier
            )
            .pointerInput(isEditMode, currentMode) {
                detectTapGestures(
                    onLongPress = {
                        hapticPulse()
                        onEditModeChange(!isEditMode)
                    },
                    onTap = {
                        if (isEditMode) onEditModeChange(false)
                    }
                )
            }
            .pointerInput(isEditMode, currentMode) {
                if (isEditMode) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                            if (kotlin.math.abs(totalDrag) > 150f) {
                                if (!isPremium) {
                                    hapticPulse()
                                    onEditModeChange(false)
                                    totalDrag = 0f
                                    return@detectHorizontalDragGestures
                                }
                                val currentIndex = modes.indexOf(currentMode)
                                val nextIndex = if (totalDrag > 0) {
                                    (currentIndex - 1 + modes.size) % modes.size
                                } else {
                                    (currentIndex + 1) % modes.size
                                }
                                onModeChange(modes[nextIndex])
                                hapticPulse()
                                totalDrag = 0f
                            }
                        }
                    )
                }
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
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
    val isTransparentTheme = DotzTheme.colors.background == Color.Transparent
    val containerColor = if (isGlass) {
        glassColor.copy(alpha = 0.05f)
    } else if (isTransparentTheme) {
        DotzTheme.colors.tile.copy(alpha = 0.10f)
    } else {
        DotzTheme.colors.tile.copy(alpha = 0.7f)
    }

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
    blockedCount: Int,
    onSettingsClick: () -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val isTransparentTheme = DotzTheme.colors.background == Color.Transparent
    val containerColor = if (isGlass) {
        glassColor.copy(alpha = 0.05f)
    } else if (isTransparentTheme) {
        DotzTheme.colors.tile.copy(alpha = 0.10f)
    } else {
        DotzTheme.colors.tile.copy(alpha = 0.7f)
    }

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
        Box(modifier = Modifier.fillMaxSize()) {
            // Settings Button in Top Right
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Launcher Settings",
                    tint = DotzTheme.colors.text.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
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

                    if (blockedCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, null, tint = DotzTheme.colors.accent.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$blockedCount Batched",
                                color = DotzTheme.colors.accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
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
}

@Composable
fun WeatherHeaderWidget(
    temp: String,
    condition: String,
    location: String,
    feelsLike: String,
    low: String,
    high: String,
    aqi: String?,
    aqiLabel: String?,
    onSettingsClick: () -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val isTransparentTheme = DotzTheme.colors.background == Color.Transparent
    val containerColor = if (isGlass) {
        glassColor.copy(alpha = 0.05f)
    } else if (isTransparentTheme) {
        DotzTheme.colors.tile.copy(alpha = 0.10f)
    } else {
        DotzTheme.colors.tile.copy(alpha = 0.7f)
    }

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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = location,
                        color = DotzTheme.colors.text.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
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
                            text = "$feelsLike • $low / $high",
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

@Composable
fun ClockRenderer(
    style: String,
    time: String,
    date: String
) {
    val textColor = DotzTheme.colors.text
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (style) {
            "modern" -> {
                Text(
                    text = time,
                    style = DotzType.timeStyle().copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    ),
                    color = textColor
                )
                Text(
                    text = date,
                    style = DotzType.dateStyle().copy(fontWeight = FontWeight.Bold),
                    color = textColor.copy(alpha = 0.6f)
                )
            }
            "minimalist" -> {
                Text(
                    text = time,
                    style = DotzType.timeStyle().copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 4.sp
                    ),
                    color = textColor
                )
            }
            "analog" -> {
                AnalogClock(size = 100.dp, color = textColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = date,
                    style = DotzType.dateStyle().copy(fontSize = 10.sp),
                    color = textColor.copy(alpha = 0.4f)
                )
            }
            "textual" -> {
                val words = remember(time) { timeToWords() }
                Text(
                    text = words.first,
                    style = DotzType.dateStyle().copy(fontSize = 12.sp, fontWeight = FontWeight.Light),
                    color = textColor.copy(alpha = 0.5f)
                )
                Text(
                    text = words.second,
                    style = DotzType.timeStyle().copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
            else -> { // classic
                Text(
                    text = time,
                    style = DotzType.timeStyle(),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = date,
                    style = DotzType.dateStyle(),
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnalogClock(size: androidx.compose.ui.unit.Dp, color: Color) {
    val calendar = Calendar.getInstance()
    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val radius = size.toPx() / 2

        // Outer Circle
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Hour Hand
        val hourAngle = (hours + minutes / 60f) * 30f - 90f
        drawLine(
            color = color,
            start = center,
            end = Offset(
                x = center.x + radius * 0.5f * cos(Math.toRadians(hourAngle.toDouble())).toFloat(),
                y = center.y + radius * 0.5f * sin(Math.toRadians(hourAngle.toDouble())).toFloat()
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Minute Hand
        val minuteAngle = minutes * 6f - 90f
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = center,
            end = Offset(
                x = center.x + radius * 0.8f * cos(Math.toRadians(minuteAngle.toDouble())).toFloat(),
                y = center.y + radius * 0.8f * sin(Math.toRadians(minuteAngle.toDouble())).toFloat()
            ),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun timeToWords(): Pair<String, String> {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)

    val nums = listOf(
        "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN",
        "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN",
        "TWENTY", "TWENTY ONE", "TWENTY TWO", "TWENTY THREE", "TWENTY FOUR", "TWENTY FIVE", "TWENTY SIX", "TWENTY SEVEN", "TWENTY EIGHT", "TWENTY NINE"
    )

    return when {
        minute == 0 -> "EXACTLY" to nums[if (hour == 0) 12 else hour] + " O'CLOCK"
        minute == 15 -> "QUARTER PAST" to nums[if (hour == 0) 12 else hour]
        minute == 30 -> "HALF PAST" to nums[if (hour == 0) 12 else hour]
        minute == 45 -> "QUARTER TO" to nums[if (hour == 11) 12 else (hour + 1) % 12]
        minute < 30 -> nums[minute] + " PAST" to nums[if (hour == 0) 12 else hour]
        else -> nums[60 - minute] + " TO" to nums[if (hour == 11) 12 else (hour + 1) % 12]
    }
}
