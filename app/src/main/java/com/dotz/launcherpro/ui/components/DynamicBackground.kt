package com.dotz.launcherpro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(Color.Black)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Calculate moving center for the radial gradient
        val centerX = width / 2 + (width / 4) * cos(phase)
        val centerY = height / 2 + (height / 4) * sin(phase)

        val brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1A1A1A), // Very dark grey
                Color.Black
            ),
            center = Offset(centerX, centerY),
            radius = width.coerceAtLeast(height) * 0.8f
        )

        drawRect(brush = brush)
    }
}
