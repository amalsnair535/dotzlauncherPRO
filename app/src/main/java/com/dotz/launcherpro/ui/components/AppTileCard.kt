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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzType

@Composable
fun AppTileCard(
    tile: AppTile,
    iconCache: IconCacheManager,
    tileOpacity: Float,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadge: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Press animation
    val pressScale = remember { Animatable(1f) }

    val baseOpacity = if (tile.isInstalled) tileOpacity else (tileOpacity * 0.4f)
    val tileBackground = DotzColors.Tile.copy(alpha = baseOpacity)
    
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
            .scale(pressScale.value)
            .clip(RoundedCornerShape(28.dp))
            .background(tileBackground)
            .combinedClickable(
                onClick = onTap,
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
                        ?: loadAppIcon(context, tile.packageName, iconPackPackage, iconCache)?.also {
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
                        tint               = DotzColors.White,
                        modifier           = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text      = tile.label,
                style     = DotzType.TileLabelStyle,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
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
                    .background(DotzColors.BadgeDot),
                contentAlignment = Alignment.Center
            ) {
                if (showCount) {
                    Text(
                        text  = if (tile.badgeCount > 99) "99+" else tile.badgeCount.toString(),
                        color = DotzColors.Background,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun loadAppIcon(
    context: Context,
    packageName: String,
    iconPackPackage: String?,
    iconCache: IconCacheManager
): Drawable? {
    val pm = context.packageManager

    // 1. Try to load from icon pack if selected
    if (iconPackPackage != null) {
        try {
            val iconPackRes = pm.getResourcesForApplication(iconPackPackage)
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val component = launchIntent?.component
            
            // 1a. Try appfilter.xml mapping (The most reliable way)
            if (component != null) {
                val componentStr = component.flattenToString()
                val drawableName = iconCache.getDrawableName(componentStr, iconPackPackage)
                if (drawableName != null) {
                    val resId = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
                    if (resId != 0) return iconPackRes.getDrawable(resId, null)
                }
            }

            // 1b. Try to find entry by package name with underscores
            val resId = iconPackRes.getIdentifier(packageName.replace(".", "_"), "drawable", iconPackPackage)
            if (resId != 0) return iconPackRes.getDrawable(resId, null)

            // 1c. Try to find entry by lowercase package name
            val resIdLower = iconPackRes.getIdentifier(packageName.lowercase().replace(".", "_"), "drawable", iconPackPackage)
            if (resIdLower != 0) return iconPackRes.getDrawable(resIdLower, null)
            
            // 1d. Try several naming conventions based on component
            if (component != null) {
                // Try several naming conventions used by icon packs
                
                // Full component name: com.android.settings/com.android.settings.Settings
                val fullComp = component.flattenToString().replace(".", "_").replace("/", "_")
                val resId2 = iconPackRes.getIdentifier(fullComp, "drawable", iconPackPackage)
                if (resId2 != 0) return iconPackRes.getDrawable(resId2, null)

                // Just the class name (often used in older icon packs)
                val className = component.className.replace(".", "_")
                val resId3 = iconPackRes.getIdentifier(className, "drawable", iconPackPackage)
                if (resId3 != 0) return iconPackRes.getDrawable(resId3, null)

                // Class name without package
                val shortClassName = component.className.substringAfterLast(".").lowercase()
                val resId4 = iconPackRes.getIdentifier(shortClassName, "drawable", iconPackPackage)
                if (resId4 != 0) return iconPackRes.getDrawable(resId4, null)
            }
        } catch (_: Exception) {}
    }

    // 2. Fallback to system default app icon
    return try {
        pm.getApplicationIcon(packageName)
    } catch (_: Exception) { null }
}
