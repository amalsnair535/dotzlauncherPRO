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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
        
        // Hide system bars in settings too for seamless transition back home
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

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
                    onLightModeToggle = viewModel::setIsLightMode,
                    on24HourToggle    = viewModel::setIs24HourFormat,
                    onGrayscaleToggle = viewModel::setGrayscaleMode,
                    onVerticalScrollToggle = viewModel::setVerticalScrolling,
                    onEnableExtraPageToggle = viewModel::setEnableExtraPage,
                    onExtraTileCountChange = viewModel::setExtraTileCount,
                    onShowWeatherToggle = viewModel::setShowWeatherInfo,
                    onShowWallpaperToggle = viewModel::setShowWallpaper,
                    onEnableDashboardToggle = viewModel::setEnableDashboard,
                    onTransparencyChange = viewModel::setTileTransparency,
                    onLayoutStyleChange = viewModel::setLayoutStyle,
                    onWallpaperClick = viewModel::openWallpaperPicker,
                    onIconPackChange  = viewModel::setIconPackPackage,
                    iconPacks         = remember { viewModel.getInstalledIconPacks() },
                    onExport          = { exportLauncher.launch("dotz_backup.json") },
                    onImport          = { importLauncher.launch("application/json") },
                    isDefaultLauncher = uiState.isDefaultLauncher,
                    onSetDefault      = viewModel::openDefaultLauncherSettings,
                    isUpdateAvailable = uiState.isUpdateAvailable,
                    onAboutClick      = {
                        startActivity(Intent(this, DotzAboutActivity::class.java))
                    },
                    onUpgradeClick    = {
                        startActivity(Intent(this, DotzUpgradeActivity::class.java))
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
    onLightModeToggle: (Boolean) -> Unit,
    on24HourToggle: (Boolean) -> Unit,
    onGrayscaleToggle: (Boolean) -> Unit,
    onVerticalScrollToggle: (Boolean) -> Unit,
    onEnableExtraPageToggle: (Boolean) -> Unit,
    onExtraTileCountChange: (Int) -> Unit,
    onShowWeatherToggle: (Boolean) -> Unit,
    onShowWallpaperToggle: (Boolean) -> Unit,
    onEnableDashboardToggle: (Boolean) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onLayoutStyleChange: (String) -> Unit,
    onWallpaperClick: () -> Unit,
    onIconPackChange: (String?) -> Unit,
    iconPacks: List<Pair<String, String>>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    isDefaultLauncher: Boolean,
    onSetDefault: () -> Unit,
    isUpdateAvailable: Boolean,
    onAboutClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onAppSelectionClick: () -> Unit,
) {
    var showIconPackDialog by remember { mutableStateOf(value = false) }
    var showExperimentalDashboardDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DOTZ SETTINGS",
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Normal,
                            color = DotzTheme.colors.text,
                        )
                        if (settings.isPremium) {
                            Spacer(Modifier.width(8.dp))
                            PremiumBadge()
                        }
                    }
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
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Section: Premium ──────────────────────────────────────────
            if (!settings.isPremium) {
                item { SectionHeader("GET PRO") }
                item {
                    PremiumPromotionCard(onClick = onUpgradeClick)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

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

            // ── Section: Clock ──────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("CLOCK") }
            item {
                SettingsToggleRow(
                    label   = "24-Hour Format",
                    checked = settings.is24HourFormat,
                    onToggle = on24HourToggle
                )
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
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
            }

            // ── Section: Appearance ───────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("APPEARANCE") }
            item {
                SettingsActionRow(
                    label = "Change Wallpaper",
                    isPremium = true,
                    isLocked = !settings.isPremium,
                    onClick = {
                        if (settings.isPremium) onWallpaperClick()
                        else showPremiumDialog = true
                    }
                )
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
                            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Extra Tiles Count", color = DotzTheme.colors.text, fontSize = 14.sp)
                            Text(
                                "${settings.extraTileCount}",
                                color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 14.sp
                            )
                        }
                        Slider(
                            value         = settings.extraTileCount.toFloat(),
                            onValueChange = { onExtraTileCountChange(it.toInt()) },
                            valueRange    = 1f..6f,
                            steps         = 4,
                            colors        = SliderDefaults.colors(
                                thumbColor       = DotzTheme.colors.text,
                                activeTrackColor = DotzTheme.colors.text,
                                inactiveTrackColor = DotzTheme.colors.text.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
            item {
                SettingsToggleRow(
                    label   = "Light Mode",
                    checked = settings.isLightMode,
                    onToggle = onLightModeToggle
                )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tile Layout", color = DotzTheme.colors.text, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        PremiumBadge()
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("classic", "list").forEach { style ->
                            val isSelected = settings.layoutStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) DotzTheme.colors.text else DotzTheme.colors.background,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (settings.isPremium) onLayoutStyleChange(style)
                                        else showPremiumDialog = true
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    style.uppercase(),
                                    color = if (isSelected) DotzTheme.colors.background else DotzTheme.colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tile Transparency", color = DotzTheme.colors.text, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            PremiumBadge()
                        }
                        Text(
                            "${(settings.tileTransparency * 100).toInt()}%",
                            color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = settings.tileTransparency,
                        onValueChange = {
                            if (settings.isPremium) onTransparencyChange(it)
                            else showPremiumDialog = true
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (settings.isPremium) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.2f),
                            activeTrackColor = if (settings.isPremium) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.1f),
                            inactiveTrackColor = DotzTheme.colors.text.copy(alpha = 0.1f)
                        )
                    )
                }
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
                    label   = "Show Wallpaper",
                    checked = settings.showWallpaper,
                    isPremium = true,
                    isLocked = !settings.isPremium,
                    onToggle = {
                        if (settings.isPremium) onShowWallpaperToggle(it)
                        else showPremiumDialog = true
                    }
                )
            }

            item {
                SettingsToggleRow(
                    label   = "Dashboard",
                    checked = settings.enableDashboard,
                    onToggle = { enabled ->
                        if (enabled) showExperimentalDashboardDialog = true
                        onEnableDashboardToggle(enabled)
                    }
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
                    label = if (isUpdateAvailable) "Update Available!" else "About Dotz",
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

    if (showExperimentalDashboardDialog) {
        AlertDialog(
            onDismissRequest = { showExperimentalDashboardDialog = false },
            containerColor = DotzTheme.colors.tile,
            title = { Text("Experimental Feature", color = DotzTheme.colors.text, fontSize = 16.sp) },
            text = { Text("The Dashboard is currently an experimental feature and may contain bugs.", color = DotzTheme.colors.text.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showExperimentalDashboardDialog = false }) {
                    Text("GOT IT", color = DotzTheme.colors.text)
                }
            }
        )
    }
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            containerColor = DotzTheme.colors.tile,
            title = { Text("PRO Feature", color = DotzTheme.colors.text, fontSize = 16.sp) },
            text = { Text("Transparency, Wallpaper, and List Layout are exclusive to Dotz PRO users.", color = DotzTheme.colors.text.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumDialog = false
                    onUpgradeClick()
                }) {
                    Text("GO PRO", color = DotzTheme.colors.text)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("NOT NOW", color = DotzTheme.colors.text.copy(alpha = 0.4f))
                }
            }
        )
    }
}

