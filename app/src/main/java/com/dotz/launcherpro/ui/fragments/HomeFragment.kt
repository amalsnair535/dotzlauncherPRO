package com.dotz.launcherpro.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dotz.launcherpro.data.AppTile
import com.dotz.launcherpro.ui.screens.AppSelectionActivity
import com.dotz.launcherpro.ui.screens.DotzHomeScreen
import com.dotz.launcherpro.ui.screens.DotzSettingsActivity
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class HomeFragment : Fragment() {

    private val viewModel: LauncherViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                DotzTheme(settings = uiState.settings) {
                    var showNotifPermDialog by remember { mutableStateOf(false) }
                    var showDefaultLauncherDialog by remember { mutableStateOf(false) }
                    var tileToAssign by remember { mutableStateOf<AppTile?>(null) }

                    LaunchedEffect(uiState.isDefaultLauncher) {
                        if (!isNotificationListenerEnabled()) showNotifPermDialog = true
                        showDefaultLauncherDialog = !uiState.isDefaultLauncher
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        DotzHomeScreen(
                            uiState = uiState,
                            iconCache = viewModel.iconCache,
                            onTileTap = { tile ->
                                viewModel.onTileTapped(tile)
                                handleTileClick(tile) {
                                    tileToAssign = tile
                                }
                            },
                            onTileLongPress = {
                                hapticPulse()
                                startActivity(Intent(requireContext(), DotzSettingsActivity::class.java))
                            },
                            onLauncherSettingsTap = {
                                startActivity(Intent(requireContext(), DotzSettingsActivity::class.java))
                            },
                            onWifiToggle = viewModel::toggleWifi,
                            onBluetoothToggle = viewModel::toggleBluetooth,
                            onSilentToggle = viewModel::toggleSilentMode,
                            onTorchToggle = viewModel::toggleTorch,
                            onAirplaneToggle = viewModel::toggleAirplaneMode,
                            onDarkModeToggle = viewModel::toggleDarkMode,
                            onDataClick = viewModel::openMobileDataSettings,
                            onWeatherClick = viewModel::openWeatherApp,
                        )

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

    private fun handleTileClick(tile: AppTile, onUnassigned: () -> Unit) {
        if (tile.packageName == requireContext().packageName) {
            startActivity(Intent(requireContext(), DotzSettingsActivity::class.java))
            return
        }

        if (tile.isInstalled) {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(tile.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                onUnassigned()
            }
        } else {
            onUnassigned()
        }
    }

    private fun hapticPulse() {
        val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return enabled?.contains(requireContext().packageName) == true
    }
}

@Composable
private fun NotificationPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DotzColors.Tile,
        title = { Text("Enable Notifications", color = DotzColors.White, fontSize = 16.sp) },
        text = { Text("Allow Dotz to read notifications for badges.", color = DotzColors.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("ENABLE", color = DotzColors.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("SKIP", color = DotzColors.White.copy(alpha = 0.4f)) }
        }
    )
}

@Composable
private fun DefaultLauncherDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DotzColors.Tile,
        title = { Text("Set as Default Launcher", color = DotzColors.White, fontSize = 16.sp) },
        text = { Text("Use Dotz as your main home screen.", color = DotzColors.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("SET DEFAULT", color = DotzColors.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("SKIP", color = DotzColors.White.copy(alpha = 0.4f)) }
        }
    )
}

@Composable
private fun UnassignedTileDialog(tileLabel: String, onDismiss: () -> Unit, onSelectApp: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DotzColors.Tile,
        title = { Text("Unassigned Tile", color = DotzColors.White, fontSize = 16.sp) },
        text = { Text("Assign an app to $tileLabel?", color = DotzColors.White.copy(alpha = 0.7f), fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onSelectApp) { Text("SELECT APP", color = DotzColors.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = DotzColors.White.copy(alpha = 0.4f)) }
        }
    )
}
