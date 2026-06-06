package com.dotz.launcherpro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.data.DotzSettings
import com.dotz.launcherpro.data.ThemePresets

// ── Dynamic Design Tokens ──────────────────────────────────────────────────
data class DotzColorSystem(
    val background: Color,
    val tile: Color,
    val text: Color,
    val accent: Color,
    val settingsIcon: Color,
    val badgeDot: Color,
    val dateText: Color,
)

val LocalDotzColors = staticCompositionLocalOf {
    DotzColorSystem(
        background = Color.Black,
        tile = Color(0xFF1A1A1A),
        text = Color.White,
        accent = Color.White,
        settingsIcon = Color.White.copy(alpha = 0.4f),
        badgeDot = Color.White,
        dateText = Color(0xFFAAAAAA)
    )
}

object DotzTheme {
    val colors: DotzColorSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalDotzColors.current

    val typography = DotzType
}

@Composable
fun DotzTheme(
    settings: DotzSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = !settings.isLightMode // Use settings instead of isSystemInDarkTheme() for explicit control

    // 1. Determine base colors
    val preset = if (settings.isLightMode) ThemePresets.Light else ThemePresets.getById(settings.themeId)
    
    var background = preset.background
    var tile = preset.tile
    var text = preset.text
    var accent = preset.accent

    // 2. Override if Adaptive (Material You) is enabled
    if (settings.useAdaptiveTheme && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
        val dynamicColorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        background = dynamicColorScheme.background
        tile = dynamicColorScheme.surfaceVariant.copy(alpha = 0.5f)
        text = dynamicColorScheme.onBackground
        accent = dynamicColorScheme.primary
    }

    val dotzColors = DotzColorSystem(
        background = background,
        tile = tile,
        text = text,
        accent = accent,
        settingsIcon = text.copy(alpha = 0.4f),
        badgeDot = accent,
        dateText = text.copy(alpha = 0.6f)
    )

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            background = background,
            surface = tile,
            onPrimary = background,
            onBackground = text,
            onSurface = text
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = background,
            surface = tile,
            onPrimary = background,
            onBackground = text,
            onSurface = text
        )
    }

    CompositionLocalProvider(LocalDotzColors provides dotzColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}
