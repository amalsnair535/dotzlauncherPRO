package com.dotz.launcherpro.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.ui.components.AppDrawerSheet
import com.dotz.launcherpro.ui.screens.AppSelectionActivity
import com.dotz.launcherpro.ui.screens.DotzHomeScreen
import com.dotz.launcherpro.ui.screens.DotzSettingsActivity
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

data class MindfulnessInfo(
    val pkg: String,
    val label: String,
    val usageTime: String?,
    val launchCount: Int
)

class HomeFragment : Fragment() {

    private val viewModel: LauncherViewModel by activityViewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshWeather()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Ensure the view itself is transparent to see the wallpaper
            setBackgroundColor(android.graphics.Color.TRANSPARENT)

            setContent {
                val uiState by viewModel.uiState.collectAsState()
                DotzTheme(settings = uiState.settings) {
                    var showNotifPermDialog by remember { mutableStateOf(false) }
                    var showDefaultLauncherDialog by remember { mutableStateOf(false) }
                    var showAppAccessDisclosure by remember { mutableStateOf(false) }
                    var showAppDrawer by remember { mutableStateOf(false) }
                    var tileToAssign by remember { mutableStateOf<AppTile?>(null) }
                    var swapSourceTile by remember { mutableStateOf<AppTile?>(null) }
                    var mindfulnessApp by remember { mutableStateOf<MindfulnessInfo?>(null) }
                    var showUsageStatsDialog by remember { mutableStateOf(false) }

                    // Stable initialization for disclosures and permissions
                    LaunchedEffect(Unit) {
                        if (!isNotificationListenerEnabled()) showNotifPermDialog = true
                        
                        if (!uiState.settings.hasAcceptedAppDisclosure) {
                            showAppAccessDisclosure = true
                        }
                        
                        if (uiState.settings.showMindfulUsage && !viewModel.hasUsageStatsPermission()) {
                            showUsageStatsDialog = true
                        }
                    }

                    LaunchedEffect(uiState.isDefaultLauncher) {
                        showDefaultLauncherDialog = !uiState.isDefaultLauncher
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        DotzHomeScreen(
                            uiState = uiState,
                            iconCache = viewModel.iconCache,
                            onTileTap = { tile ->
                                if (swapSourceTile != null) {
                                    if (swapSourceTile!!.tileId != tile.tileId) {
                                        viewModel.moveTile(swapSourceTile!!.tileId, tile.tileId)
                                        hapticPulse()
                                    }
                                    swapSourceTile = null
                                } else {
                                    val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(tile.packageName)
                                    if (uiState.settings.showMindfulUsage && tile.launchCount >= 10 && isSocial) {
                                        mindfulnessApp = MindfulnessInfo(tile.packageName, tile.label, tile.usageTime, tile.launchCount)
                                    } else {
                                        handleTileClick(tile.packageName) {
                                            tileToAssign = tile
                                        }
                                    }
                                }
                            },
                            onTileLongPress = { tile ->
                                hapticPulse()
                                swapSourceTile = tile
                            },
                            onLauncherSettingsTap = {
                                startActivity(Intent(requireContext(), DotzSettingsActivity::class.java))
                            },
                            onWifiToggle = viewModel::toggleWifiDirect,
                            onBluetoothToggle = viewModel::toggleBluetoothDirect,
                            onSilentToggle = viewModel::toggleSilentMode,
                            onTorchToggle = viewModel::toggleTorch,
                            onAirplaneToggle = viewModel::toggleAirplaneModeDirect,
                            onDarkModeToggle = viewModel::toggleDarkModeDirect,
                            onDataClick = viewModel::toggleMobileDataDirect,
                            onWeatherClick = viewModel::openWeatherApp,
                            onWifiLongClick = { hapticPulse(); viewModel.toggleWifi() },
                            onBluetoothLongClick = { hapticPulse(); viewModel.toggleBluetooth() },
                            onDataLongClick = { hapticPulse(); viewModel.openMobileDataSettings() },
                            onAirplaneLongClick = { hapticPulse(); viewModel.toggleAirplaneMode() },
                            onSilentLongClick = { hapticPulse(); viewModel.toggleSilentMode() },
                            onTorchLongClick = { hapticPulse(); viewModel.toggleTorch() },
                            onDarkModeLongClick = { hapticPulse(); viewModel.toggleDarkMode() },
                            onPageChanged = viewModel::setInnerPage,
                            onOpenDrawer = {
                                if (uiState.settings.enableAppDrawer) {
                                    hapticPulse()
                                    showAppDrawer = true
                                }
                            },
                            highlightedTileId = swapSourceTile?.tileId
                        )

                        if (showAppDrawer) {
                            val installedApps = remember { viewModel.getInstalledApps() }
                            AppDrawerSheet(
                                apps = installedApps,
                                onDismiss = { showAppDrawer = false },
                                onLaunch = { pkg ->
                                    val app = installedApps.find { it.packageName == pkg }
                                    val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
                                    if (app != null && uiState.settings.showMindfulUsage && app.launchCount >= 10 && isSocial) {
                                        showAppDrawer = false
                                        mindfulnessApp = MindfulnessInfo(pkg, app.label, app.usageTime, app.launchCount)
                                    } else {
                                        showAppDrawer = false
                                        launchApp(pkg)
                                    }
                                }
                            )
                        }

                        mindfulnessApp?.let { info ->
                            MindfulnessDialog(
                                label = info.label,
                                usageTime = info.usageTime ?: "0m",
                                launchCount = info.launchCount,
                                onDismiss = { mindfulnessApp = null },
                                onConfirm = {
                                    val pkg = info.pkg
                                    mindfulnessApp = null
                                    launchApp(pkg)
                                }
                            )
                        }

                        if (showNotifPermDialog) {
                            NotificationPermissionDialog(
                                onDismiss = { showNotifPermDialog = false },
                                onGoToSettings = {
                                    showNotifPermDialog = false
                                    try {
                                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (_: Exception) {
                                        startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                            )
                        }

                        if (showDefaultLauncherDialog) {
                            DefaultLauncherDialog(
                                onDismiss = { showDefaultLauncherDialog = false },
                                onGoToSettings = {
                                    showDefaultLauncherDialog = false
                                    viewModel.openDefaultLauncherSettings()
                                }
                            )
                        }

                        if (showAppAccessDisclosure) {
                            AppAccessDisclosureDialog(
                                onAccept = {
                                    showAppAccessDisclosure = false
                                    viewModel.acceptAppDisclosure()
                                }
                            )
                        }

                        if (showUsageStatsDialog) {
                            UsageStatsPermissionDialog(
                                onDismiss = { showUsageStatsDialog = false },
                                onGoToSettings = {
                                    showUsageStatsDialog = false
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            )
                        }

                        tileToAssign?.let { tile ->
                            UnassignedTileDialog(
                                tileLabel = tile.label,
                                onDismiss = { tileToAssign = null },
                                onSelectApp = {
                                    tileToAssign = null
                                    startActivity(
                                        Intent(requireContext(), AppSelectionActivity::class.java)
                                            .putExtra("tileId", tile.tileId)
                                            .putExtra("tileLabel", tile.label),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleTileClick(packageName: String, onUnassigned: () -> Unit) {
        if (packageName == requireContext().packageName) {
            startActivity(Intent(requireContext(), DotzSettingsActivity::class.java))
            return
        }

        launchApp(packageName) ?: onUnassigned()
    }

    private fun launchApp(packageName: String): Unit? {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Unit
        } else null
    }

    private fun hapticPulse() {
        val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return enabled?.contains(requireContext().packageName) == true
    }

    private fun hasLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun UsageStatsPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = { Text("Mindful Usage", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = { Text("Enable usage stats to see how much time you spend in apps and set mindful limits.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("ENABLE", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("NOT NOW", color = Color.White.copy(alpha = 0.4f)) }
        }
    )
}

@Composable
private fun MindfulnessDialog(label: String, usageTime: String, launchCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = {
            Text(
                "Mindful Check",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    "You've opened $label $launchCount times today.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                Text(
                    "Total time spent: $usageTime",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                Text(
                    "Do you really want to open it again?",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("YES, PROCEED", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("NO, STAY FOCUSED", color = Color.White.copy(alpha = 0.4f))
            }
        }
    )
}

@Composable
private fun NotificationPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = { Text("Enable Notifications", color = Color.White, fontSize = 16.sp) },
        text = { Text("Allow Dotz to read notifications for badges.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("ENABLE", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("SKIP", color = Color.White.copy(alpha = 0.4f)) }
        }
    )
}

@Composable
private fun DefaultLauncherDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = { Text("Set as Default Launcher", color = Color.White, fontSize = 16.sp) },
        text = { Text("Use Dotz as your main home screen.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("SET DEFAULT", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("SKIP", color = Color.White.copy(alpha = 0.4f)) }
        }
    )
}

@Composable
private fun AppAccessDisclosureDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Dismiss not allowed to ensure acceptance */ },
        containerColor = Color.Black,
        title = { Text("App Visibility", color = Color.White, fontSize = 16.sp) },
        text = { Text("To function as a launcher, Dotz needs to see your installed apps so you can assign them to tiles. We only use this list locally and never share it.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("I UNDERSTAND", color = Color.White) }
        }
    )
}

@Composable
private fun UnassignedTileDialog(tileLabel: String, onDismiss: () -> Unit, onSelectApp: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = { Text("Unassigned Tile", color = Color.White, fontSize = 16.sp) },
        text = { Text("Assign an app to $tileLabel?", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onSelectApp) { Text("SELECT APP", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.4f)) }
        }
    )
}