@Composable
private fun PremiumPromotionCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DotzTheme.colors.text, DotzTheme.colors.text.copy(alpha = 0.8f))
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Unlock Dotz PRO",
                    color = DotzTheme.colors.background,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = DotzTheme.colors.background,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Access Transparency, Custom Wallpapers, List Layout and more exclusive features.",
                color = DotzTheme.colors.background.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "UPGRADE NOW",
                color = DotzTheme.colors.background,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun PremiumBadge() {
    Box(
        modifier = Modifier
            .background(DotzTheme.colors.text.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text("PRO", color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppSelectionMenuRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("App Selection", color = DotzTheme.colors.text, fontSize = 14.sp)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzTheme.colors.text.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (isLocked) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 14.sp)
            if (isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Icon(
            if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzTheme.colors.text.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
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
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Icon Pack", color = DotzTheme.colors.text, fontSize = 14.sp)
            Text(
                displayName,
                color    = DotzTheme.colors.text.copy(alpha = 0.35f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzTheme.colors.text.copy(alpha = 0.4f)
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
        containerColor   = DotzTheme.colors.tile,
        title = { Text("Select Icon Pack", color = DotzTheme.colors.text, fontSize = 16.sp) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text(
                        "Default",
                        color = if (currentIconPack == null) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.6f),
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
                        color = if (currentIconPack == pkg) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.6f),
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
                Text("CANCEL", color = DotzTheme.colors.text, fontSize = 13.sp)
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
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DotzTheme.colors.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = DotzTheme.colors.text, fontSize = 14.sp)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text       = text,
        color      = DotzTheme.colors.text.copy(alpha = 0.4f),
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
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(label, color = if (isLocked) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 14.sp)
            if (isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            enabled         = !isLocked || checked, // Allow disabling if somehow enabled, but mostly locked
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = DotzTheme.colors.background,
                checkedTrackColor  = if (isLocked) DotzTheme.colors.text.copy(alpha = 0.2f) else DotzTheme.colors.text,
                uncheckedThumbColor = DotzTheme.colors.text.copy(alpha = 0.4f),
                uncheckedTrackColor = DotzTheme.colors.tile
            )
        )
    }
}

