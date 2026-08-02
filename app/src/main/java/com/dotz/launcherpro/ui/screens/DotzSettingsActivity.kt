package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.R
import com.dotz.launcherpro.manager.PermissionManager
import com.dotz.launcherpro.ui.components.DotzAlertDialog
import com.dotz.launcherpro.ui.components.PremiumBadge
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import com.dotz.launcherpro.viewmodel.LauncherUiState
import com.dotz.launcherpro.viewmodel.ThemeMode
import kotlinx.coroutines.launch

data class SettingsActions(
    val onBack: () -> Unit,
    val onShowDots: (Boolean) -> Unit,
    val onShowCounts: (Boolean) -> Unit,
    val onNotificationFilterToggle: (Boolean) -> Unit,
    val onGrayscaleToggle: (Boolean) -> Unit,
    val onAutoGrayscaleToggle: (Boolean) -> Unit,
    val onVerticalScrollToggle: (Boolean) -> Unit,
    val onEnableExtraPageToggle: (Boolean) -> Unit,
    val onExtraTileCountChange: (Int) -> Unit,
    val onShowWeatherToggle: (Boolean) -> Unit,
    val onEnableTimelineToggle: (Boolean) -> Unit,
    val onHomeHeaderModeChange: (String) -> Unit,
    val onTransparencyChange: (Float) -> Unit,
    val onLayoutStyleChange: (String) -> Unit,
    val onWallpaperClick: () -> Unit,
    val onIconPackChange: (String?) -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onSetDefault: () -> Unit,
    val onAboutClick: () -> Unit,
    val onUpgradeClick: () -> Unit,
    val onAppSelectionClick: () -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onUseLiquidGlassToggle: (Boolean) -> Unit,
    val onShowMindfulUsageToggle: (Boolean) -> Unit,
    val onCreateProfile: (String) -> Unit,
    val onDeleteProfile: (String) -> Unit,
    val onSwitchProfile: (String) -> Unit,
    val onStartUltraFocus: (Int) -> Unit,
    val onBatchNotificationsToggle: (Boolean) -> Unit,
    val onNotificationBatchIntervalChange: (Int) -> Unit,
    val onDeliverBatch: () -> Unit,
    val onWeatherUnitChange: (String) -> Unit,
    val onEditModeToggle: (Boolean) -> Unit,
    val onFontChange: (String) -> Unit,
    val onBiometricPauseToggle: (Boolean) -> Unit,
    val onUpdateClick: () -> Unit,
    val onThemeIdChange: (String) -> Unit,
    val onCustomColorChange: (Int) -> Unit,
    val onClockStyleChange: (String) -> Unit
)

class DotzSettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure app draws behind cutouts for true edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 35) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        enableEdgeToEdge()
        
        setContent {
            val theme by viewModel.themeState.collectAsState()
            val focus by viewModel.focusState.collectAsState()
            
            val settingsForSettings = theme.settings.copy(showWallpaper = false, tileTransparency = 1.0f)
            DotzTheme(settings = settingsForSettings) {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            val json = viewModel.exportSettings()
                            contentResolver.openOutputStream(it)?.use { out ->
                                out.write(json.toByteArray())
                            }
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_exported))
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
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_imported))
                                } else {
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_import_failed))
                                }
                            }
                        }
                    }
                }

                DotzSettingsScreen(
                    theme = theme,
                    focus = focus,
                    snackbarHostState = snackbarHostState,
                    actions = SettingsActions(
                        onBack = { finish() },
                        onShowDots = viewModel::setShowNotificationDots,
                        onShowCounts = viewModel::setShowNumericalCounts,
                        onNotificationFilterToggle = viewModel::setNotificationFilterEnabled,
                        onGrayscaleToggle = viewModel::setGrayscaleMode,
                        onAutoGrayscaleToggle = viewModel::setAutoGrayscale,
                        onVerticalScrollToggle = viewModel::setVerticalScrolling,
                        onEnableExtraPageToggle = viewModel::setEnableExtraPage,
                        onExtraTileCountChange = viewModel::setExtraTileCount,
                        onShowWeatherToggle = viewModel::setShowWeatherInfo,
                        onEnableTimelineToggle = viewModel::setEnableTimeline,
                        onHomeHeaderModeChange = viewModel::setHomeHeaderMode,
                        onTransparencyChange = viewModel::setTileTransparency,
                        onLayoutStyleChange = viewModel::setLayoutStyle,
                        onWallpaperClick = viewModel::openWallpaperPicker,
                        onIconPackChange = viewModel::setIconPackPackage,
                        onExport = { exportLauncher.launch("dotz_backup.json") },
                        onImport = { importLauncher.launch("application/json") },
                        onSetDefault = viewModel::openDefaultLauncherSettings,
                        onThemeModeChange = viewModel::setThemeMode,
                        onUseLiquidGlassToggle = viewModel::setUseLiquidGlass,
                        onShowMindfulUsageToggle = viewModel::setShowMindfulUsage,
                        onAboutClick = {
                            startActivity(Intent(this, DotzAboutActivity::class.java))
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_up, R.anim.stay)
                        },
                        onUpgradeClick = {
                            startActivity(Intent(this, DotzUpgradeActivity::class.java))
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_up, R.anim.stay)
                        },
                        onAppSelectionClick = {
                            startActivity(Intent(this, AppSelectionListActivity::class.java))
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_up, R.anim.stay)
                        },
                        onCreateProfile = viewModel::createProfile,
                        onDeleteProfile = viewModel::deleteProfile,
                        onSwitchProfile = viewModel::switchProfile,
                        onStartUltraFocus = viewModel::startUltraFocusSession,
                        onBatchNotificationsToggle = viewModel::setBatchNotifications,
                        onNotificationBatchIntervalChange = viewModel::setNotificationBatchInterval,
                        onDeliverBatch = viewModel::deliverBatch,
                        onWeatherUnitChange = viewModel::setWeatherUnit,
                        onEditModeToggle = viewModel::setEditModeEnabled,
                        onFontChange = viewModel::setFontId,
                        onBiometricPauseToggle = viewModel::setUseBiometricPause,
                        onUpdateClick = { viewModel.startUpdateFlow(this@DotzSettingsActivity) },
                        onThemeIdChange = viewModel::setThemeId,
                        onCustomColorChange = viewModel::setCustomAccentColor,
                        onClockStyleChange = viewModel::setClockStyle
                    )
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_down)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DotzSettingsScreen(
    theme: com.dotz.launcherpro.viewmodel.ThemeState,
    focus: com.dotz.launcherpro.viewmodel.FocusState,
    snackbarHostState: SnackbarHostState,
    actions: SettingsActions
) {
    val settings = theme.settings
    val isUpgradeAvailable = theme.isUpgradeAvailable
    val isDefaultLauncher = theme.isDefaultLauncher
    val isUpdateAvailable = theme.isUpdateAvailable
    val isLiteVersion = theme.isLiteVersion
    val hasUsageStatsPermission = focus.hasUsageStatsPermission
    val iconPacks = theme.installedIconPacks
    val currentThemeMode = theme.currentThemeMode
    val homeHeaderMode = settings.homeHeaderMode

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
            actions.onShowWeatherToggle(true)
        } else {
            Toast.makeText(context, context.getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    if (showLocationDisclosure) {
        DotzAlertDialog(
            onDismissRequest = { showLocationDisclosure = false },
            title = stringResource(R.string.dialog_location_disclosure_title),
            content = { 
                Text(
                    stringResource(R.string.dialog_location_disclosure_message),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
            confirmButtonText = stringResource(R.string.btn_grant),
            onConfirm = { 
                showLocationDisclosure = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            dismissButtonText = stringResource(R.string.btn_cancel),
            onDismiss = { showLocationDisclosure = false }
        )
    }

    if (showUsageStatsDisclosure) {
        DotzAlertDialog(
            onDismissRequest = { showUsageStatsDisclosure = false },
            title = stringResource(R.string.dialog_usage_disclosure_title),
            content = { 
                Text(
                    stringResource(R.string.dialog_usage_disclosure_message), 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
            confirmButtonText = stringResource(R.string.btn_enable),
            onConfirm = { 
                showUsageStatsDisclosure = false
                PermissionManager.openUsageAccessSettings(context)
            },
            dismissButtonText = stringResource(R.string.btn_not_now),
            onDismiss = { showUsageStatsDisclosure = false }
        )
    }

    if (showUltraFocusDurationDialog) {
        DotzAlertDialog(
            onDismissRequest = { showUltraFocusDurationDialog = false },
            title = stringResource(R.string.dialog_ultra_focus_duration_title),
            content = {
                Column {
                    Text(stringResource(R.string.dialog_ultra_focus_duration_message), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        15 to stringResource(R.string.duration_15_mins),
                        30 to stringResource(R.string.duration_30_mins),
                        60 to stringResource(R.string.duration_1_hour),
                        120 to stringResource(R.string.duration_2_hours)
                    ).forEach { (mins, label) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { 
                                actions.onStartUltraFocus(mins)
                                showUltraFocusDurationDialog = false
                            },
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButtonText = stringResource(R.string.btn_cancel),
            onConfirm = { showUltraFocusDurationDialog = false }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_header),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Premium Promo ---
            if (!settings.isPremium && isUpgradeAvailable) {
                item(key = "premium_promo") {
                    PremiumPromotionCard(onClick = actions.onUpgradeClick)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // --- Customization Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_group_home)) }
            item {
                SettingsPreviewCard {
                    val isList = settings.layoutStyle == "list"
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Minimalist Header Mock
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClockRenderer(
                                style = settings.clockStyle,
                                time = "08:18", // Fixed time for preview
                                date = "SAT, 01 AUG"
                            )
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                        }

                        if (isList) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(2) {
                                    MiniAppTile(
                                        label = "App",
                                        icon = if (it == 0) Icons.Default.Phone else Icons.AutoMirrored.Filled.Message,
                                        transparency = settings.tileTransparency,
                                        isList = true
                                    )
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(3) {
                                    MiniAppTile(
                                        label = "App",
                                        icon = when(it) {
                                            0 -> Icons.Default.Phone
                                            1 -> Icons.AutoMirrored.Filled.Message
                                            else -> Icons.Default.CameraAlt
                                        },
                                        transparency = settings.tileTransparency,
                                        isList = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_app_selection),
                    icon = Icons.Default.Apps,
                    onClick = actions.onAppSelectionClick
                )
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_wallpaper),
                    icon = Icons.Default.Wallpaper,
                    isPremium = isUpgradeAvailable,
                    isLocked = !settings.isPremium && isUpgradeAvailable,
                    onClick = {
                        if (!isUpgradeAvailable || settings.isPremium) actions.onWallpaperClick()
                        else showPremiumDialog = true
                    }
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_vertical_scroll),
                    icon = Icons.Default.SwapVert,
                    checked = settings.verticalScrolling,
                    onToggle = actions.onVerticalScrollToggle
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_extra_tiles),
                    icon = Icons.Default.AddBox,
                    checked = settings.enableExtraPage,
                    onToggle = actions.onEnableExtraPageToggle
                )
            }
            if (settings.enableExtraPage) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.settings_label_extra_tiles_count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("${settings.extraTileCount}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Slider(
                            value = settings.extraTileCount.toFloat(),
                            onValueChange = { actions.onExtraTileCountChange(it.toInt()) },
                            valueRange = 1f..6f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            // --- Appearance Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_group_appearance)) }
            item {
                ModernSettingsActionRow(
                    label = "Clock Style",
                    icon = Icons.Default.AccessTime,
                    subtitle = settings.clockStyle.replaceFirstChar { it.uppercase() },
                    onClick = {}
                )
            }
            item {
                TonalSegmentedControl(
                    options = listOf(
                        "classic" to "Classic",
                        "modern" to "Modern",
                        "minimalist" to "Minimal",
                        "analog" to "Analog",
                        "textual" to "Words"
                    ),
                    proOptions = setOf("modern", "minimalist", "analog", "textual"),
                    selected = settings.clockStyle,
                    onSelect = { 
                        if (it == "classic" || settings.isPremium) actions.onClockStyleChange(it)
                        else showPremiumDialog = true
                    }
                )
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_theme_mode),
                    icon = Icons.Default.Palette,
                    subtitle = currentThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = {}
                )
            }
            item {
                ThemeModeSelectionRow(
                    currentMode = currentThemeMode,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    isLiteVersion = isLiteVersion,
                    onModeChange = actions.onThemeModeChange,
                    onShowPremiumDialog = { showPremiumDialog = true }
                )
            }
            if (currentThemeMode == ThemeMode.CUSTOM) {
                item {
                    ColorPaletteRow(
                        selectedColor = settings.customAccentColor,
                        onColorSelect = actions.onCustomColorChange
                    )
                }
            }
            item {
                TransparencySlider(settings.tileTransparency, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) actions.onTransparencyChange(it) else showPremiumDialog = true
                }
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_liquid_glass),
                    icon = Icons.Default.BlurOn,
                    checked = settings.useLiquidGlass,
                    onToggle = { 
                        if (settings.isPremium || !isUpgradeAvailable) actions.onUseLiquidGlassToggle(it)
                        else showPremiumDialog = true
                    },
                    subtitle = stringResource(R.string.settings_subtitle_liquid_glass)
                )
            }
            item {
                TileLayoutSelection(settings.layoutStyle, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) actions.onLayoutStyleChange(it) else showPremiumDialog = true
                }
            }
            item {
                FontSelectionRow(
                    currentFontId = settings.fontId,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    onFontChange = actions.onFontChange,
                    onShowPremiumDialog = { showPremiumDialog = true }
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = "Enable Tile Editing",
                    icon = Icons.Default.Edit,
                    checked = settings.editModeEnabled,
                    onToggle = actions.onEditModeToggle,
                    subtitle = "Long press to move tiles"
                )
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_icon_pack),
                    icon = Icons.Default.Category,
                    subtitle = settings.iconPackPackage ?: stringResource(R.string.label_default),
                    onClick = { showIconPackDialog = true }
                )
            }

            // --- Mindfulness Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_group_mindfulness)) }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_ultra_focus),
                    icon = Icons.Default.Psychology,
                    subtitle = stringResource(R.string.settings_subtitle_ultra_focus),
                    onClick = { showUltraFocusDurationDialog = true }
                )
            }
            item {
                ModernSettingsActionRow(
                    label = "Select Ultra Focus Apps",
                    icon = Icons.Default.AppRegistration,
                    subtitle = "Pick up to 7 apps for focus sessions",
                    onClick = {
                        context.startActivity(Intent(context, UltraFocusAppSelectionActivity::class.java))
                    }
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_timeline),
                    icon = Icons.Default.Timeline,
                    checked = settings.enableTimeline,
                    onToggle = actions.onEnableTimelineToggle,
                    subtitle = stringResource(R.string.settings_subtitle_timeline),
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_mindful_usage),
                    icon = Icons.Default.Psychology,
                    checked = settings.showMindfulUsage,
                    onToggle = { enabled ->
                        if (enabled && !hasUsageStatsPermission) {
                            showUsageStatsDisclosure = true
                        } else {
                            actions.onShowMindfulUsageToggle(enabled)
                        }
                    },
                    subtitle = stringResource(R.string.settings_subtitle_mindful_usage)
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = "Biometric Intention Lock",
                    icon = Icons.Default.Fingerprint,
                    checked = settings.useBiometricPause,
                    isPremium = isUpgradeAvailable,
                    isLocked = !settings.isPremium && isUpgradeAvailable,
                    onToggle = { 
                        if (settings.isPremium || !isUpgradeAvailable) actions.onBiometricPauseToggle(it)
                        else showPremiumDialog = true
                    },
                    subtitle = "Require biometric to open distracting apps"
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.settings_label_header_mode), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    TonalSegmentedControl(
                        options = listOf(
                            "toggles" to stringResource(R.string.header_mode_toggles),
                            "music" to stringResource(R.string.header_mode_music),
                            "stats" to stringResource(R.string.header_mode_focus),
                            "weather" to stringResource(R.string.header_mode_weather)
                        ),
                        selected = homeHeaderMode,
                        onSelect = actions.onHomeHeaderModeChange
                    )
                }
            }

            // --- Notifications Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_group_notifications)) }
            item {
                ModernSettingsToggleRow(
                    label = "Batch Notifications",
                    icon = Icons.Default.Inventory,
                    checked = settings.batchNotifications,
                    onToggle = actions.onBatchNotificationsToggle,
                    subtitle = "Deliver in scheduled intervals"
                )
            }
            if (settings.batchNotifications) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Delivery Interval", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        TonalSegmentedControl(
                            options = listOf(
                                "1" to "1h",
                                "2" to "2h",
                                "4" to "4h",
                                "8" to "8h"
                            ),
                            selected = settings.notificationBatchInterval.toString(),
                            onSelect = { actions.onNotificationBatchIntervalChange(it.toInt()) }
                        )
                    }
                }
                item {
                    ModernSettingsActionRow(
                        label = "Deliver Batch Now",
                        icon = Icons.Default.MoveToInbox,
                        onClick = actions.onDeliverBatch
                    )
                }
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_notification_dots),
                    icon = Icons.Default.Circle,
                    checked = settings.showNotificationDots,
                    onToggle = actions.onShowDots
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_numerical_counts),
                    icon = Icons.Default.Pin,
                    checked = settings.showNumericalCounts,
                    onToggle = actions.onShowCounts
                )
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_filter_distractions),
                    icon = Icons.Default.FilterAlt,
                    checked = settings.notificationFilterEnabled,
                    onToggle = actions.onNotificationFilterToggle,
                    subtitle = stringResource(R.string.settings_subtitle_filter_distractions)
                )
            }

            // --- System & Backup Section ---
            item { SettingsSectionHeader("System & Backup") }
            if (!isDefaultLauncher) {
                item {
                    ModernSettingsActionRow(
                        label = stringResource(R.string.settings_item_set_default),
                        icon = Icons.Default.Home,
                        onClick = actions.onSetDefault
                    )
                }
            }
            item {
                ModernSettingsToggleRow(
                    label = stringResource(R.string.settings_item_weather), 
                    icon = Icons.Default.Cloud, 
                    checked = settings.showWeatherInfo, 
                    onToggle = { enabled ->
                        if (enabled) {
                            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                actions.onShowWeatherToggle(true)
                            } else {
                                showLocationDisclosure = true
                            }
                        } else {
                            actions.onShowWeatherToggle(false)
                        }
                    }
                )
            }
            if (settings.showWeatherInfo) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Temperature Unit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        TonalSegmentedControl(
                            options = listOf(
                                "metric" to "Celsius",
                                "imperial" to "Fahrenheit"
                            ),
                            selected = settings.weatherUnit,
                            onSelect = actions.onWeatherUnitChange
                        )
                    }
                }
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_export),
                    icon = Icons.Default.Download,
                    onClick = actions.onExport
                )
            }
            item {
                ModernSettingsActionRow(
                    label = stringResource(R.string.settings_item_import),
                    icon = Icons.Default.Upload,
                    onClick = actions.onImport
                )
            }

            // --- Profiles Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_group_profiles)) }
            item {
                ProfileManagementCard(
                    activeId = settings.activeProfileId,
                    profiles = settings.profiles,
                    isPremium = settings.isPremium,
                    isUpgradeAvailable = isUpgradeAvailable,
                    onSwitch = actions.onSwitchProfile,
                    onDelete = actions.onDeleteProfile,
                    onAddClick = { 
                        if (!isUpgradeAvailable || settings.isPremium) showCreateProfileDialog = true
                        else showPremiumDialog = true
                    }
                )
            }

            // --- Info Section ---
            item { SettingsSectionHeader(stringResource(R.string.settings_label_about)) }
            item(key = "card_about") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    onClick = {
                        if (isUpdateAvailable) {
                            actions.onUpdateClick()
                        } else {
                            actions.onAboutClick()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isUpdateAvailable) Icons.Default.Update else Icons.Default.Info,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = if (isUpdateAvailable) stringResource(R.string.settings_item_update_available) else stringResource(R.string.settings_item_about_dotz),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    if (showIconPackDialog) {
        IconPackDialog(
            currentIconPack = settings.iconPackPackage,
            iconPacks = iconPacks,
            onSelect = { pkg -> actions.onIconPackChange(pkg); showIconPackDialog = false },
            onDismiss = { showIconPackDialog = false },
        )
    }

    if (showPremiumDialog) {
        DotzAlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = stringResource(R.string.dialog_pro_feature_title),
            content = { Text(stringResource(R.string.dialog_pro_feature_message), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButtonText = stringResource(R.string.btn_go_pro),
            onConfirm = { showPremiumDialog = false; actions.onUpgradeClick() },
            dismissButtonText = stringResource(R.string.btn_not_now),
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showCreateProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        DotzAlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = stringResource(R.string.dialog_new_profile_title),
            content = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    placeholder = { Text(stringResource(R.string.dialog_new_profile_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButtonText = stringResource(R.string.btn_create),
            onConfirm = { if (profileName.isNotBlank()) { actions.onCreateProfile(profileName); showCreateProfileDialog = false } },
            dismissButtonText = stringResource(R.string.btn_cancel),
            onDismiss = { showCreateProfileDialog = false }
        )
    }
}

@Composable
private fun FontSelectionRow(currentFontId: String, isPremium: Boolean, isUpgradeAvailable: Boolean, onFontChange: (String) -> Unit, onShowPremiumDialog: () -> Unit) {
    val fonts = listOf(
        "default" to "Default",
        "serif" to "Serif",
        "monospace" to "Mono",
        "sans-serif" to "Sans"
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FontDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Typography", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (!isPremium && isUpgradeAvailable) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Spacer(Modifier.height(12.dp))
        TonalSegmentedControl(
            options = fonts,
            selected = currentFontId,
            proOptions = if (!isPremium && isUpgradeAvailable) fonts.map { it.first }.filter { it != "default" }.toSet() else emptySet(),
            onSelect = { 
                if (it == "default" || isPremium || !isUpgradeAvailable) onFontChange(it)
                else onShowPremiumDialog()
            }
        )
    }
}
