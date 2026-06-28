@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcherpro.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import com.dotz.launcherpro.utils.IconUtils

@Composable
fun AppTileCard(
    tile: AppTile,
    iconCache: IconCacheManager,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadge: Boolean,
    transparency: Float = 1.0f,
    isHighlighted: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Press animation
    val pressScale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val highlightScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val currentScale = if (isHighlighted) highlightScale else pressScale.value

    val baseOpacity = if (tile.isInstalled) transparency else 0.4f * transparency
    val tileBackground = if (isHighlighted) DotzTheme.colors.text.copy(alpha = 0.2f) else DotzTheme.colors.tile.copy(alpha = baseOpacity)
    
    // Check cache first, then load if needed
    val cachedBitmap = remember(tile.packageName, iconPackPackage, grayscale) {
        iconCache.getIcon(tile.packageName, iconPackPackage, grayscale)
    }

    val colorFilter = if (grayscale && (cachedBitmap == null)) {
        // Only apply color filter if we didn't get a grayscale-rendered bitmap from cache
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(currentScale)
            .clip(RoundedCornerShape(28.dp))
            .background(tileBackground)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTap()
                },
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Determine icon source
                val iconAny = remember(tile.packageName, iconPackPackage, grayscale) {
                    iconCache.getIcon(tile.packageName, iconPackPackage, grayscale)
                        ?: IconUtils.loadAppIcon(context, tile.packageName, iconPackPackage, iconCache)?.also {
                            iconCache.saveIcon(tile.packageName, iconPackPackage, grayscale, it)
                        }
                }

                if (iconAny != null) {
                    val bitmap = remember(iconAny, grayscale) {
                        when (iconAny) {
                            is android.graphics.Bitmap -> iconAny.asImageBitmap()
                            is Drawable -> {
                                val width = if (iconAny.intrinsicWidth > 0) iconAny.intrinsicWidth else 512
                                val height = if (iconAny.intrinsicHeight > 0) iconAny.intrinsicHeight else 512
                                iconAny.toBitmap(width, height).asImageBitmap()
                            }
                            else -> null
                        }
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            painter     = BitmapPainter(bitmap),
                            contentDescription = tile.label,
                            modifier    = Modifier.size(36.dp),
                            colorFilter = colorFilter
                        )
                    }
                } else {
                    Icon(
                        imageVector        = tile.iconVector,
                        contentDescription = tile.label,
                        tint               = DotzTheme.colors.text,
                        modifier           = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text      = tile.label,
                style     = DotzType.tileLabelStyle(),
                color     = DotzTheme.colors.text,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )

            if (tile.usageTime != null) {
                Text(
                    text = tile.usageTime,
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // ── Badge ─────────────────────────────────────────────────────────────
        if (showBadge && (tile.badgeCount >= 0)) {
            val showCount = tile.badgeCount > 0
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(if (showCount) 20.dp else 8.dp)
                    .clip(CircleShape)
                    .background(DotzTheme.colors.badgeDot),
                contentAlignment = Alignment.Center
            ) {
                if (showCount) {
                    Text(
                        text  = if (tile.badgeCount > 99) "99+" else tile.badgeCount.toString(),
                        color = if (DotzTheme.colors.badgeDot == Color.White) Color.Black else Color.White,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
