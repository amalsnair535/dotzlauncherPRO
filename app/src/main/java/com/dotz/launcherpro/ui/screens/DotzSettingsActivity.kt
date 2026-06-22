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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import com.dotz.launcherpro.viewmodel.ThemeMode
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
            // Force Dark Theme for Settings screen regardless of launcher settings
            val settingsForSettings = uiState.settings.copy(isLightMode = false, useCircadianTheming = false, showWallpaper = false, tileTransparency = 1.0f)
            DotzTheme(settings = settingsForSettings) {
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

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                  permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        viewModel.setShowWeatherInfo(true)
                        viewModel.refreshWeather()
                    } else {
                        Toast.makeText(context, "Location permission required for weather", Toast.LENGTH_SHORT).show()
                    }
                }

                DotzSettingsScreen(
                    settings          = uiState.settings,
                    isUpgradeAvailable = uiState.isUpgradeAvailable,
                    onBack            = { finish() },
                    onShowDots        = viewModel::setShowNotificationDots,
                    onShowCounts      = viewModel::setShowNumericalCounts,
                    onNotificationFilterToggle = viewModel::setNotificationFilterEnabled,
                    onGrayscaleToggle = viewModel::setGrayscaleMode,
                    onVerticalScrollToggle = viewModel::setVerticalScrolling,
                    onEnableExtraPageToggle = viewModel::setEnableExtraPage,
                    onExtraTileCountChange = viewModel::setExtraTileCount,
                    onShowWeatherToggle = { enabled ->
                        if (enabled) {
                            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                viewModel.setShowWeatherInfo(true)
                                viewModel.refreshWeather()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        } else {
                            viewModel.setShowWeatherInfo(false)
                        }
                    },
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
                    onThemeModeChange = viewModel::setThemeMode,
                    onEnableAppDrawerToggle = viewModel::setEnableAppDrawer,
                    onShowMindfulUsageToggle = viewModel::setShowMindfulUsage,
                    isUpdateAvailable = uiState.isUpdateAvailable,
                    isLiteVersion     = uiState.isLiteVersion,
                    onAboutClick      = {
                        startActivity(Intent(this, DotzAboutActivity::class.java))
                    },
                    onUpgradeClick    = {
                        startActivity(Intent(this, DotzUpgradeActivity::class.java))
                    },
                    onAppSelectionClick = {
                        startActivity(Intent(this, AppSelectionListActivity::class.java))
                    },
                    onCreateProfile = viewModel::createProfile,
                    onDeleteProfile = viewModel::deleteProfile,
                    onSwitchProfile = viewModel::switchProfile,
                    onFontChange      = viewModel::setFontId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DotzSettingsScreen(
    settings: com.dotz.launcherpro.data.DotzSettings,
    isUpgradeAvailable: Boolean,
    onBack: () -> Unit,
    onShowDots: (Boolean) -> Unit,
    onShowCounts: (Boolean) -> Unit,
    onNotificationFilterToggle: (Boolean) -> Unit,
    onGrayscaleToggle: (Boolean) -> Unit,
    onVerticalScrollToggle: (Boolean) -> Unit,
    onEnableExtraPageToggle: (Boolean) -> Unit,
    onExtraTileCountChange: (Int) -> Unit,
    onShowWeatherToggle: (Boolean) -> Unit,
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
    isLiteVersion: Boolean,
    onAboutClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onAppSelectionClick: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onEnableAppDrawerToggle: (Boolean) -> Unit,
    onShowMindfulUsageToggle: (Boolean) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onFontChange: (String) -> Unit,
) {
    var showIconPackDialog by remember { mutableStateOf(value = false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showExperimentalDashboardDialog by remember { mutableStateOf(false) }
    var showAppDrawerWarningDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }

    val currentThemeMode = remember(settings.isLightMode, settings.useCircadianTheming, settings.showWallpaper) {
        when {
            settings.showWallpaper -> ThemeMode.TRANSPARENT
            settings.useCircadianTheming -> ThemeMode.CIRCADIAN
            settings.isLightMode -> ThemeMode.LIGHT
            else -> ThemeMode.DARK
        }
    }

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
            if (!settings.isPremium && isUpgradeAvailable) {
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

            // ── Section: Profiles ─────────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)); SectionHeader("HOME SCREEN PROFILES") }
            item {
                ProfileManagementCard(
                    activeId = settings.activeProfileId,
                    profiles = settings.profiles,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    onSwitch = onSwitchProfile,
                    onDelete = onDeleteProfile,
                    onAddClick = { 
                        if (!isUpgradeAvailable || settings.isPremium) showCreateProfileDialog = true
                        else showPremiumDialog = true
                    }
                )
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
            item { Spacer(Modifier.height(8.dp)); SectionHeader("THEME MODE") }
            item {
                ThemeModeSelectionRow(
                    currentMode = currentThemeMode,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    isLiteVersion = isLiteVersion,
                    onModeChange = onThemeModeChange,
                    onUpgradeClick = onUpgradeClick,
                    onShowPremiumDialog = { showPremiumDialog = true }
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
            
            if (!isLiteVersion) {
                item {
                    SettingsActionRow(
                        label = "Change Wallpaper",
                        isPremium = isUpgradeAvailable,
                        isLocked = !settings.isPremium && isUpgradeAvailable,
                        onClick = {
                            if (!isUpgradeAvailable || settings.isPremium) onWallpaperClick()
                            else showPremiumDialog = true
                        }
                    )
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
                                Text("Tile Transparency", color = if (!settings.isPremium && isUpgradeAvailable) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 14.sp)
                                if (isUpgradeAvailable) {
                                    Spacer(Modifier.width(8.dp))
                                    PremiumBadge()
                                }
                            }
                            Text(
                                "${(settings.tileTransparency * 100).toInt()}%",
                                color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 14.sp
                            )
                        }
                        Slider(
                            value = settings.tileTransparency,
                            onValueChange = {
                                if (!isUpgradeAvailable || settings.isPremium) onTransparencyChange(it)
                                else showPremiumDialog = true
                            },
                            enabled = !isUpgradeAvailable || settings.isPremium,
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (!isUpgradeAvailable || settings.isPremium) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.2f),
                                activeTrackColor = if (!isUpgradeAvailable || settings.isPremium) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.1f),
                                inactiveTrackColor = DotzTheme.colors.text.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            if (!isLiteVersion) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tile Layout", color = if (!settings.isPremium && isUpgradeAvailable) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 14.sp)
                            if (isUpgradeAvailable) {
                                Spacer(Modifier.width(8.dp))
                                PremiumBadge()
                            }
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
                                            if (isSelected) Color.White else Color.Black,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (!isUpgradeAvailable || settings.isPremium) onLayoutStyleChange(style)
                                            else showPremiumDialog = true
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        style.uppercase(),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
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
                                thumbColor       = Color.Black,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
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
                    label   = "Show Weather Info",
                    checked = settings.showWeatherInfo,
                    onToggle = onShowWeatherToggle
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
                SettingsToggleRow(
                    label = "All Apps Drawer",
                    checked = settings.enableAppDrawer,
                    onToggle = { enabled ->
                        if (enabled) {
                            showAppDrawerWarningDialog = true
                        } else {
                            onEnableAppDrawerToggle(false)
                        }
                    }
                )
                Text(
                    "Enable swipe up for all apps",
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsToggleRow(
                    label = "Mindful Usage Tracking",
                    checked = settings.showMindfulUsage,
                    onToggle = onShowMindfulUsageToggle
                )
                Text(
                    "Show time spent in apps and launch limits",
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
            }

            item {
                IconPackSelectionRow(
                    currentIconPack = settings.iconPackPackage ?: "Default",
                    iconPacks = iconPacks,
                    onClick = { showIconPackDialog = true }
                )
            }

            item {
                FontSelectionRow(
                    currentFontId = settings.fontId,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    onClick = { 
                        if (!isUpgradeAvailable || settings.isPremium) showFontDialog = true
                        else showPremiumDialog = true
                    }
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

    if (showFontDialog) {
        FontSelectionDialog(
            currentFontId = settings.fontId,
            onSelect = { 
                onFontChange(it)
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false }
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

    if (showAppDrawerWarningDialog) {
        AlertDialog(
            onDismissRequest = { showAppDrawerWarningDialog = false },
            containerColor = DotzTheme.colors.tile,
            title = { Text("Enable All Apps?", color = DotzTheme.colors.text, fontSize = 16.sp) },
            text = { Text("Having access to all apps can decrease focus and increase screen time. Are you sure you want to enable this?", color = DotzTheme.colors.text.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onEnableAppDrawerToggle(true)
                    showAppDrawerWarningDialog = false
                }) {
                    Text("ENABLE", color = DotzTheme.colors.text)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppDrawerWarningDialog = false }) {
                    Text("CANCEL", color = DotzTheme.colors.text.copy(alpha = 0.4f))
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

    if (showCreateProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            containerColor = Color.Black,
            title = { Text("NEW PROFILE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a name for this profile (e.g. Work, Home). It will clone your current setup.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        placeholder = { Text("Profile Name", color = Color.White.copy(alpha = 0.2f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onCreateProfile(profileName)
                            showCreateProfileDialog = false
                        }
                    }
                ) {
                    Text("CREATE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.4f))
                }
            }
        )
    }
}

@Composable
private fun ProfileManagementCard(
    activeId: String,
    profiles: List<com.dotz.launcherpro.data.LauncherProfile>,
    isPremium: Boolean,
    isUpgradeAvailable: Boolean,
    onSwitch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        // Filter out "default" from the dynamic list as we show it specially at the top
        val customProfiles = profiles.filter { it.id != "default" }

        // Default Profile
        ProfileRow(
            name = "DEFAULT",
            isActive = activeId == "default",
            canDelete = false,
            onClick = { onSwitch("default") },
            onDelete = {}
        )
        
        customProfiles.forEach { profile ->
            val isLocked = isUpgradeAvailable && !isPremium
            ProfileRow(
                name = profile.name.uppercase(),
                isActive = activeId == profile.id,
                canDelete = true,
                isLocked = isLocked,
                onClick = { 
                    if (!isLocked) onSwitch(profile.id) 
                    else { /* Controlled by parent showPremiumDialog via onAddClick logic if needed, or handled here */ }
                },
                onDelete = { if (!isLocked) onDelete(profile.id) }
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        TextButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, tint = if (isUpgradeAvailable && !isPremium) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("CREATE NEW PROFILE", color = if (isUpgradeAvailable && !isPremium) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (isUpgradeAvailable && !isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
    }
}

@Composable
private fun ProfileRow(
    name: String,
    isActive: Boolean,
    canDelete: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = if (isActive) Color.Black else if (isLocked) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp
            )
            if (isLocked && !isActive) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Lock, null, tint = DotzTheme.colors.text.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
            }
        }
        
        if (canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp), enabled = !isLocked) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (isActive) Color.Black.copy(alpha = 0.4f) else DotzTheme.colors.text.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FontSelectionRow(
    currentFontId: String,
    isPremium: Boolean,
    isUpgradeAvailable: Boolean,
    onClick: () -> Unit
) {
    val fonts = listOf(
        "default" to "System Default",
        "inter" to "Inter",
        "manrope" to "Manrope",
        "roboto" to "Roboto",
        "ibm_plex_sans" to "IBM Plex Sans",
        "space_grotesk" to "Space Grotesk",
        "outfit" to "Outfit",
        "jakarta" to "Plus Jakarta Sans",
        "sora" to "Sora",
        "jetbrains_mono" to "JetBrains Mono",
        "ibm_plex_mono" to "IBM Plex Mono",
        "space_mono" to "Space Mono",
        "dm_sans" to "DM Sans",
        "instrument" to "Instrument Sans",
        "work_sans" to "Work Sans"
    )
    
    val displayName = fonts.find { it.first == currentFontId }?.second ?: "System Default"

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("App Font", color = if (isUpgradeAvailable && !isPremium) DotzTheme.colors.text.copy(alpha = 0.4f) else DotzTheme.colors.text, fontSize = 14.sp)
                if (isUpgradeAvailable) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            Text(
                displayName,
                color    = DotzTheme.colors.text.copy(alpha = 0.35f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Icon(
            if (isUpgradeAvailable && !isPremium) Icons.Default.Lock else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DotzTheme.colors.text.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FontSelectionDialog(
    currentFontId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val fonts = listOf(
        "default" to "System Default",
        "inter" to "Inter",
        "manrope" to "Manrope",
        "roboto" to "Roboto",
        "ibm_plex_sans" to "IBM Plex Sans",
        "space_grotesk" to "Space Grotesk",
        "outfit" to "Outfit",
        "jakarta" to "Plus Jakarta Sans",
        "sora" to "Sora",
        "jetbrains_mono" to "JetBrains Mono",
        "ibm_plex_mono" to "IBM Plex Mono",
        "space_mono" to "Space Mono",
        "dm_sans" to "DM Sans",
        "instrument" to "Instrument Sans",
        "work_sans" to "Work Sans"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DotzTheme.colors.tile,
        title = { Text("Select App Font", color = DotzTheme.colors.text, fontSize = 16.sp) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(fonts.size) { index ->
                    val (id, name) = fonts[index]
                    Text(
                        name,
                        color = if (currentFontId == id) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(id) }
                            .padding(vertical = 12.dp),
                        fontSize = 14.sp,
                        fontFamily = com.dotz.launcherpro.ui.theme.DotzType.getFontFamily(id)
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
private fun ThemeModeSelectionRow(
    currentMode: ThemeMode,
    isPremium: Boolean,
    isUpgradeAvailable: Boolean,
    isLiteVersion: Boolean,
    onModeChange: (ThemeMode) -> Unit,
    onUpgradeClick: () -> Unit,
    onShowPremiumDialog: () -> Unit
) {
    val modes = if (isLiteVersion) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK)
    } else {
        ThemeMode.entries
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        modes.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowModes.forEach { mode ->
                    val isSelected = currentMode == mode
                    val isProMode = mode == ThemeMode.CIRCADIAN || mode == ThemeMode.TRANSPARENT
                    val isLocked = isProMode && isUpgradeAvailable && !isPremium

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .background(
                                if (isSelected) Color.White else Color.Black,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { 
                                if (isLocked) {
                                    onShowPremiumDialog()
                                } else {
                                    onModeChange(mode)
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when(mode) {
                                    ThemeMode.LIGHT -> "LIGHT MODE"
                                    ThemeMode.DARK -> "DARK MODE"
                                    ThemeMode.CIRCADIAN -> "CIRCADIAN THEME"
                                    ThemeMode.TRANSPARENT -> "TRANSPARENT MODE"
                                },
                                color = if (isSelected) Color.Black else if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            if (isProMode && isUpgradeAvailable) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), 
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "PRO", 
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f), 
                                        fontSize = 7.sp, 
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Transparent Mode & Wallpapers",
                    "Circadian Theming (Day/Night Colors)",
                    "Tile Transparency Control",
                    "Modern List Layout",
                    "Premium Dashboard Experience"
                ).forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = DotzTheme.colors.background.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            feature,
                            color = DotzTheme.colors.background.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

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
                checkedThumbColor  = Color.Black,
                checkedTrackColor  = if (isLocked) Color.White.copy(alpha = 0.2f) else Color.White,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color(0xFF1A1A1A) // Match row background roughly
            )
        )
    }
}

