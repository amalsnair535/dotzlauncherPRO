package com.dotz.launcherpro

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.dotz.launcherpro.ui.LauncherPagerAdapter
import com.dotz.launcherpro.viewmodel.LauncherViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = LauncherPagerAdapter(this)
        
        // Start on Home Screen (index 1)
        viewPager.setCurrentItem(1, false)

        // Observe dashboard setting
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                viewPager.isUserInputEnabled = state.settings.enableDashboard
                if (!state.settings.enableDashboard) {
                    viewPager.setCurrentItem(1, true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        // Re-apply immersive mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Disable back button — this IS the home screen */
    @Deprecated("Deprecated in API 33")
    override fun onBackPressed() {
        // Do nothing or call super if you want default behavior
        // super.onBackPressed()
    }
}
