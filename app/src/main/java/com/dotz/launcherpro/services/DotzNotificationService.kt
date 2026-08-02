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
    val postTime: Long,
    val canReply: Boolean = false
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
    private var isBatchingEnabled = false
    private var lastBatchTime = 0L
    private var batchIntervalHours = 4

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

        fun sendReply(notificationKey: String, message: String) {
            val sbn = instance?.activeNotifications?.find { it.key == notificationKey } ?: return
            val action = sbn.notification.actions?.find { it.remoteInputs?.isNotEmpty() == true } ?: return
            val remoteInput = action.remoteInputs!![0]
            
            val intent = Intent().apply {
                val bundle = android.os.Bundle()
                bundle.putCharSequence(remoteInput.resultKey, message)
                android.app.RemoteInput.addResultsToIntent(action.remoteInputs, this, bundle)
            }
            
            try {
                action.actionIntent.send(instance, 0, intent)
                instance?.cancelNotification(notificationKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val dotzApp = application as com.dotz.launcherpro.DotzApp
        prefsRepository = dotzApp.prefsRepository
        serviceScope.launch {
            prefsRepository.settingsFlow.collectLatest { settings ->
                isFilterEnabled = settings.notificationFilterEnabled
                isBatchingEnabled = settings.batchNotifications
                lastBatchTime = settings.lastBatchTime
                batchIntervalHours = settings.notificationBatchInterval
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
            
            val now = System.currentTimeMillis()
            val intervalMillis = batchIntervalHours * 60 * 60 * 1000L
            val shouldHold = isBatchingEnabled && (now - lastBatchTime < intervalMillis)

            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    val pkg = sbn.packageName
                    
                    if (shouldHold) {
                        // If batching is on and we haven't reached the interval, "block" everything
                        // except maybe Dialer/System which we can choose to bypass
                        if (pkg != "com.android.dialer" && pkg != "com.android.server.telecom") {
                           blocked++
                           return@forEach
                        }
                    }

                    val isDistracting = DefaultApps.distractingPackages.any { pkg.contains(it, ignoreCase = true) }
                    
                    if (isFilterEnabled && isDistracting) {
                        blocked++
                        return@forEach
                    }

                    val extras = sbn.notification.extras
                    
                    // Try to get internal count (e.g., missed calls count or message count)
                    // android.number is the standard extra for this.
                    val internalCount = extras.getInt("android.number", 0)
                    val countToAdd = if (internalCount > 0) internalCount else 1
                    
                    counts[pkg] = (counts[pkg] ?: 0) + countToAdd

                    val title = extras.getCharSequence("android.title")?.toString()
                    val text = extras.getCharSequence("android.text")?.toString()
                    
                    val canReply = sbn.notification.actions?.any { action ->
                        action.remoteInputs?.isNotEmpty() == true
                    } ?: false

                    items.add(NotificationItem(
                        key = sbn.key,
                        packageName = pkg,
                        title = title,
                        text = text,
                        postTime = sbn.postTime,
                        canReply = canReply
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

    /** Reply to a notification */
    fun sendReply(notificationKey: String, message: String) {
        val sbn = activeNotifications?.find { it.key == notificationKey } ?: return
        val action = sbn.notification.actions?.find { it.remoteInputs?.isNotEmpty() == true } ?: return
        val remoteInput = action.remoteInputs!![0]
        
        val intent = Intent().apply {
            val bundle = android.os.Bundle()
            bundle.putCharSequence(remoteInput.resultKey, message)
            android.app.RemoteInput.addResultsToIntent(action.remoteInputs, this, bundle)
        }
        
        try {
            action.actionIntent.send(this, 0, intent)
            // Auto-cancel/Dismiss notification after reply?
            cancelNotification(notificationKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
