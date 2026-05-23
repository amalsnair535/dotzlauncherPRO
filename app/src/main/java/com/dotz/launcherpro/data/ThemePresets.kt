package com.dotz.launcherpro.data

import androidx.compose.ui.graphics.Color

data class DotzThemePreset(
    val id: String,
    val name: String,
    val background: Color,
    val tile: Color,
    val text: Color,
    val accent: Color
)

object ThemePresets {
    val Default = DotzThemePreset(
        id = "default",
        name = "Dotz Dark",
        background = Color(0xFF000000),
        tile = Color(0xFF1A1A1A),
        text = Color(0xFFFFFFFF),
        accent = Color(0xFFFFFFFF)
    )

    val Nord = DotzThemePreset(
        id = "nord",
        name = "Nordic",
        background = Color(0xFF2E3440),
        tile = Color(0xFF3B4252),
        text = Color(0xFFECEFF4),
        accent = Color(0xFF88C0D0)
    )

    val Forest = DotzThemePreset(
        id = "forest",
        name = "Deep Forest",
        background = Color(0xFF1B1F17),
        tile = Color(0xFF2A2E24),
        text = Color(0xFFE8E9E4),
        accent = Color(0xFF8DA37E)
    )

    val Sunset = DotzThemePreset(
        id = "sunset",
        name = "Sunset Glow",
        background = Color(0xFF1F1A1D),
        tile = Color(0xFF2E262A),
        text = Color(0xFFF5F0F2),
        accent = Color(0xFFE9967A)
    )

    val all = listOf(Default, Nord, Forest, Sunset)
    
    fun getById(id: String) = all.find { it.id == id } ?: Default
}
