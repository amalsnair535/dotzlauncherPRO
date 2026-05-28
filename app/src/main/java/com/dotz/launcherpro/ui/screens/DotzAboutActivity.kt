package com.dotz.launcherpro.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class DotzAboutActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                AboutScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onCheckUpdate = { viewModel.checkForUpdates() },
                    onDownloadUpdate = { url -> viewModel.downloadUpdate(url) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    uiState: com.dotz.launcherpro.viewmodel.LauncherUiState,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: (String) -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "5.0.0"
        } catch (e: Exception) {
            "5.0.0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ABOUT",
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
        LaunchedEffect(Unit) {
            onCheckUpdate()
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Dotz Launcher PRO ⚫",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DotzColors.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version $versionName",
                    fontSize = 14.sp,
                    color = DotzColors.White.copy(alpha = 0.5f)
                )
            }

            item {
                val updateLabel = when {
                    uiState.isUpdateAvailable -> "Update to ${uiState.latestVersionName} available"
                    uiState.isCheckingForUpdate -> "Checking for updates..."
                    else -> "You are on the latest version"
                }

                AboutActionRow(
                    label = "Updates",
                    subLabel = updateLabel,
                    icon = if (uiState.isUpdateAvailable) Icons.Default.Download else Icons.Default.Refresh,
                    onClick = {
                        if (uiState.isUpdateAvailable) {
                            uiState.updateApkUrl?.let { onDownloadUpdate(it) }
                        } else {
                            onCheckUpdate()
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Minimal Android launcher focused on calm and intentional phone usage.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = DotzColors.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            item { MatteDivider() }

            item {
                AboutActionRow(
                    label = "GitHub",
                    subLabel = "amalsnair535/DotzLauncherPRO",
                    icon = Icons.Default.Code,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/DotzLauncherPRO".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Contact",
                    subLabel = "dotzlauncher@gmail.com",
                    icon = Icons.Default.Email,
                    trailingIcon = Icons.Default.ContentCopy,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Dotz Contact", "dotzlauncher@gmail.com")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { MatteDivider() }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "PRIVACY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        color = DotzColors.White.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Dotz Launcher PRO does not collect or store any personal data. All settings and configurations are kept locally on your device.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = DotzColors.White.copy(alpha = 0.6f)
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Built with ❤️",
                    fontSize = 12.sp,
                    color = DotzColors.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun AboutActionRow(
    label: String,
    subLabel: String,
    icon: ImageVector,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzColors.Tile, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DotzColors.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = DotzColors.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subLabel, color = DotzColors.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = DotzColors.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MatteDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = DotzColors.White.copy(alpha = 0.1f)
    )
}
