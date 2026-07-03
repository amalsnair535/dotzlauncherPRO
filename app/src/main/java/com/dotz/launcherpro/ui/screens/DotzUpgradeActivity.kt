package com.dotz.launcherpro.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class DotzUpgradeActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Seamless immersive mode
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val lifetimePrice by viewModel.lifetimePrice.collectAsState()

            // Close screen if premium status becomes true
            LaunchedEffect(uiState.isPremium) {
                if (uiState.isPremium) {
                    finish()
                }
            }

            DotzTheme(settings = uiState.settings) {
                UpgradeScreen(
                    onBack = { finish() },
                    lifetimePrice = lifetimePrice,
                    onUpgrade = { planId ->
                        viewModel.buyProduct(this, planId)
                    }
                )
            }
        }
    }
}

@Composable
private fun UpgradeScreen(
    onBack: () -> Unit,
    lifetimePrice: String,
    onUpgrade: (String) -> Unit
) {
    Scaffold(
        containerColor = DotzTheme.colors.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DotzTheme.colors.text)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = DotzTheme.colors.text,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Dotz Launcher PRO",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DotzTheme.colors.text,
                    letterSpacing = 2.sp
                )
                Text(
                    "PRO FEATURE THAT NO ONE WANTS 🙄",
                    fontSize = 12.sp,
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(40.dp))
            }

            // Features List
            item {
                FeatureRow("Transparent Mode (Custom Wallpapers)")
                FeatureRow("Circadian Theming (Dynamic UI Colors)")
                FeatureRow("Tile Transparency Control")
                FeatureRow("Modern List Layout")
                FeatureRow("Premium Fastlane Experience")
                Spacer(Modifier.height(40.dp))
            }

            // Plans
            item {
                PlanCard(
                    name = "Lifetime Access",
                    price = lifetimePrice,
                    isSelected = true,
                    badge = "PRO FEATURE THAT NO ONE WANTS 🙄"
                ) { /* Only one plan available */ }
                Spacer(Modifier.height(40.dp))
            }

            // Subscribe Button
            item {
                Button(
                    onClick = { onUpgrade("dotz_pro_lifetime") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DotzTheme.colors.text,
                        contentColor = DotzTheme.colors.background
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "GET LIFETIME ACCESS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "One-time purchase. No subscription.",
                    fontSize = 11.sp,
                    color = DotzTheme.colors.text.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(32.dp))
            }

            item {
                val context = LocalContext.current
                val viewModel: LauncherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                OutlinedButton(
                    onClick = {
                        viewModel.showRewardedAd(context as ComponentActivity) {
                            viewModel.grant24HourPremium()
                            Toast.makeText(context, "24 Hours PRO Unlocked!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DotzTheme.colors.text
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DotzTheme.colors.text.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "WATCH AD FOR 24h PRO",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Try all premium features for one day.",
                    fontSize = 11.sp,
                    color = DotzTheme.colors.text.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = DotzTheme.colors.text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, color = DotzTheme.colors.text, fontSize = 14.sp)
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) DotzTheme.colors.text.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = DotzTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(DotzTheme.colors.text, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(badge, color = DotzTheme.colors.background, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(price, color = DotzTheme.colors.text.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = DotzTheme.colors.text,
                    unselectedColor = DotzTheme.colors.text.copy(alpha = 0.3f)
                )
            )
        }
    }
}
