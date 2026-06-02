package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.data.IconCacheManager

/**
 * 2-column × 3-row grid of [AppTileCard]s.
 * Restores classic spacing while maintaining centered alignment.
 */
@Composable
fun AppGrid(
    tiles: List<AppTile>,
    iconCache: IconCacheManager,
    grayscale: Boolean,
    iconPackPackage: String?,
    showBadges: Boolean,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = tiles.chunked(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp), // Balanced padding for classic tile size
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top) // Old build spacing
    ) {
        rows.forEach { rowTiles ->
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowTiles.forEach { tile ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AppTileCard(
                            tile         = tile,
                            iconCache    = iconCache,
                            grayscale    = grayscale,
                            iconPackPackage = iconPackPackage,
                            showBadge    = showBadges,
                            onTap        = { onTileTap(tile) },
                            onLongPress  = { onTileLongPress(tile) },
                            modifier     = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                }
                if (rowTiles.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
