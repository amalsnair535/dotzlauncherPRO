package com.dotz.launcherpro.ui.screens

import android.content.Intent
import android.os.Bundle
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
    val onDeliverBatch: () -> Unit
)

class DotzSettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val settingsForSettings = uiState.settings.copy(showWallpaper = false, tileTransparency = 1.0f)
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
                    uiState = uiState,
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
                        },
                        onUpgradeClick = {
                            startActivity(Intent(this, DotzUpgradeActivity::class.java))
                        },
                        onAppSelectionClick = {
                            startActivity(Intent(this, AppSelectionListActivity::class.java))
                        },
                        onCreateProfile = viewModel::createProfile,
                        onDeleteProfile = viewModel::deleteProfile,
                        onSwitchProfile = viewModel::switchProfile,
                onStartUltraFocus = viewModel::startUltraFocusSession,
                onBatchNotificationsToggle = viewModel::setBatchNotifications,
                onDeliverBatch = viewModel::deliverBatch
            )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DotzSettingsScreen(
    uiState: LauncherUiState,
    snackbarHostState: SnackbarHostState,
    actions: SettingsActions
) {
    val settings = uiState.settings
    val isUpgradeAvailable = uiState.isUpgradeAvailable
    val isDefaultLauncher = uiState.isDefaultLauncher
    val isUpdateAvailable = uiState.isUpdateAvailable
    val isLiteVersion = uiState.isLiteVersion
    val hasUsageStatsPermission = uiState.hasUsageStatsPermission
    val iconPacks = uiState.installedIconPacks
    val currentThemeMode = uiState.currentThemeMode
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "spacer_top") { Spacer(Modifier.height(16.dp)) }
            
            // --- Premium Section ---
            if (!settings.isPremium && isUpgradeAvailable) {
                item(key = "premium_promo") { PremiumPromotionCard(onClick = actions.onUpgradeClick) }
            }

            // --- Home Screen Section ---
            item(key = "group_home") { SettingsGroup(title = stringResource(R.string.settings_group_home)) {
                SettingsActionRow(label = stringResource(R.string.settings_item_app_selection), icon = Icons.Default.Apps, onClick = actions.onAppSelectionClick)
                Divider()
                SettingsActionRow(
                    label = stringResource(R.string.settings_item_wallpaper),
                    icon = Icons.Default.Wallpaper,
                    isPremium = isUpgradeAvailable,
                    isLocked = !settings.isPremium && isUpgradeAvailable,
                    onClick = {
                        if (!isUpgradeAvailable || settings.isPremium) actions.onWallpaperClick()
                        else showPremiumDialog = true
                    }
                )
                Divider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_item_vertical_scroll),
                    icon = Icons.Default.SwapVert,
                    checked = settings.verticalScrolling,
                    onToggle = actions.onVerticalScrollToggle
                )
                Divider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_item_extra_tiles),
                    icon = Icons.Default.AddBox,
                    checked = settings.enableExtraPage,
                    onToggle = actions.onEnableExtraPageToggle
                )
                if (settings.enableExtraPage) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                thumbColor = MaterialTheme.colorScheme.onSurface,
                                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }}

            // --- Theme Section ---
            item(key = "group_appearance") { SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
                SettingsActionRow(
                    label = stringResource(R.string.settings_item_theme_mode),
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
                        onModeChange = actions.onThemeModeChange,
                        onShowPremiumDialog = { showPremiumDialog = true }
                    )
                }
                Divider()
                TransparencySlider(settings.tileTransparency, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) actions.onTransparencyChange(it) else showPremiumDialog = true
                }
                Divider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_item_liquid_glass),
                    icon = Icons.Default.BlurOn,
                    checked = settings.useLiquidGlass,
                    onToggle = { 
                        if (settings.isPremium || !isUpgradeAvailable) actions.onUseLiquidGlassToggle(it)
                        else showPremiumDialog = true
                    },
                    subtitle = stringResource(R.string.settings_subtitle_liquid_glass)
                )
                Divider()
                TileLayoutSelection(settings.layoutStyle, settings.isPremium || !isUpgradeAvailable) {
                    if (settings.isPremium || !isUpgradeAvailable) actions.onLayoutStyleChange(it) else showPremiumDialog = true
                }
                Divider()
                SettingsActionRow(
                    label = stringResource(R.string.settings_item_ultra_focus),
                    icon = Icons.Default.Psychology,
                    subtitle = stringResource(R.string.settings_subtitle_ultra_focus),
                    onClick = { showUltraFocusDurationDialog = true }
                )
                Divider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_item_timeline),
                    icon = Icons.Default.Timeline,
                    checked = settings.enableTimeline,
                    onToggle = actions.onEnableTimelineToggle,
                    subtitle = stringResource(R.string.settings_subtitle_timeline),
                )
                Divider()
                Column(modifier = Modifier.padding(16.dp)) {
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
            }}

            // --- Notifications ---
            item(key = "group_notifications") { SettingsGroup(title = stringResource(R.string.settings_group_notifications)) {
                SettingsToggleRow(
                    label = "Batch Notifications",
                    icon = Icons.Default.Inventory,
                    checked = settings.batchNotifications,
                    onToggle = actions.onBatchNotificationsToggle,
                    subtitle = "Deliver in batches every 4 hours"
                )
                if (settings.batchNotifications) {
                    SettingsActionRow(
                        label = "Deliver Batch Now",
                        icon = Icons.Default.MoveToInbox,
                        onClick = actions.onDeliverBatch
                    )
                }
                Divider()
                SettingsToggleRow(label = stringResource(R.string.settings_item_notification_dots), icon = Icons.Default.Circle, checked = settings.showNotificationDots, onToggle = actions.onShowDots)
                Divider()
                SettingsToggleRow(label = stringResource(R.string.settings_item_numerical_counts), icon = Icons.Default.Pin, checked = settings.showNumericalCounts, onToggle = actions.onShowCounts)
                Divider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_item_filter_distractions),
                    icon = Icons.Default.FilterAlt,
                    checked = settings.notificationFilterEnabled,
                    onToggle = actions.onNotificationFilterToggle,
                    subtitle = stringResource(R.string.settings_subtitle_filter_distractions)
                )
            }}

            // --- Profiles ---
            item(key = "group_profiles") { SettingsGroup(title = stringResource(R.string.settings_group_profiles)) {
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
            }}

            // --- Mindfulness ---
            item(key = "group_mindfulness") { SettingsGroup(title = stringResource(R.string.settings_group_mindfulness)) {
                SettingsToggleRow(
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
            }}

            // --- System ---
            item(key = "group_system") { SettingsGroup(title = stringResource(R.string.settings_group_system)) {
                if (!isDefaultLauncher) {
                    SettingsActionRow(label = stringResource(R.string.settings_item_set_default), icon = Icons.Default.Home, onClick = actions.onSetDefault)
                    Divider()
                }
                SettingsToggleRow(
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
                Divider()
                SettingsActionRow(label = stringResource(R.string.settings_item_icon_pack), icon = Icons.Default.Category, subtitle = settings.iconPackPackage ?: stringResource(R.string.label_default), onClick = { showIconPackDialog = true })
                Divider()
                SettingsActionRow(label = stringResource(R.string.settings_item_export), icon = Icons.Default.Download, onClick = actions.onExport)
                Divider()
                SettingsActionRow(label = stringResource(R.string.settings_item_import), icon = Icons.Default.Upload, onClick = actions.onImport)
            }}

            // --- Info ---
            item(key = "label_about") {
                Text(
                    text = stringResource(R.string.settings_label_about),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    letterSpacing = 1.sp
                )
            }
            item(key = "card_about") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    SettingsActionRow(
                        label = if (isUpdateAvailable) stringResource(R.string.settings_item_update_available) else stringResource(R.string.settings_item_about_dotz),
                        icon = Icons.Default.Info,
                        onClick = actions.onAboutClick
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
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    null, 
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLocked) 0.3f else 0.8f), 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label, 
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold
                )
                if (isPremium && isLocked) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = !isLocked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLocked) 0.3f else 0.8f), 
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label, 
                    color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold
                )
                if (isPremium && isLocked) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Icon(
            imageVector = if (isLocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
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
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            val isPro = proOptions.contains(mode)
            
            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(mode) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        label, 
                        style = MaterialTheme.typography.labelLarge, 
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isPro) {
                        Spacer(Modifier.width(6.dp))
                        PremiumBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelectionRow(currentMode: ThemeMode, isPremium: Boolean, isUpgradeAvailable: Boolean, isLiteVersion: Boolean, onModeChange: (ThemeMode) -> Unit, onShowPremiumDialog: () -> Unit) {
    val modes = if (isLiteVersion) listOf(ThemeMode.LIGHT, ThemeMode.DARK) else ThemeMode.entries
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.forEach { mode ->
            val isSelected = currentMode == mode
            val isPro = mode == ThemeMode.CIRCADIAN || mode == ThemeMode.TRANSPARENT
            val locked = isPro && isUpgradeAvailable && !isPremium

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clickable { if (locked) onShowPremiumDialog() else onModeChange(mode) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when(mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.CIRCADIAN -> Icons.Default.Schedule
                                ThemeMode.TRANSPARENT -> Icons.Default.Wallpaper
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            mode.name, 
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                        if (locked) {
                            PremiumBadge(Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransparencySlider(value: Float, isPremium: Boolean, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Opacity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.label_tile_transparency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun TileLayoutSelection(current: String, isPremium: Boolean, onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.label_tile_layout), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (!isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("classic" to stringResource(R.string.layout_grid), "list" to stringResource(R.string.layout_list)).forEach { (style, label) ->
                val selected = current == style
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { onChange(style) }, 
                    shape = RoundedCornerShape(16.dp), 
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label, 
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileManagementCard(activeId: String, profiles: List<com.dotz.launcherpro.data.LauncherProfile>, isPremium: Boolean, isUpgradeAvailable: Boolean, onSwitch: (String) -> Unit, onDelete: (String) -> Unit, onAddClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        ProfileItem(stringResource(R.string.label_default), activeId == "default", false, { onSwitch("default") }, null)
        profiles.filter { it.id != "default" }.forEach { profile ->
            ProfileItem(
                name = profile.name,
                active = activeId == profile.id,
                locked = isUpgradeAvailable && !isPremium,
                onClick = { if (!isUpgradeAvailable || isPremium) onSwitch(profile.id) },
                onDelete = { onDelete(profile.id) }
            )
        }
        TextButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_item_add_profile))
            if (isUpgradeAvailable && !isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
    }
}

@Composable
private fun ProfileItem(name: String, active: Boolean, locked: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = active, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface))
            Spacer(Modifier.width(8.dp))
            Text(name, color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            } else if (onDelete != null && !active) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun PremiumPromotionCard(onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.onSurface) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.premium_promo_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.premium_promo_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            }
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun IconPackDialog(currentIconPack: String?, iconPacks: List<Pair<String, String>>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_select_icon_pack),
        content = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text(stringResource(R.string.label_default), color = if (currentIconPack == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
                items(iconPacks.size) { index ->
                    val (pkg, name) = iconPacks[index]
                    Text(name, color = if (currentIconPack == pkg) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(pkg) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButtonText = stringResource(R.string.btn_cancel),
        onConfirm = onDismiss
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}
