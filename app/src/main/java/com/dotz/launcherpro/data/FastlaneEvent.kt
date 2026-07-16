package com.dotz.launcherpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FastlaneType {
    APP_OPEN, CALL_MISSED, CALL_INCOMING, CALL_OUTGOING,
    SMS_RECEIVED, MUSIC_TRACK, CAMERA_PHOTO, CAMERA_SCREENSHOT,
    WIFI_STATUS, BT_STATUS, BATTERY_STATUS, NOTIF_HISTORY, CALENDAR,
    SPONSORED, FOCUS_SUMMARY
}

@Entity(tableName = "fastlane_history")
data class FastlaneEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: FastlaneType,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String? = null,
    val extraData: String? = null, // JSON for Quick Actions
    val isDismissed: Boolean = false
)
