package com.dotz.launcherpro.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DotzType {
    val TimeStyle = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1).sp,
        color = Color.White,
    )
    val DateStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.sp,
        color = Color.White,
    )
    val TileLabelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.5.sp,
        color = Color.White,
    )
}
