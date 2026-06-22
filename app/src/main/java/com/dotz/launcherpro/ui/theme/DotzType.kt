package com.dotz.launcherpro.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.R

object DotzType {
    
    fun getFontFamily(fontId: String?): FontFamily {
        return when (fontId) {
            "inter" -> FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_bold, FontWeight.Bold))
            "manrope" -> FontFamily(Font(R.font.manrope_regular, FontWeight.Normal), Font(R.font.manrope_medium, FontWeight.Medium), Font(R.font.manrope_bold, FontWeight.Bold))
            "ibm_plex_sans" -> FontFamily(Font(R.font.ibm_plex_sans_regular, FontWeight.Normal), Font(R.font.ibm_plex_sans_medium, FontWeight.Medium), Font(R.font.ibm_plex_sans_bold, FontWeight.Bold))
            "space_grotesk" -> FontFamily(Font(R.font.space_grotesk_regular, FontWeight.Normal), Font(R.font.space_grotesk_medium, FontWeight.Medium), Font(R.font.space_grotesk_bold, FontWeight.Bold))
            "outfit" -> FontFamily(Font(R.font.outfit_regular, FontWeight.Normal), Font(R.font.outfit_medium, FontWeight.Medium), Font(R.font.outfit_bold, FontWeight.Bold))
            "jakarta" -> FontFamily(Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal), Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium), Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold))
            "sora" -> FontFamily(Font(R.font.sora_regular, FontWeight.Normal), Font(R.font.sora_medium, FontWeight.Medium), Font(R.font.sora_bold, FontWeight.Bold))
            "jetbrains_mono" -> FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal), Font(R.font.jetbrains_mono_medium, FontWeight.Medium), Font(R.font.jetbrains_mono_bold, FontWeight.Bold))
            "ibm_plex_mono" -> FontFamily(Font(R.font.ibm_plex_mono_regular, FontWeight.Normal), Font(R.font.ibm_plex_mono_medium, FontWeight.Medium), Font(R.font.ibm_plex_mono_bold, FontWeight.Bold))
            "space_mono" -> FontFamily(Font(R.font.space_mono_regular, FontWeight.Normal), Font(R.font.space_mono_bold, FontWeight.Bold))
            "dm_sans" -> FontFamily(Font(R.font.dm_sans_regular, FontWeight.Normal), Font(R.font.dm_sans_medium, FontWeight.Medium), Font(R.font.dm_sans_bold, FontWeight.Bold))
            "instrument" -> FontFamily(Font(R.font.instrument_sans_regular, FontWeight.Normal), Font(R.font.instrument_sans_medium, FontWeight.Medium), Font(R.font.instrument_sans_bold, FontWeight.Bold))
            "work_sans" -> FontFamily(Font(R.font.work_sans_regular, FontWeight.Normal), Font(R.font.work_sans_medium, FontWeight.Medium), Font(R.font.work_sans_bold, FontWeight.Bold))
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

    // Legacy support for basic styles
    val TimeStyle = getTimeStyle(FontFamily.Default)
    val DateStyle = getDateStyle(FontFamily.Default)
    val TileLabelStyle = getTileLabelStyle(FontFamily.Default)
}
