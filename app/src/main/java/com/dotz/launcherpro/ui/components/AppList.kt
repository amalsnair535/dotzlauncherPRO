package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
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
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top)
    ) {
        tiles.forEach { tile ->
            AppListTile(
                tile = tile,
                iconCache = iconCache,
                grayscale = grayscale,
                iconPackPackage = iconPackPackage,
                showBadge = showBadges,
                transparency = transparency,
                onTap = { onTileTap(tile) },
                onLongPress = { onTileLongPress(tile) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppListTile(
    tile: AppTile,
    iconCache: IconCacheManager,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadge: Boolean,
    transparency: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val baseOpacity = if (tile.isInstalled) transparency else 0.4f * transparency
    val tileBackground = DotzTheme.colors.tile.copy(alpha = baseOpacity)

    val cachedBitmap = remember(tile.packageName, iconPackPackage, grayscale) {
        iconCache.getIcon(tile.packageName, iconPackPackage, grayscale)
    }

    val colorFilter = if (grayscale && cachedBitmap == null) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tileBackground)
            .combinedClickable(
                onClick = onTap,
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

        // Label
        Text(
            text = tile.label,
            style = DotzType.TileLabelStyle.copy(fontSize = 18.sp),
            color = DotzTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // Badge could be added here if needed, but the image doesn't show any.
    }
}
