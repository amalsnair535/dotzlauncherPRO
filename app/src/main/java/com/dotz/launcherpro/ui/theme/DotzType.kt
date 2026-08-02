package com.dotz.launcherpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DotzType {

    fun resolveFontFamily(fontId: String): FontFamily {
        return when (fontId) {
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            "sans-serif" -> FontFamily.SansSerif
            else -> FontFamily.Default
        }
    }

    fun getTimeStyle(fontFamily: FontFamily) = TextStyle(
        fontFamily = fontFamily,
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1).sp,
    )
    
    fun getDateStyle(fontFamily: FontFamily) = TextStyle(
        fontFamily = fontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.sp,
    )
    
    fun getTileLabelStyle(fontFamily: FontFamily) = TextStyle(
        fontFamily = fontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.5.sp,
    )

    fun getTypography(fontFamily: FontFamily): Typography {
        val baseTimeStyle = getTimeStyle(fontFamily)
        val baseDateStyle = getDateStyle(fontFamily)
        val baseTileStyle = getTileLabelStyle(fontFamily)

        return Typography(
            displayLarge  = baseTimeStyle,
            displayMedium = baseTimeStyle.copy(fontSize = 32.sp),
            displaySmall  = baseTimeStyle.copy(fontSize = 24.sp),
            headlineLarge = baseDateStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            headlineMedium = baseDateStyle.copy(fontSize = 18.sp),
            headlineSmall = baseDateStyle.copy(fontSize = 16.sp),
            titleLarge    = baseTileStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            titleMedium   = baseTileStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            titleSmall    = baseTileStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            bodyLarge     = baseTileStyle.copy(fontSize = 14.sp),
            bodyMedium    = baseTileStyle.copy(fontSize = 12.sp),
            bodySmall     = baseTileStyle.copy(fontSize = 11.sp),
            labelLarge    = baseDateStyle.copy(fontSize = 14.sp),
            labelMedium   = baseDateStyle.copy(fontSize = 12.sp),
            labelSmall    = baseDateStyle.copy(fontSize = 11.sp)
        )
    }

    @Composable
    fun timeStyle() = MaterialTheme.typography.displayLarge
    
    @Composable
    fun dateStyle() = MaterialTheme.typography.labelLarge
    
    @Composable
    fun tileLabelStyle() = MaterialTheme.typography.bodySmall
}
