package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import com.dotz.launcherpro.utils.IconUtils

/**
 * List layout similar to the provided image.
 */
@Composable
fun AppList(
    tiles: List<AppTile>,
    iconCache: IconCacheManager,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadges: Boolean,
    transparency: Float = 1.0f,
    highlightedTileId: Int? = null,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        tiles.forEach { tile ->
            AppListTile(
                tile = tile,
                iconCache = iconCache,
                grayscale = grayscale,
                iconPackPackage = iconPackPackage,
                showBadge = showBadges,
                transparency = transparency,
                isHighlighted = tile.tileId == highlightedTileId,
                onTap = { onTileTap(tile) },
                onLongPress = { onTileLongPress(tile) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListTile(
    tile: AppTile,
    iconCache: IconCacheManager,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadge: Boolean,
    transparency: Float,
    isHighlighted: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val baseOpacity = if (tile.isInstalled) transparency else 0.4f * transparency
    val tileBackground = if (isHighlighted) DotzTheme.colors.text.copy(alpha = 0.2f) else DotzTheme.colors.tile.copy(alpha = baseOpacity)

    val cachedBitmap = remember(tile.packageName, iconPackPackage, grayscale) {
        iconCache.getIcon(tile.packageName, iconPackPackage, grayscale)
    }

    val colorFilter = if (grayscale && cachedBitmap == null) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tileBackground)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTap()
                },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
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
                        is android.graphics.drawable.Drawable -> {
                            val width = if (iconAny.intrinsicWidth > 0) iconAny.intrinsicWidth else 512
                            val height = if (iconAny.intrinsicHeight > 0) iconAny.intrinsicHeight else 512
                            iconAny.toBitmap(width, height).asImageBitmap()
                        }
                        else -> null
                    }
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        painter = BitmapPainter(bitmap),
                        contentDescription = tile.label,
                        modifier = Modifier.size(40.dp),
                        colorFilter = colorFilter
                    )
                }
            } else {
                Icon(
                    imageVector = tile.iconVector,
                    contentDescription = tile.label,
                    tint = DotzTheme.colors.text,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // Label and Usage Time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tile.label,
                style = DotzType.tileLabelStyle().copy(fontSize = 18.sp),
                color = DotzTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (tile.usageTime != null) {
                Text(
                    text = tile.usageTime,
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Badge ─────────────────────────────────────────────────────────────
        if (showBadge && (tile.badgeCount >= 0)) {
            val showCount = tile.badgeCount > 0
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(if (showCount) 24.dp else 10.dp)
                    .clip(CircleShape)
                    .background(DotzTheme.colors.badgeDot),
                contentAlignment = Alignment.Center
            ) {
                if (showCount) {
                    Text(
                        text = if (tile.badgeCount > 99) "99+" else tile.badgeCount.toString(),
                        color = if (DotzTheme.colors.badgeDot == Color.White) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
