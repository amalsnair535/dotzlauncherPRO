package com.dotz.launcherpro.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dotz.launcherpro.ui.fragments.DashboardFragment
import com.dotz.launcherpro.ui.fragments.HomeFragment

class LauncherPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()  // Swipe Right Screen (Dashboard on the left)
            1 -> HomeFragment()       // Main Minimal Screen (Starts here)
            else -> HomeFragment()
        }
    }
}
