package com.dotz.launcherpro.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single app tile in the launcher.
 * @param tileId Stable identifier for the tile slot (0-11)
 * @param packageName The resolved package name of the target app
 * @param label Display label shown under the icon
 * @param iconVector Fallback vector icon if package icon unavailable
 * @param badgeCount -1 = no badge, 0 = dot, >0 = count
 * @param isInstalled Whether the app is currently installed on the device
 */
data class AppTile(
    val tileId: Int,
    val packageName: String,
    val label: String,
    val iconVector: ImageVector,
    val badgeCount: Int = -1,
    val isInstalled: Boolean = true,
)

enum class TileType {
    CALL, WHATSAPP, MESSAGE, MAPS, MUSIC, PAY,
    CAMERA, CALCULATOR, CLOCK, CALENDAR, NOTES, SETTINGS
}

object DefaultApps {

    /** Page 0 — Primary / Communication */
    val page0Defaults = listOf(
        AppTile(0,  "com.google.android.dialer",        "CALL",      Icons.Default.Call),
        AppTile(1,  "com.whatsapp",                     "WHATSAPP",  Icons.AutoMirrored.Filled.Chat),
        AppTile(2,  "com.google.android.apps.messaging","MESSAGE",   Icons.AutoMirrored.Filled.Message),
        AppTile(3,  "com.google.android.apps.maps",     "MAPS",      Icons.Default.LocationOn),
        AppTile(4,  "com.spotify.music",                "MUSIC",     Icons.Default.MusicNote),
        AppTile(5,  "com.google.android.apps.walletnfcrel", "PAY",   Icons.Default.CreditCard)
    )

    /** Page 1 — Utility */
    val page1Defaults = listOf(
        AppTile(6,  "com.android.camera2",              "CAMERA",    Icons.Default.CameraAlt),
        AppTile(7,  "com.google.android.calculator",    "CALCULATOR",Icons.Default.Calculate),
        AppTile(8,  "com.google.android.deskclock",     "CLOCK",     Icons.Default.AccessTime),
        AppTile(9,  "com.google.android.calendar",      "CALENDAR",  Icons.Default.CalendarMonth),
        AppTile(10, "com.google.android.keep",          "NOTES",     Icons.Default.Lightbulb),
        AppTile(11, "com.dotz.launcherpro",             "SETTINGS",  Icons.Default.Settings)
    )

    /** Page 2 — Extra */
    val page2Defaults = listOf(
        AppTile(12, "", "APP 13", Icons.Default.Apps),
        AppTile(13, "", "APP 14", Icons.Default.Apps),
        AppTile(14, "", "APP 15", Icons.Default.Apps),
        AppTile(15, "", "APP 16", Icons.Default.Apps),
        AppTile(16, "", "APP 17", Icons.Default.Apps),
        AppTile(17, "", "APP 18", Icons.Default.Apps)
    )

    val allDefaults = page0Defaults + page1Defaults + page2Defaults

    /** Packages that should show numeric badge (calls & SMS) */
    val numericBadgePackages = setOf(
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms"
    )

    /** Common social media packages to filter out if Notification Filter is enabled */
    val distractingPackages = setOf(
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.twitter.android",
        "com.snapchat.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.reddit.frontpage",
        "com.linkedin.android",
        "com.pinterest"
    )

    /** Fallback package names for common OEM variants */
    val packageFallbacks = mapOf(
        "com.google.android.dialer"         to listOf("com.samsung.android.dialer", "com.android.dialer"),
        "com.google.android.apps.messaging" to listOf("com.samsung.android.messaging", "com.android.mms"),
        "com.android.camera2"               to listOf("com.samsung.android.camera2", "com.google.android.GoogleCamera"),
        "com.google.android.apps.walletnfcrel" to listOf("com.samsung.android.spay"),
        "com.google.android.keep"           to listOf("com.miui.notes", "com.colornote.notepad")
    )
}
