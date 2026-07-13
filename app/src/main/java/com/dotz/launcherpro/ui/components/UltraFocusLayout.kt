package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType

@Composable
fun UltraFocusLayout(
    tiles: List<AppTile>,
    remainingMillis: Long,
    onTileTap: (AppTile) -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Timer text
    val minutes = (remainingMillis / 60000).toInt()
    val seconds = ((remainingMillis % 60000) / 1000).toInt()
    val timerText = String.format("%02d:%02d", minutes, seconds)

    // Define the core essentials we want to show
    val essentialPackages = listOf(
        "com.android.dialer",          // Phone
        "com.android.messaging",       // Messaging
        "com.google.android.apps.maps",// Maps
        "com.google.android.music",    // Music
        "com.whatsapp",                // WhatsApp
        "com.google.android.keep",     // Notes
        "camera"                       // Camera
    )

    val displayTiles = tiles.filter { tile ->
        essentialPackages.any { essential -> tile.packageName.contains(essential, ignoreCase = true) }
    }.take(7).toMutableList()

    if (displayTiles.size < 7) {
        val remaining = tiles.filter { it.isInstalled && !displayTiles.contains(it) }.take(7 - displayTiles.size)
        displayTiles.addAll(remaining)
    }

    // Sort alphabetically by label
    val sortedTiles = displayTiles.sortedBy { it.label.lowercase() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        Text(
            text = timerText,
            style = DotzType.timeStyle().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = DotzTheme.colors.accent.copy(alpha = 0.6f)
        )

        // Essentials List
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortedTiles.forEach { tile ->
                UltraFocusItem(
                    tile = tile,
                    onTap = { onTileTap(tile) }
                )
            }
        }

        // Exit button
        Surface(
            onClick = onEndSession,
            color = DotzTheme.colors.text.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "EXIT FOCUS MODE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black),
                color = DotzTheme.colors.text.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun UltraFocusItem(
    tile: AppTile,
    onTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onTap() }
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tile.label.lowercase(),
            style = DotzType.timeStyle().copy(
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.ExtraLight
            ),
            color = DotzTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        if (tile.usageTime != null) {
            Text(
                text = tile.usageTime!!,
                style = MaterialTheme.typography.labelSmall,
                color = DotzTheme.colors.text.copy(alpha = 0.3f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
