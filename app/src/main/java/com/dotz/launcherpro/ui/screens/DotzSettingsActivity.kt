package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

class DotzSettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            val json = viewModel.exportSettings()
                            contentResolver.openOutputStream(it)?.use { out ->
                                out.write(json.toByteArray())
                            }
                            Toast.makeText(context, "Settings exported", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { fileUri ->
                        scope.launch {
                            val json = contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { reader -> reader.readText() }
                            if (json != null) {
                                val success = viewModel.importSettings(json)
                                if (success) {
                                    Toast.makeText(context, "Settings imported", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                DotzSettingsScreen(
                    settings          = uiState.settings,
                    onBack            = { finish() },
                    onShowDots        = viewModel::setShowNotificationDots,
                    onShowCounts      = viewModel::setShowNumericalCounts,
                    onNotificationFilterToggle = viewModel::setNotificationFilterEnabled,
                    onOpacityChange   = viewModel::setTileOpacity,
                    onGrayscaleToggle = viewModel::setGrayscaleMode,
                    onVerticalScrollToggle = viewModel::setVerticalScrolling,
                    onEnableExtraPageToggle = viewModel::setEnableExtraPage,
                    onExtraTileCountChange = viewModel::setExtraTileCount,
                    onDynamicBgToggle = viewModel::setDynamicBackgroundEnabled,
                    onShowWeatherToggle = viewModel::setShowWeatherInfo,
                    onEnableDashboardToggle = viewModel::setEnableDashboard,
                    onIconPackChange  = viewModel::setIconPackPackage,
                    iconPacks         = remember { viewModel.getInstalledIconPacks() },
                    onExport          = { exportLauncher.launch("dotz_backup.json") },
                    onImport          = { importLauncher.launch("application/json") },
                    isDefaultLauncher = uiState.isDefaultLauncher,
                    onSetDefault      = viewModel::openDefaultLauncherSettings,
                    onAboutClick      = {
                        startActivity(Intent(this, DotzAboutActivity::class.java))
                    }
                ) {
                    startActivity(Intent(this, AppSelectionListActivity::class.java))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DotzSettingsScreen(
    settings: com.dotz.launcherpro.data.DotzSettings,
    onBack: () -> Unit,
    onShowDots: (Boolean) -> Unit,
    onShowCounts: (Boolean) -> Unit,
    onNotificationFilterToggle: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onGrayscaleToggle: (Boolean) -> Unit,
    onVerticalScrollToggle: (Boolean) -> Unit,
    onEnableExtraPageToggle: (Boolean) -> Unit,
    onExtraTileCountChange: (Int) -> Unit,
    onDynamicBgToggle: (Boolean) -> Unit,
    onShowWeatherToggle: (Boolean) -> Unit,
    onEnableDashboardToggle: (Boolean) -> Unit,
    onIconPackChange: (String?) -> Unit,
    iconPacks: List<Pair<String, String>>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    isDefaultLauncher: Boolean,
    onSetDefault: () -> Unit,
    onAboutClick: () -> Unit,
    onAppSelectionClick: () -> Unit,
) {
    var showIconPackDialog by remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DOTZ SETTINGS",
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
            // ── Section: App Selection ────────────────────────────────────
            item { SectionHeader("APP SELECTION") }
            item {
                AppSelectionMenuRow(onClick = onAppSelectionClick)
            }

            // ── Section: General ──────────────────────────────────────────
            if (!isDefaultLauncher) {
                item { Spacer(Modifier.height(8.dp)); SectionHeader("GENERAL") }
                item {
                    SettingsActionRow(
                        label = "Set Dotz as Default Launcher",
                        onClick = onSetDefault
                    )
                }
            }

            // ── Section: Notifications ────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("NOTIFICATIONS") }
            item {
                SettingsToggleRow(
                    label   = "Show Notification Dots",
                    checked = settings.showNotificationDots,
                    onToggle = onShowDots
                )
            }
            item {
                SettingsToggleRow(
                    label   = "Show Numerical Counts for Calls/SMS",
                    checked = settings.showNumericalCounts,
                    onToggle = onShowCounts
                )
            }
            item {
                SettingsToggleRow(
                    label   = "Filter Distracting Notifications",
                    checked = settings.notificationFilterEnabled,
                    onToggle = onNotificationFilterToggle
                )
                Text(
                    "Hides dots for social media apps",
                    color = DotzColors.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
            }

            // ── Section: Appearance ───────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("APPEARANCE") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DotzColors.Tile, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tile Opacity", color = DotzColors.White, fontSize = 14.sp)
                        Text(
                            "${(settings.tileOpacity * 100).toInt()}%",
                            color = DotzColors.White.copy(alpha = 0.5f), fontSize = 14.sp
                        )
                    }
                    Slider(
                        value         = settings.tileOpacity,
                        onValueChange = onOpacityChange,
                        valueRange    = 0.6f..1.0f,
                        colors        = SliderDefaults.colors(
                            thumbColor       = DotzColors.White,
                            activeTrackColor = DotzColors.White,
                            inactiveTrackColor = DotzColors.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            item {
                SettingsToggleRow(
                    label   = "Enable Extra Tiles (Page 3)",
                    checked = settings.enableExtraPage,
                    onToggle = onEnableExtraPageToggle
                )
            }

            if (settings.enableExtraPage) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Extra Tiles Count", color = DotzColors.White, fontSize = 14.sp)
                            Text(
                                "${settings.extraTileCount}",
                                color = DotzColors.White.copy(alpha = 0.5f), fontSize = 14.sp
                            )
                        }
                        Slider(
                            value         = settings.extraTileCount.toFloat(),
                            onValueChange = { onExtraTileCountChange(it.toInt()) },
                            valueRange    = 1f..6f,
                            steps         = 4,
                            colors        = SliderDefaults.colors(
                                thumbColor       = DotzColors.White,
                                activeTrackColor = DotzColors.White,
                                inactiveTrackColor = DotzColors.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
            item {
                SettingsToggleRow(
                    label   = "Grayscale Mode",
                    checked = settings.grayscaleMode,
                    onToggle = onGrayscaleToggle
                )
            }

            item {
                SettingsToggleRow(
                    label   = "Vertical Scrolling",
                    checked = settings.verticalScrolling,
                    onToggle = onVerticalScrollToggle
                )
            }

            item {
                SettingsToggleRow(
                    label   = "Dynamic Background",
                    checked = settings.dynamicBackgroundEnabled,
                    onToggle = onDynamicBgToggle
                )
            }

            item {
                SettingsToggleRow(
                    label   = "Show Weather Info",
                    checked = settings.showWeatherInfo,
                    onToggle = onShowWeatherToggle
                )
            }

            item {
                SettingsToggleRow(
                    label   = "Enable Swipe-Left Dashboard",
                    checked = settings.enableDashboard,
                    onToggle = onEnableDashboardToggle
                )
            }

            item {
                IconPackSelectionRow(
                    currentIconPack = settings.iconPackPackage ?: "Default",
                    iconPacks = iconPacks,
                    onClick = { showIconPackDialog = true }
                )
            }

            // ── Section: Backup & Restore ─────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("BACKUP & RESTORE") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackupButton(
                        label   = "Export",
                        icon    = Icons.Default.Download,
                        modifier = Modifier.weight(1f),
                        onClick = onExport
                    )
                    BackupButton(
                        label   = "Import",
                        icon    = Icons.Default.Upload,
                        modifier = Modifier.weight(1f),
                        onClick = onImport
                    )
                }
            }

            // ── Section: About ────────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("INFO") }
            item {
                SettingsActionRow(
                    label = "About Dotz",
                    onClick = onAboutClick
                )
            }
        }
    }

    if (showIconPackDialog) {
        IconPackDialog(
            currentIconPack = settings.iconPackPackage,
            iconPacks = iconPacks,
            onSelect = { pkg ->
                onIconPackChange(pkg)
                showIconPackDialog = false
            },
            onDismiss = { showIconPackDialog = false },
        )
    }
}

@Composable
private fun AppSelectionMenuRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("App Selection", color = DotzColors.White, fontSize = 14.sp)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzColors.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingsActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = DotzColors.White, fontSize = 14.sp)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzColors.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun IconPackSelectionRow(
    currentIconPack: String,
    iconPacks: List<Pair<String, String>>,
    onClick: () -> Unit
) {
    val displayName = if (currentIconPack == "Default") "Default" 
                      else iconPacks.find { it.first == currentIconPack }?.second ?: currentIconPack

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
            Text("Icon Pack", color = DotzColors.White, fontSize = 14.sp)
            Text(
                displayName,
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

@Composable
private fun IconPackDialog(
    currentIconPack: String?,
    iconPacks: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DotzColors.Tile,
        title = { Text("Select Icon Pack", color = DotzColors.White, fontSize = 16.sp) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text(
                        "Default",
                        color = if (currentIconPack == null) DotzColors.White else DotzColors.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp),
                        fontSize = 14.sp
                    )
                }
                items(iconPacks.size) { index ->
                    val (pkg, name) = iconPacks[index]
                    Text(
                        name,
                        color = if (currentIconPack == pkg) DotzColors.White else DotzColors.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(pkg) }
                            .padding(vertical = 12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = DotzColors.White, fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun BackupButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DotzColors.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = DotzColors.White, fontSize = 14.sp)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text       = text,
        color      = DotzColors.White.copy(alpha = 0.4f),
        fontSize   = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        modifier   = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = DotzColors.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.Black,
                checkedTrackColor  = DotzColors.White,
                uncheckedThumbColor = DotzColors.White.copy(alpha = 0.4f),
                uncheckedTrackColor = DotzColors.Tile
            )
        )
    }
}

