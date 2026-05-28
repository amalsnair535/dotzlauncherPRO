package com.dotz.launcherpro.services

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dotz.launcherpro.data.DefaultApps
import com.dotz.launcherpro.data.DotzPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long
)

/**
 * Tracks active notifications and exposes them as a StateFlow.
 * The companion object acts as a singleton store accessible from
 * the ViewModel without requiring a bound service connection.
 */
class DotzNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefsRepository: DotzPreferencesRepository
    private var isFilterEnabled = false

    companion object {
        /** Map of packageName → notification count (0 means "dot only") */
        private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

        /** List of active notification items for Dashboard */
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

        /** Count of distracting notifications blocked today */
        private val _blockedCount = MutableStateFlow(0)
        val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

        /** Call this from the launcher to clear badge when tile is tapped */
        fun clearBadge(packageName: String) {
            val current = _notificationCounts.value.toMutableMap()
            current.remove(packageName)
            _notificationCounts.value = current
        }

        private var instance: DotzNotificationService? = null
        fun isConnected() = instance != null

        /** Dismiss all notifications for a package (best-effort) */
        fun cancelNotificationsForPackage(packageName: String) {
            instance?.activeNotifications
                ?.filter { it.packageName == packageName }
                ?.forEach { sbn ->
                    try {
                        instance?.cancelNotification(sbn.key)
                    } catch (_: Exception) {}
                }
        }

        fun clearAllNotifications() {
            instance?.cancelAllNotifications()
            _notifications.value = emptyList()
            _notificationCounts.value = emptyMap()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsRepository = DotzPreferencesRepository(this)
        serviceScope.launch {
            prefsRepository.settingsFlow.collectLatest { settings ->
                isFilterEnabled = settings.notificationFilterEnabled
                rebuildCounts()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        rebuildCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        rebuildCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        rebuildCounts()
    }

    private fun rebuildCounts() {
        try {
            val counts = mutableMapOf<String, Int>()
            val items = mutableListOf<NotificationItem>()
            var blocked = 0

            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    val pkg = sbn.packageName
                    if (isFilterEnabled && DefaultApps.distractingPackages.contains(pkg)) {
                        blocked++
                        return@forEach
                    }
                    counts[pkg] = (counts[pkg] ?: 0) + 1

                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence("android.title")?.toString()
                    val text = extras.getCharSequence("android.text")?.toString()
                    
                    items.add(NotificationItem(
                        key = sbn.key,
                        packageName = pkg,
                        title = title,
                        text = text,
                        postTime = sbn.postTime
                    ))
                }
            }
            _notificationCounts.value = counts
            _notifications.value = items.sortedByDescending { it.postTime }
            _blockedCount.value = blocked
        } catch (_: Exception) {
            // Service not fully connected yet
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
