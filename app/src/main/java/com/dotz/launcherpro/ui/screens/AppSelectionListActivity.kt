package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class AppSelectionListActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                AppSelectionListScreen(
                    page0 = uiState.page0Tiles,
                    page1 = uiState.page1Tiles,
                    page2 = uiState.page2Tiles,
                    onBack = { finish() },
                ) { tile ->
                    startActivity(
                        Intent(this, AppSelectionActivity::class.java)
                            .putExtra("tileId", tile.tileId)
                            .putExtra("tileLabel", tile.label),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionListScreen(
    page0: List<com.dotz.launcherpro.data.AppTile>,
    page1: List<com.dotz.launcherpro.data.AppTile>,
    page2: List<com.dotz.launcherpro.data.AppTile>,
    onBack: () -> Unit,
    onRemapTile: (com.dotz.launcherpro.data.AppTile) -> Unit,
) {
    val allTiles = page0 + page1 + page2

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "APP SELECTION",
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Normal,
                        color = DotzColors.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DotzColors.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
        containerColor = Color.Black,
    ) { innerPadding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allTiles.size) { index ->
                val tile = allTiles[index]
                TileRemapRow(
                    label    = tile.label,
                    pkg      = tile.packageName,
                    onClick  = { onRemapTile(tile) }
                )
            }
        }
    }
}

@Composable
private fun TileRemapRow(label: String, pkg: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = DotzColors.White, fontSize = 14.sp)
            Text(
                pkg,
                color    = DotzColors.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzColors.White.copy(alpha = 0.4f)
        )
    }
}
