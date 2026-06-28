package com.dotz.launcherpro

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import android.view.View
import com.dotz.launcherpro.ui.LauncherPagerAdapter
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Ensure window is transparent for wallpaper
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
        )

        // Modern full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = LauncherPagerAdapter(this)
        
        // Fluid Page Transformer
        viewPager.setPageTransformer(FluidPageTransformer())
        viewPager.offscreenPageLimit = 2

        // Start on Home Screen (index 1)
        viewPager.setCurrentItem(1, false)
        viewModel.setFastlaneVisible(false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setFastlaneVisible(position == 0)
                // When moving away from dashboard, we might need to re-enable user input 
                // based on the current inner page state
                updateSwipeAbility()
            }
        })

        // Observe dashboard setting and inner page state to prevent nested scroll conflicts
        lifecycleScope.launch {
            combine(viewModel.uiState, viewModel.currentInnerPage) { _, _ ->
                Unit
            }.collectLatest {
                updateSwipeAbility()
            }
        }
    }

    private fun updateSwipeAbility() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager) ?: return
        val uiState = viewModel.uiState.value
        val innerPage = viewModel.currentInnerPage.value
        val fastlaneEnabled = uiState.settings.enableFastlane
        val isVertical = uiState.settings.verticalScrolling
        
        // Logic:
        // 1. If we are on Fastlane (0), we can ALWAYS swipe back to Home (1).
        // 2. If we are on Home (1):
        //    - If vertical scrolling is ON, we can ALWAYS swipe to Fastlane (0) because there's no horizontal conflict.
        //    - If vertical scrolling is OFF, we can only swipe to Fastlane (0) if we are on the first inner horizontal page (index 0).
        val canSwipeToFastlane = isVertical || innerPage == 0
        viewPager.isUserInputEnabled = fastlaneEnabled && (canSwipeToFastlane || viewPager.currentItem == 0)
        
        if (!fastlaneEnabled && viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, false)
        }
    }

    override fun onResume() {
        super.onResume()
        // No need to refresh everything on every resume, DataStore handles settings updates.
        // viewModel.refreshState() 
        
        // Re-apply immersive mode without animation if possible
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Minimal PageTransformer that ensures the native ViewPager2 feel.
     */
    class FluidPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.alpha = if (position <= -1f || position >= 1f) 0f else 1f
        }
    }
}
