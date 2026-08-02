package com.dotz.launcherpro.manager

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

data class UsageStatsResult(
    val appStats: Map<String, Pair<String?, Int>>,
    val totalScreenTime: Long,
    val unlockCount: Int,
    val notificationsReceived: Int,
    val totalAppOpens: Int = 0
)

class UsageManager(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getAllAppStatsToday(): UsageStatsResult {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = mutableMapOf<String, Long>()
        val countMap = mutableMapOf<String, Int>()
        
        var totalScreenTime = 0L
        var unlockCount = 0
        var notificationsReceived = 0
        val myPackage = context.packageName

        try {
            // 1. Get Per-App Foreground Time
            val aggregateStats = usm.queryAndAggregateUsageStats(startTime, endTime)
            aggregateStats?.forEach { (pkg, stat) ->
                val time = stat.totalTimeInForeground
                if (time > 0) {
                    statsMap[pkg] = time
                }
            }

            // 2. Get Accurate Total Screen Time, Launch Counts, Unlocks & Notifications from Events
            val events = usm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            
            var currentForegroundPackage: String? = null
            var foregroundStartTime = 0L

            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (pm.getLaunchIntentForPackage(pkg) != null) {
                            countMap[pkg] = (countMap[pkg] ?: 0) + 1
                        }
                        
                        val isCountedApp = pkg != myPackage && 
                                          pkg != "com.android.systemui" && 
                                          pm.getLaunchIntentForPackage(pkg) != null

                        if (isCountedApp) {
                            if (currentForegroundPackage == null) {
                                foregroundStartTime = event.timeStamp
                            }
                            currentForegroundPackage = pkg
                        } else {
                            if (currentForegroundPackage != null) {
                                totalScreenTime += (event.timeStamp - foregroundStartTime)
                                currentForegroundPackage = null
                            }
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (currentForegroundPackage == pkg) {
                            totalScreenTime += (event.timeStamp - foregroundStartTime)
                            currentForegroundPackage = null
                        }
                    }
                    16 -> { // KEYGUARD_DISMISSED
                        unlockCount++
                    }
                    12 -> { // NOTIFICATION_INTERRUPTION
                        notificationsReceived++
                    }
                }
            }
            
            if (currentForegroundPackage != null && currentForegroundPackage != myPackage) {
                totalScreenTime += (endTime - foregroundStartTime)
            }

        } catch (_: Exception) {}

        val appStats = (statsMap.keys + countMap.keys).associateWith { pkg ->
            val totalMillis = statsMap[pkg] ?: 0L
            val timeStr = if (totalMillis > 60000) formatDuration(totalMillis) else null
            val count = countMap[pkg] ?: 0
            timeStr to count
        }
        
        val elapsedToday = endTime - startTime
        val finalTotal = totalScreenTime.coerceIn(0L, elapsedToday)
        val totalAppOpens = countMap.values.sum()
        
        return UsageStatsResult(appStats, finalTotal, unlockCount, notificationsReceived, totalAppOpens)
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        if (totalMinutes < 1) return "<1m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
