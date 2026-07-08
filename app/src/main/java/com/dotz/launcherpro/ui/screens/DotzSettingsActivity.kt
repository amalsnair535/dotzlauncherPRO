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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.components.DotzAlertDialog
import com.dotz.launcherpro.ui.components.PremiumBadge
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import com.dotz.launcherpro.viewmodel.ThemeMode
import kotlinx.coroutines.launch

class DotzSettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        setContent {
            val uiState by viewModel.uiState.collectAsState()
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

                DotzSettingsScreen(
                    settings          = uiState.settings,
                    isUpgradeAvailable = uiState.isUpgradeAvailable,
                    onBack            = { finish() },
                    onShowDots        = viewModel::setShowNotificationDots,
                    onShowCounts      = viewModel::setShowNumericalCounts,
                    onNotificationFilterToggle = viewModel::setNotificationFilterEnabled,
                    onGrayscaleToggle = viewModel::setGrayscaleMode,
                    onAutoGrayscaleToggle = viewModel::setAutoGrayscale,
                    onVerticalScrollToggle = viewModel::setVerticalScrolling,
                    onEnableExtraPageToggle = viewModel::setEnableExtraPage,
                    onExtraTileCountChange = viewModel::setExtraTileCount,
                    onShowWeatherToggle = { enabled ->
                        viewModel.setShowWeatherInfo(enabled)
                    },
                    onEnableFastlaneToggle = viewModel::setEnableFastlane,
                    homeHeaderMode = uiState.settings.homeHeaderMode,
                    onHomeHeaderModeChange = viewModel::setHomeHeaderMode,
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
                    onUseLiquidGlassToggle = viewModel::setUseLiquidGlass,
                    onShowMindfulUsageToggle = viewModel::setShowMindfulUsage,
                    hasUsageStatsPermission = uiState.hasUsageStatsPermission,
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
                    onStartUltraFocus = viewModel::startUltraFocusSession
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
    onAutoGrayscaleToggle: (Boolean) -> Unit,
    onVerticalScrollToggle: (Boolean) -> Unit,
    onEnableExtraPageToggle: (Boolean) -> Unit,
    onExtraTileCountChange: (Int) -> Unit,
    onShowWeatherToggle: (Boolean) -> Unit,
    onEnableFastlaneToggle: (Boolean) -> Unit,
    homeHeaderMode: String,
    onHomeHeaderModeChange: (String) -> Unit,
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
    onUseLiquidGlassToggle: (Boolean) -> Unit,
    onShowMindfulUsageToggle: (Boolean) -> Unit,
    hasUsageStatsPermission: Boolean,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onStartUltraFocus: (Int) -> Unit,
) {
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showLocationDisclosure by remember { mutableStateOf(false) }
    var showUltraFocusDurationDialog by remember { mutableStateOf(false) }
    var showUsageStatsDisclosure by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onShowWeatherToggle(true)
        } else {
            Toast.makeText(context, "Location permission required for weather", Toast.LENGTH_SHORT).show()
        }
    }

    if (showLocationDisclosure) {
        // ... (existing dialog)
    }

    if (showLocationDisclosure) {
        DotzAlertDialog(
            onDismissRequest = { showLocationDisclosure = false },
            title = "Location Disclosure",
            content = { 
                Text(
                    "Dotz Launcher requests location data to provide you with real-time weather conditions for your area. " +
                    "This data is used only when the weather feature is active and is not stored or shared for any other purpose.",
                    color = Color.White.copy(alpha = 0.7f)
                ) 
            },
            confirmButtonText = "GRANT",
            onConfirm = { 
                showLocationDisclosure = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            dismissButtonText = "CANCEL",
            onDismiss = { showLocationDisclosure = false }
        )
    }

    if (showUsageStatsDisclosure) {
        DotzAlertDialog(
            onDismissRequest = { showUsageStatsDisclosure = false },
            title = "Mindful Usage Disclosure",
            content = { 
                Text(
                    "Dotz Launcher uses anonymized usage statistics to track your screen time and device unlocks. " +
                    "This information is processed only on your device to calculate your Focus Score and enable app usage limits. " +
                    "No usage data is ever collected or transmitted.", 
                    color = Color.White.copy(alpha = 0.7f)
                ) 
            },
            confirmButtonText = "ENABLE",
            onConfirm = { 
                showUsageStatsDisclosure = false
                val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                try { context.startActivity(intent) } catch (_: Exception) { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
            },
            dismissButtonText = "NOT NOW",
            onDismiss = { showUsageStatsDisclosure = false }
        )
    }

    if (showUltraFocusDurationDialog) {
        DotzAlertDialog(
            onDismissRequest = { showUltraFocusDurationDialog = false },
            title = "Ultra Focus Duration",
            content = {
                Column {
                    Text("Select how long you want to stay in Ultra Focus mode. All distractions will be hidden.", color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    listOf(15 to "15 Minutes", 30 to "30 Minutes", 60 to "1 Hour", 120 to "2 Hours").forEach { (mins, label) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { 
                                onStartUltraFocus(mins)
                                showUltraFocusDurationDialog = false
                            },
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Text(label, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButtonText = "CANCEL",
            onConfirm = { showUltraFocusDurationDialog = false }
        )
    }

    val currentThemeMode = remember(settings.isLightMode, settings.useCircadianTheming, settings.showWallpaper) {
        when {
            settings.showWallpaper -> ThemeMode.TRANSPARENT
            settings.useCircadianTheming -> ThemeMode.CIRCADIAN
            settings.isLightMode -> ThemeMode.LIGHT
            else -> ThemeMode.DARK
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Customize your minimalist experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Black,
                    scrolledContainerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }
            
            // --- Premium Section ---
            if (!settings.isPremium && isUpgradeAvailable) {
                item { PremiumPromotionCard(onClick = onUpgradeClick) }
            }

            // --- Home Screen Section ---
            item { SettingsGroup(title = "Home Screen") {
                SettingsActionRow(label = "App Selection", icon = Icons.Default.Apps, onClick = onAppSelectionClick)
                Divider()
                SettingsActionRow(
                    label = "Change Wallpaper",
                    icon = Icons.Default.Wallpaper,
                    isPremium = isUpgradeAvailable,
                    isLocked = !settings.isPremium && isUpgradeAvailable,
                    onClick = {
                        if (!isUpgradeAvailable || settings.isPremium) onWallpaperClick()
                        else showPremiumDialog = true
                    }
                )
                Divider()
                SettingsToggleRow(
                    label = "Vertical Scrolling",
                    icon = Icons.Default.SwapVert,
                    checked = settings.verticalScrolling,
                    onToggle = onVerticalScrollToggle
                )
                Divider()
                SettingsToggleRow(
                    label = "Enable Extra Tiles (Page 3)",
                    icon = Icons.Default.AddBox,
                    checked = settings.enableExtraPage,
                    onToggle = onEnableExtraPageToggle
                )
                if (settings.enableExtraPage) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Extra Tiles Count", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                            Text("${settings.extraTileCount}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                        Slider(
                            value = settings.extraTileCount.toFloat(),
                            onValueChange = { onExtraTileCountChange(it.toInt()) },
                            valueRange = 1f..6f,
                            steps = 4
                        )
                    }
                }
            }}

            // --- Theme Section ---
            item { SettingsGroup(title = "Appearance") {
                SettingsActionRow(
                    label = "Theme Mode",
                    icon = Icons.Default.Palette,
                    subtitle = currentThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = {}
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ThemeModeSelectionRow(
                        currentMode = currentThemeMode,
                        isPremium = settings.isPremium,
                        isUpgradeAvailable = isUpgradeAvailable,
                        isLiteVersion = isLiteVersion,
                        onModeChange = onThemeModeChange,
                        onShowPremiumDialog = { showPremiumDialog = true }
                    )
                }
                Divider()
                SettingsToggleRow(label = "Grayscale Mode", icon = Icons.Default.Contrast, checked = settings.grayscaleMode, onToggle = onGrayscaleToggle)
                Divider()
                SettingsToggleRow(
                    label = "Grayscale Auto-Schedule",
                    icon = Icons.Default.Bedtime,
                    checked = settings.autoGrayscale,
                    onToggle = onAutoGrayscaleToggle,
                    subtitle = "Turns on after 10 PM"
                )
                Divider()
                TransparencySlider(settings.tileTransparency, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) onTransparencyChange(it) else showPremiumDialog = true
                }
                Divider()
                SettingsToggleRow(
                    label = "Liquid Glass Effect",
                    icon = Icons.Default.BlurOn,
                    checked = settings.useLiquidGlass,
                    onToggle = { 
                        if (settings.isPremium || !isUpgradeAvailable) onUseLiquidGlassToggle(it)
                        else showPremiumDialog = true
                    },
                    subtitle = "Animated glassmorphism for all themes"
                )
                Divider()
                TileLayoutSelection(settings.layoutStyle, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) onLayoutStyleChange(it) else showPremiumDialog = true
                }
                Divider()
                SettingsActionRow(
                    label = "Start Ultra Focus Session",
                    icon = Icons.Default.Psychology,
                    subtitle = "Minimal essentials for a set time",
                    onClick = { showUltraFocusDurationDialog = true }
                )
                Divider()
                SettingsToggleRow(
                    label = "Fastlane Timeline",
                    icon = Icons.Default.Timeline,
                    checked = settings.enableFastlane,
                    onToggle = onEnableFastlaneToggle,
                    subtitle = "Chronological feed of your digital life",
                )
                Divider()
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Header Mode", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    TonalSegmentedControl(
                        options = listOf("toggles" to "Toggles", "music" to "Music", "stats" to "Focus"),
                        selected = homeHeaderMode,
                        onSelect = onHomeHeaderModeChange
                    )
                }
            }}

            // --- Notifications ---
            item { SettingsGroup(title = "Notifications") {
                SettingsToggleRow(label = "Notification Dots", icon = Icons.Default.Circle, checked = settings.showNotificationDots, onToggle = onShowDots)
                Divider()
                SettingsToggleRow(label = "Numerical Counts", icon = Icons.Default.Pin, checked = settings.showNumericalCounts, onToggle = onShowCounts)
                Divider()
                SettingsToggleRow(
                    label = "Filter Distractions",
                    icon = Icons.Default.FilterAlt,
                    checked = settings.notificationFilterEnabled,
                    onToggle = onNotificationFilterToggle,
                    subtitle = "Hides dots for social apps"
                )
            }}

            // --- Profiles ---
            item { SettingsGroup(title = "Profiles") {
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
            }}

            // --- Mindfulness ---
            item { SettingsGroup(title = "Mindfulness") {
                SettingsToggleRow(
                    label = "Mindful Usage",
                    icon = Icons.Default.Psychology,
                    checked = settings.showMindfulUsage,
                    onToggle = { enabled ->
                        if (enabled && !hasUsageStatsPermission) {
                            showUsageStatsDisclosure = true
                        } else {
                            onShowMindfulUsageToggle(enabled)
                        }
                    },
                    subtitle = "Track app time and launch limits"
                )
            }}

            // --- System ---
            item { SettingsGroup(title = "System") {
                if (!isDefaultLauncher) {
                    SettingsActionRow(label = "Set as Default", icon = Icons.Default.Home, onClick = onSetDefault)
                    Divider()
                }
                SettingsToggleRow(
                    label = "Weather Info", 
                    icon = Icons.Default.Cloud, 
                    checked = settings.showWeatherInfo, 
                    onToggle = { enabled ->
                        if (enabled) {
                            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                onShowWeatherToggle(true)
                            } else {
                                showLocationDisclosure = true
                            }
                        } else {
                            onShowWeatherToggle(false)
                        }
                    }
                )
                Divider()
                SettingsActionRow(label = "Icon Pack", icon = Icons.Default.Category, subtitle = settings.iconPackPackage ?: "Default", onClick = { showIconPackDialog = true })
                Divider()
                SettingsActionRow(label = "Export Settings", icon = Icons.Default.Download, onClick = onExport)
                Divider()
                SettingsActionRow(label = "Import Settings", icon = Icons.Default.Upload, onClick = onImport)
            }}

            // --- Info ---
            item {
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    SettingsActionRow(
                        label = if (isUpdateAvailable) "Update Available!" else "About Dotz",
                        icon = Icons.Default.Info,
                        onClick = onAboutClick
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showIconPackDialog) {
        IconPackDialog(
            currentIconPack = settings.iconPackPackage,
            iconPacks = iconPacks,
            onSelect = { pkg -> onIconPackChange(pkg); showIconPackDialog = false },
            onDismiss = { showIconPackDialog = false },
        )
    }

    if (showPremiumDialog) {
        DotzAlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = "PRO Feature",
            content = { Text("Transparency, Wallpapers, and Layouts are exclusive to Dotz PRO.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButtonText = "GO PRO",
            onConfirm = { showPremiumDialog = false; onUpgradeClick() },
            dismissButtonText = "NOT NOW",
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showCreateProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        DotzAlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = "New Profile",
            content = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    placeholder = { Text("Work, Home, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButtonText = "CREATE",
            onConfirm = { if (profileName.isNotBlank()) { onCreateProfile(profileName); showCreateProfileDialog = false } },
            dismissButtonText = "CANCEL",
            onDismiss = { showCreateProfileDialog = false }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f)
                )
            }
            
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    subtitle: String? = null,
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onToggle(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = if (isLocked) 0.2f else 0.8f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (isPremium && isLocked) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = !isLocked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    icon: ImageVector,
    subtitle: String? = null,
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = if (isLocked) 0.2f else 0.8f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (isPremium && isLocked) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Icon(
            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun TonalSegmentedControl(
    options: List<Pair<String, String>>, 
    selected: String, 
    proOptions: Set<String> = emptySet(),
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(4.dp)) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            val isPro = proOptions.contains(mode)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label.uppercase(), 
                        style = MaterialTheme.typography.labelLarge, 
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f), 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isPro) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.Black.copy(alpha = 0.1f) 
                                    else Color.White.copy(alpha = 0.1f), 
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO", 
                                fontSize = 6.sp, 
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelectionRow(currentMode: ThemeMode, isPremium: Boolean, isUpgradeAvailable: Boolean, isLiteVersion: Boolean, onModeChange: (ThemeMode) -> Unit, onShowPremiumDialog: () -> Unit) {
    val modes = if (isLiteVersion) listOf(ThemeMode.LIGHT, ThemeMode.DARK) else ThemeMode.entries
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { mode ->
            val isSelected = currentMode == mode
            val isPro = mode == ThemeMode.CIRCADIAN || mode == ThemeMode.TRANSPARENT
            val locked = isPro && isUpgradeAvailable && !isPremium

            Surface(
                modifier = Modifier.weight(1f).height(64.dp).clickable { if (locked) onShowPremiumDialog() else onModeChange(mode) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mode.name, color = if (isSelected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        if (isPro && isUpgradeAvailable) {
                            Spacer(Modifier.height(4.dp))
                            PremiumBadge()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransparencySlider(value: Float, isPremium: Boolean, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Opacity, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Tile Transparency", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.5f))
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0.1f..1.0f)
    }
}

@Composable
private fun TileLayoutSelection(current: String, isPremium: Boolean, onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tile Layout", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            if (!isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("classic" to "GRID", "list" to "LIST").forEach { (style, label) ->
                val selected = current == style
                Surface(modifier = Modifier.weight(1f).height(40.dp).clickable { onChange(style) }, shape = RoundedCornerShape(8.dp), color = if (selected) Color.White else Color.White.copy(alpha = 0.05f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileManagementCard(activeId: String, profiles: List<com.dotz.launcherpro.data.LauncherProfile>, isPremium: Boolean, isUpgradeAvailable: Boolean, onSwitch: (String) -> Unit, onDelete: (String) -> Unit, onAddClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        ProfileItem("Default", activeId == "default", false) { onSwitch("default") }
        profiles.filter { it.id != "default" }.forEach { profile ->
            ProfileItem(profile.name, activeId == profile.id, isUpgradeAvailable && !isPremium) {
                if (!isUpgradeAvailable || isPremium) onSwitch(profile.id)
            }
        }
        TextButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Profile")
            if (isUpgradeAvailable && !isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
    }
}

@Composable
private fun ProfileItem(name: String, active: Boolean, locked: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (active) Color.White.copy(alpha = 0.1f) else Color.Transparent).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = active, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Color.White))
            Spacer(Modifier.width(8.dp))
            Text(name, color = if (locked) Color.White.copy(alpha = 0.3f) else Color.White)
        }
        if (locked) Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
private fun PremiumPromotionCard(onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(28.dp), color = Color.White) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Get Dotz PRO", style = MaterialTheme.typography.headlineSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                Text("Unlock wallpapers, profiles, and more.", style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun IconPackDialog(currentIconPack: String?, iconPacks: List<Pair<String, String>>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Select Icon Pack",
        content = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text("Default", color = if (currentIconPack == null) Color.White else Color.White.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
                items(iconPacks.size) { index ->
                    val (pkg, name) = iconPacks[index]
                    Text(name, color = if (currentIconPack == pkg) Color.White else Color.White.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(pkg) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButtonText = "CANCEL",
        onConfirm = onDismiss
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
}
