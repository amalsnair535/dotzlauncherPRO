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
        viewModel.setDashboardVisible(false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setDashboardVisible(position == 0)
            }
        })

        // Observe dashboard setting and inner page state to prevent nested scroll conflicts
        lifecycleScope.launch {
            combine(viewModel.uiState, viewModel.currentInnerPage) { state, innerPage ->
                state.settings.enableDashboard to innerPage
            }.collectLatest { (dashboardEnabled, innerPage) ->
                viewPager.isUserInputEnabled = dashboardEnabled && (innerPage == 0 || viewPager.currentItem == 0)
                
                if (!dashboardEnabled && viewPager.currentItem != 1) {
                    viewPager.setCurrentItem(1, false)
                }
            }
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
     * Custom PageTransformer for fluid transitions between Dashboard and Home.
     */
    class FluidPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            val absPos = abs(position)
            view.apply {
                translationX = 0f
                alpha = if (absPos >= 1f) 0f else 1f - absPos
            }
        }
    }
}
