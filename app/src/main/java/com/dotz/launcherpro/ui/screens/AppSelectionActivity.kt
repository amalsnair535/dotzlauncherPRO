package com.dotz.launcherpro.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.data.DrawerApp
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class AppSelectionActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val tileId = intent.getIntExtra("tileId", -1)
        val tileLabel = intent.getStringExtra("tileLabel") ?: "APP"
        if (tileId == -1) { finish(); return }

        val installedApps = viewModel.getInstalledAppsForTile(tileId)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                AppSelectionScreen(
                    apps    = installedApps,
                    title   = "SELECT $tileLabel APP",
                    onBack  = { finish() }
                ) { pkg, label ->
                    viewModel.updateTileOverride(tileId, pkg, label.uppercase())
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionScreen(
    apps: List<DrawerApp>,
    title: String,
    onBack: () -> Unit,
    onSelect: (String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        val list = if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
        list.sortedBy { it.label.uppercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Normal,
                        color = DotzTheme.colors.text,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DotzTheme.colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DotzTheme.colors.background),
            )
        },
        containerColor = DotzTheme.colors.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder   = { Text("Search apps…", color = DotzTheme.colors.text.copy(alpha = 0.3f)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = DotzTheme.colors.text.copy(alpha = 0.5f)) },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DotzTheme.colors.text.copy(alpha = 0.4f),
                    unfocusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.15f),
                    focusedTextColor     = DotzTheme.colors.text,
                    unfocusedTextColor   = DotzTheme.colors.text,
                    cursorColor          = DotzTheme.colors.text
                )
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { app ->
                    AppRow(
                        pkg = app.packageName, 
                        label = app.label, 
                        onClick = { 
                            onSelect(app.packageName, app.label)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(pkg: String, label: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(pkg) { loadIcon(context, pkg) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DotzTheme.colors.tile)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            val bmp = remember(icon) { icon.toBitmap().asImageBitmap() }
            Image(
                bitmap             = bmp,
                contentDescription = label,
                modifier           = Modifier.size(36.dp)
            )
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = DotzTheme.colors.text, fontSize = 14.sp)
            Text(pkg, color = DotzTheme.colors.text.copy(alpha = 0.35f), fontSize = 10.sp)
        }
    }
}

private fun loadIcon(context: Context, pkg: String): Drawable? = try {
    context.packageManager.getApplicationIcon(pkg)
} catch (_: Exception) { null }
