package com.dotz.launcherpro.ui.theme

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dotz.launcherpro.data.DotzSettings
import com.dotz.launcherpro.data.DotzThemePreset
import com.dotz.launcherpro.data.ThemePresets
import java.util.Calendar

// ── Dynamic Design Tokens ──────────────────────────────────────────────────
data class DotzColorSystem(
    val background: Color,
    val solidBackground: Color,
    val tile: Color,
    val text: Color,
    val accent: Color,
    val settingsIcon: Color,
    val badgeDot: Color,
    val dateText: Color,
    val isGlass: Boolean = false
)

val LocalDotzColors = staticCompositionLocalOf {
    DotzColorSystem(
        background = Color.Black,
        solidBackground = Color.Black,
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
fun getCircadianTint(): Color {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..10 -> Color(0xFFB2EBF2) // Morning: Crisp Cool (Light Blue)
        in 11..16 -> Color.Transparent // Mid-day: Neutral
        in 17..20 -> Color(0xFFFFCC80) // Evening: Warm Amber (Orange-ish)
        in 21..23, in 0..5 -> Color(0xFFFFAB91) // Night: Deep Warm (Reddish-Orange)
        else -> Color.Transparent
    }
}

@Composable
fun DotzTheme(
    settings: DotzSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = if (settings.useCircadianTheming) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour !in 7..18 // Use Light Mode between 7 AM and 6 PM for Circadian
    } else {
        !settings.isLightMode
    }
    
    val effectiveIsLightMode = !darkTheme

    val isLiquidGlass = settings.useLiquidGlass

    // 1. Determine base colors
    val preset = if (effectiveIsLightMode) ThemePresets.Light else ThemePresets.getById(settings.themeId)

    val solidBackground = preset.background
    var background = if (settings.showWallpaper) Color.Transparent else preset.background
    var tile = preset.tile.copy(alpha = settings.tileTransparency)
    var text = preset.text
    var accent = preset.accent

    // 2. Override if Adaptive (Material You) is enabled
    var currentSolidBackground = solidBackground
    if (settings.useAdaptiveTheme && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
        val dynamicColorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        background = if (settings.showWallpaper) Color.Transparent else dynamicColorScheme.background
        currentSolidBackground = dynamicColorScheme.background
        tile = dynamicColorScheme.surfaceVariant.copy(alpha = 0.5f)
        text = dynamicColorScheme.onBackground
        accent = dynamicColorScheme.primary
    }

    // 3. Apply Circadian Tint if enabled
    if (settings.useCircadianTheming) {
        val tint = getCircadianTint()
        if (tint != Color.Transparent) {
            // Apply a more noticeable blend, especially for the accent color
            background = background.blend(tint, 0.5f)
            currentSolidBackground = currentSolidBackground.blend(tint, 0.5f)
            accent = tint.copy(alpha = 1.0f) // Use the tint color itself as accent for visibility
            tile = tile.blend(tint, 0.3f)
        }
    }

    // 4. Apply Custom Color Tint if in Custom Mode
    if (settings.themeId == "custom" && settings.customAccentColor != null) {
        val customColor = Color(settings.customAccentColor)
        // Background Tint (subtle 15%)
        if (background != Color.Transparent) {
            background = background.blend(customColor, 0.15f)
        }
        currentSolidBackground = currentSolidBackground.blend(customColor, 0.15f)
        
        // Tile Tint (distinct 25%)
        tile = tile.blend(customColor, 0.25f)
        
        // Use custom color as primary accent
        accent = customColor
    }

    val dotzColors = DotzColorSystem(
        background = background,
        solidBackground = currentSolidBackground,
        tile = tile,
        text = text,
        accent = accent,
        settingsIcon = text.copy(alpha = 0.4f),
        badgeDot = accent,
        dateText = text.copy(alpha = 0.6f),
        isGlass = isLiquidGlass
    )

    // Font Handling
    val fontFamily = DotzType.resolveFontFamily(settings.fontId)
    val materialTypography = DotzType.getTypography(fontFamily)

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

    CompositionLocalProvider(
        LocalDotzColors provides dotzColors
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            content = content
        )
    }
}

// Extension to blend colors
fun Color.blend(overlay: Color, amount: Float): Color {
    val inverse = 1f - amount
    return Color(
        red = (this.red * inverse) + (overlay.red * amount),
        green = (this.green * inverse) + (overlay.green * amount),
        blue = (this.blue * inverse) + (overlay.blue * amount),
        alpha = this.alpha
    )
}
