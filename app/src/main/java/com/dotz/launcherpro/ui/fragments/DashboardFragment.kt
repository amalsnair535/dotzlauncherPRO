package com.dotz.launcherpro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dotz.launcherpro.ui.components.*
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

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
                    DashboardScreen(uiState)
                }
            }
        }
    }

    @Composable
    fun DashboardScreen(uiState: com.dotz.launcherpro.viewmodel.LauncherUiState) {
        val scrollState = rememberScrollState()

        // Auto-scroll to bottom when AI response arrives
        LaunchedEffect(uiState.aiResponse) {
            if (uiState.aiResponse != null) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

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
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DASHBOARD",
                    color = DotzTheme.colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (uiState.isUpgradeAvailable) {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(DotzTheme.colors.text.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = DotzTheme.colors.text,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Grid Layout for Cards - Use IntrinsicSize.Min to match heights dynamically
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                AppUsageCard(
                    uiState = uiState, 
                    onClick = { viewModel.openDigitalWellbeing() },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                
                if (!uiState.isLiteVersion) {
                    Spacer(Modifier.width(16.dp))
                    if (uiState.isPremium) {
                        AiSummaryCard(uiState = uiState, modifier = Modifier.weight(1f).fillMaxHeight())
                    } else {
                        LockedDashboardCard(
                            title = "AI SUMMARY",
                            icon = Icons.Default.AutoAwesome,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NowPlayingCard(
                uiState = uiState,
                onPlayPause = { viewModel.mediaPlayPause() },
                onSkipNext = { viewModel.mediaSkipNext() },
                onSkipPrevious = { viewModel.mediaSkipPrevious() },
                modifier = Modifier.fillMaxWidth()
            )

            if (!uiState.isLiteVersion) {
                Spacer(Modifier.height(16.dp))

                if (uiState.isPremium) {
                    AiAssistantCard(
                        uiState = uiState,
                        onAsk = { viewModel.askAi(it) },
                        onClear = { viewModel.clearAi() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LockedDashboardCard(
                        title = "DOTZ AI",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}
