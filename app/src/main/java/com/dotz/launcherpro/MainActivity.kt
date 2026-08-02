package com.dotz.launcherpro

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dotz.launcherpro.ui.screens.DotzHomeScreen
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect

class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Ensure window is transparent for wallpaper
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
        )

        // Ensure app draws behind cutouts for true edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // Modern full-screen immersive mode
        enableEdgeToEdge()
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setHighRefreshRate()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            // Start background tasks once UI is ready
            LaunchedEffect(Unit) {
                viewModel.startBackgroundTasks()
            }

            DotzTheme(settings = uiState.theme.settings) {
                DotzHomeScreen(
                    viewModel = viewModel
                ) {
                    val intent = android.content.Intent(this, com.dotz.launcherpro.ui.screens.DotzSettingsActivity::class.java)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_up, R.anim.stay)
                }
            }
        }
    }

    private fun setHighRefreshRate() {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
            }
            
            val modes = display?.supportedModes
            val highRefreshMode = modes?.maxByOrNull { it.refreshRate }
            
            if (highRefreshMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = highRefreshMode.modeId
                window.attributes = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setUiVisible(visible = true)
        // Re-apply immersive mode
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.setUiVisible(visible = false)
    }
}
