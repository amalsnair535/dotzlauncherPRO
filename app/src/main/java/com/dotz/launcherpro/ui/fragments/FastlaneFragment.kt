package com.dotz.launcherpro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class FastlaneFragment : Fragment() {

    private val viewModel: LauncherViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                DotzTheme(settings = uiState.settings) {
                    FastlaneScreen(uiState)
                }
            }
        }
    }

    @Composable
    fun FastlaneScreen(uiState: com.dotz.launcherpro.viewmodel.LauncherUiState) {
        val scrollState = rememberScrollState()
        var mindfulnessApp by remember { mutableStateOf<MindfulnessInfo?>(null) }

        val backgroundColor = if (uiState.settings.showWallpaper) Color.Transparent else DotzTheme.colors.background

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "FASTLANE",
                    color = DotzTheme.colors.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                if (!uiState.isPremium && uiState.isUpgradeAvailable) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }

            FastlaneContent(uiState) { mindfulnessApp = it }
            
            Spacer(Modifier.height(48.dp))
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
                    viewModel.launchApp(pkg)
                }
            )
        }
    }

    @Composable
    private fun FastlaneContent(
        uiState: com.dotz.launcherpro.viewmodel.LauncherUiState,
        onMindfulLaunch: (MindfulnessInfo) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (uiState.timelineItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent activity to show in Fastlane.",
                        color = DotzTheme.colors.text.copy(alpha = 0.2f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                uiState.timelineItems.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // Timeline Line & Dot Column
                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top half of the line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .weight(1f)
                                    .background(if (index == 0) Color.Transparent else DotzTheme.colors.text.copy(alpha = 0.1f))
                            )
                            
                            // The Dot (matches the icon position)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DotzTheme.colors.text.copy(alpha = 0.2f))
                            )
                            
                            // Bottom half of the line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .weight(1f)
                                    .background(if (index == uiState.timelineItems.size - 1) Color.Transparent else DotzTheme.colors.text.copy(alpha = 0.1f))
                            )
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        TimelineCard(
                            item = item,
                            onItemClick = { pkg -> 
                                if (pkg != null) {
                                    val isSocial = com.dotz.launcherpro.data.DefaultApps.isSocialMediaApp(pkg)
                                    // Check mindfulness logic
                                    val app = uiState.topApps.find { it.packageName == pkg }
                                    val launchCount = app?.launchCount ?: 0
                                    val usageTime = app?.usageTime
                                    val label = app?.label ?: pkg.substringAfterLast('.')

                                    if (uiState.settings.showMindfulUsage && launchCount >= 3 && isSocial) {
                                        onMindfulLaunch(MindfulnessInfo(pkg, label, usageTime, launchCount))
                                    } else {
                                        viewModel.launchApp(pkg)
                                    }
                                }
                            },
                            onPlayPause = { viewModel.mediaPlayPause() },
                            onSkipNext = { viewModel.mediaSkipNext() },
                            onSkipPrevious = { viewModel.mediaSkipPrevious() },
                            onReply = { key, msg -> viewModel.sendReply(key, msg) },
                            isPlaying = uiState.isPlaying,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
