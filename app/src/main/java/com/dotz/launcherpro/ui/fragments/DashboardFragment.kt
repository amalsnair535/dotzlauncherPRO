package com.dotz.launcherpro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Top Status Bar Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Grid Layout for Cards - Use IntrinsicSize.Min to match heights dynamically
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                FocusStatsCard(uiState = uiState, modifier = Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.width(16.dp))
                AiSummaryCard(uiState = uiState, modifier = Modifier.weight(1f).fillMaxHeight())
            }

            Spacer(Modifier.height(16.dp))

            NowPlayingCard(
                uiState = uiState,
                onPlayPause = { viewModel.mediaPlayPause() },
                onSkipNext = { viewModel.mediaSkipNext() },
                onSkipPrevious = { viewModel.mediaSkipPrevious() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            AiAssistantCard(
                uiState = uiState,
                onAsk = { viewModel.askAi(it) },
                onClear = { viewModel.clearAi() },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
