package com.dotz.launcherpro.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherUiState
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class DotzAboutActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by viewModel.themeState.collectAsState()
            DotzTheme(settings = theme.settings) {
                AboutScreen(
                    onBack = { finish() },
                    isUpdateAvailable = theme.isUpdateAvailable
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(com.dotz.launcherpro.R.anim.stay, com.dotz.launcherpro.R.anim.slide_down)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    isUpdateAvailable: Boolean
) {
    val context = LocalContext.current
    val viewModel: LauncherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "6.0.0"
        } catch (_: Exception) {
            "6.0.0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ABOUT",
                        style = MaterialTheme.typography.labelLarge,
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
                    text = "Dotz Launcher",
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

            // ── Primary Actions ───────────────────────────────────────────
            item {
                AboutActionRow(
                    label = "Rate Dotz Launcher",
                    subLabel = "Support the development with a review",
                    icon = Icons.Default.Star,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                        try { context.startActivity(intent) } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()))
                        }
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "What's New",
                    subLabel = "Read the latest update notes",
                    icon = Icons.Default.NewReleases,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/DotzLauncherPRO/blob/main/CHANGELOG.md".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Buy Me a Coffee / Donation",
                    subLabel = "Support the project via UPI",
                    icon = Icons.Default.Coffee,
                    onClick = {
                        val upiUri = "upi://pay?pa=amalsnair535-1@okhdfcbank&pn=Amal%20Nair&mc=0000&mode=02&purpose=00"
                        val intent = Intent(Intent.ACTION_VIEW, upiUri.toUri())
                        val chooser = Intent.createChooser(intent, "Pay with...")
                        try { context.startActivity(chooser) } catch (_: Exception) {
                            Toast.makeText(context, "No UPI apps found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item { MatteDivider() }

            // ── Legal & Open Source ──────────────────────────────────────
            item {
                AboutActionRow(
                    label = "Privacy Policy",
                    subLabel = "Our commitment to your data",
                    icon = Icons.Default.Description,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/dotzlauncherPRO/blob/main/PRIVACY_POLICY.md".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Licenses",
                    subLabel = "Review software licenses",
                    icon = Icons.AutoMirrored.Filled.Article,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/DotzLauncherPRO/blob/main/LICENSE".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Open Source Libraries",
                    subLabel = "Check out our GitHub repository",
                    icon = Icons.Default.Security,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/amalsnair535/DotzLauncherPRO".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item { MatteDivider() }

            // ── Feedback & Support ────────────────────────────────────────
            item {
                AboutActionRow(
                    label = "Report a Bug",
                    subLabel = "Tell us what's broken",
                    icon = Icons.Default.BugReport,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:amalsnair535@gmail.com".toUri()).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Dotz Bug Report")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
            }

            item {
                AboutActionRow(
                    label = "Suggest a Feature",
                    subLabel = "What should we build next?",
                    icon = Icons.Default.Lightbulb,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:amalsnair535@gmail.com".toUri()).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Dotz Feature Suggestion")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
            }

            item { MatteDivider() }

            // ── Socials ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/+w_zs7VSQ3tllMGQ9".toUri()))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Telegram", tint = DotzTheme.colors.text.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.instagram.com/dotzlauncher?igsh=Znd3eXFxZWlpMzUw".toUri()))
                    }) {
                        Icon(Icons.Default.CameraAlt, "Instagram", tint = DotzTheme.colors.text.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://amalsnair535.github.io/dotzlauncherPRO/".toUri()))
                    }) {
                        Icon(Icons.Default.Language, "Website", tint = DotzTheme.colors.text.copy(alpha = 0.6f))
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Built with ❤️ in India",
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
