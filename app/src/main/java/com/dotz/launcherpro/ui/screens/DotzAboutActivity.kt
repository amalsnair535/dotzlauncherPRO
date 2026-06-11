package com.dotz.launcherpro.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
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
import com.dotz.launcherpro.viewmodel.LauncherUiState
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
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "5.5.0"
        } catch (_: Exception) {
            "5.5.0"
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
                    text = "Dotz Launcher PRO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DotzTheme.colors.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version $versionName",
                    fontSize = 14.sp,
                    color = DotzTheme.colors.text.copy(alpha = 0.5f)
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Minimal Android launcher focused on calm and intentional phone usage.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = DotzTheme.colors.text.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            item { MatteDivider() }

            item {
                AboutActionRow(
                    label = "Website",
                    subLabel = "amalsnair535.github.io/dotzlauncherPRO",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://amalsnair535.github.io/dotzlauncherPRO/".toUri())
                        context.startActivity(intent)
                    }
                )
            }

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
                    subLabel = "amalsnair535@gmail.com",
                    icon = Icons.Default.Email,
                    trailingIcon = Icons.Default.ContentCopy,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Dotz Contact", "amalsnair535@gmail.com")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { MatteDivider() }

            item {
                AboutActionRow(
                    label = "Privacy Policy",
                    subLabel = "Read our commitment to your privacy",
                    icon = Icons.Default.Code,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://gist.github.com/amalsnair535/c114456d49cd0aed815bd79d5bbe05d4".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Terms & Conditions",
                    subLabel = "Review our service terms",
                    icon = Icons.Default.Code,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/dotzlauncherPRO/blob/bbb11de4532e26beec065f9dd5b0718494ab3b88/TERMS_AND_CONDITIONS.md".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item { MatteDivider() }

            item {
                AboutActionRow(
                    label = "Support the Project",
                    subLabel = "Buy Me a Coffee (UPI)",
                    icon = Icons.Default.Coffee,
                    onLongClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Dotz UPI", "amalsnair535-1@okhdfcbank")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "UPI ID copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onClick = {
                        val upiUri = "upi://pay?pa=amalsnair535-1@okhdfcbank&pn=Amal%20Nair&mc=0000&mode=02&purpose=00"
                        val intent = Intent(Intent.ACTION_VIEW, upiUri.toUri())
                        val chooser = Intent.createChooser(intent, "Pay with...")
                        try {
                            context.startActivity(chooser)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No UPI apps found", Toast.LENGTH_SHORT).show()
                        }
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
                        color = DotzTheme.colors.text.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Dotz Launcher PRO does not collect or store any personal data. All settings and configurations are kept locally on your device.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = DotzTheme.colors.text.copy(alpha = 0.6f)
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Built with ❤️",
                    fontSize = 12.sp,
                    color = DotzTheme.colors.text.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutActionRow(
    label: String,
    subLabel: String,
    icon: ImageVector,
    trailingIcon: ImageVector? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DotzTheme.colors.text.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = DotzTheme.colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subLabel, color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = DotzTheme.colors.text.copy(alpha = 0.3f),
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
        color = DotzTheme.colors.text.copy(alpha = 0.1f)
    )
}
