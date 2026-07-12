package com.dotz.launcherpro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import com.dotz.launcherpro.dataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

data class LauncherProfile(
    val id: String,
    val name: String,
    val tileOverrides: Map<Int, String>,
    val tileLabels: Map<Int, String>,
    val tileOrder: List<Int>,
    val grayscaleMode: Boolean,
    val notificationFilterEnabled: Boolean,
    val layoutStyle: String,
    val extraTileCount: Int = 6,
    val enableExtraPage: Boolean = true
)

data class DotzSettings(
    val showNotificationDots: Boolean = true,
    val showNumericalCounts: Boolean = true,
    val grayscaleMode: Boolean = false,
    val iconPackPackage: String? = null,
    val themeId: String = "default",
    val useAdaptiveTheme: Boolean = false,
    val notificationFilterEnabled: Boolean = false,
    val isLightMode: Boolean = false,
    val verticalScrolling: Boolean = false,
    val enableExtraPage: Boolean = false,
    val extraTileCount: Int = 6,
    val showWeatherInfo: Boolean = false,
    val showMindfulUsage: Boolean = true,
    val showWallpaper: Boolean = false,
    val enableTimeline: Boolean = false,
    val homeHeaderMode: String = "toggles", // "toggles", "music", or "stats"
    val tileTransparency: Float = 1.0f,
    val layoutStyle: String = "classic",
    /** JSON-serialized map of tileId -> packageName overrides */
    val tileOverrides: Map<Int, String> = emptyMap(),
    val tileLabels: Map<Int, String> = emptyMap(),
    val focusStreak: Int = 0,
    val lastUsedDate: Long = 0,
    val focusTimeToday: Long = 0, // in milliseconds
    val lastWeatherFetchTime: Long = 0,
    val isPremium: Boolean = false,
    val premiumExpiry: Long = 0, // 0 means permanent or no trial
    val useCircadianTheming: Boolean = false,
    val autoGrayscale: Boolean = false,
    val useLiquidGlass: Boolean = false,
    val enableAppDrawer: Boolean = false,
    val appDrawerEnabledAt: Long = 0,
    val appDrawerOpenCount: Int = 0,
    val lastAppDrawerOpenDate: Long = 0,
    val hasAcceptedAppDisclosure: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    val lastSponsoredShowTime: Long = 0,
    val tileOrder: List<Int> = (0..17).toList(),
    val activeProfileId: String = "default",
    val profiles: List<LauncherProfile> = emptyList(),
    val ultraFocusEndTime: Long = 0,
    val emergencyDrawerOpens: Int = 0,
)

object PrefsKeys {
    val SHOW_NOTIFICATION_DOTS  = booleanPreferencesKey("show_notification_dots")
    val SHOW_NUMERICAL_COUNTS   = booleanPreferencesKey("show_numerical_counts")
    val GRAYSCALE_MODE          = booleanPreferencesKey("grayscale_mode")
    val ICON_PACK_PACKAGE       = stringPreferencesKey("icon_pack_package")
    val THEME_ID                = stringPreferencesKey("theme_id")
    val USE_ADAPTIVE_THEME      = booleanPreferencesKey("use_adaptive_theme")
    val NOTIFICATION_FILTER_ENABLED = booleanPreferencesKey("notification_filter_enabled")
    val IS_LIGHT_MODE           = booleanPreferencesKey("is_light_mode")
    val VERTICAL_SCROLLING      = booleanPreferencesKey("vertical_scrolling")
    val ENABLE_EXTRA_PAGE       = booleanPreferencesKey("enable_extra_page")
    val EXTRA_TILE_COUNT        = intPreferencesKey("extra_tile_count")
    val SHOW_WEATHER_INFO       = booleanPreferencesKey("show_weather_info")
    val SHOW_WALLPAPER          = booleanPreferencesKey("show_wallpaper")
    val ENABLE_TIMELINE         = booleanPreferencesKey("enable_timeline")
    val HOME_HEADER_MODE        = stringPreferencesKey("home_header_mode")
    val TILE_TRANSPARENCY       = floatPreferencesKey("tile_transparency")
    val LAYOUT_STYLE            = stringPreferencesKey("layout_style")
    // Tile overrides stored as individual keys: tile_override_0, tile_override_1, …
    fun tileOverride(id: Int) = stringPreferencesKey("tile_override_$id")
    fun tileLabel(id: Int)    = stringPreferencesKey("tile_label_$id")

    val FOCUS_STREAK = intPreferencesKey("focus_streak")
    val LAST_USED_DATE = longPreferencesKey("last_used_date")
    val FOCUS_TIME_TODAY = longPreferencesKey("focus_time_today")
    val LAST_WEATHER_FETCH_TIME = longPreferencesKey("last_weather_fetch_time")
    val SHOW_MINDFUL_USAGE = booleanPreferencesKey("show_mindful_usage")
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry")
    val USE_CIRCADIAN_THEMING = booleanPreferencesKey("use_circadian_theming")
    val AUTO_GRAYSCALE        = booleanPreferencesKey("auto_grayscale")
    val USE_LIQUID_GLASS      = booleanPreferencesKey("use_liquid_glass")
    val ENABLE_APP_DRAWER      = booleanPreferencesKey("enable_app_drawer")
    val APP_DRAWER_ENABLED_AT  = longPreferencesKey("app_drawer_enabled_at")
    val APP_DRAWER_OPEN_COUNT  = intPreferencesKey("app_drawer_open_count")
    val LAST_APP_DRAWER_OPEN_DATE = longPreferencesKey("last_app_drawer_open_date")
    val HAS_ACCEPTED_APP_DISCLOSURE = booleanPreferencesKey("has_accepted_app_disclosure")
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    val LAST_SPONSORED_SHOW_TIME = longPreferencesKey("last_sponsored_show_time")
    val TILE_ORDER              = stringPreferencesKey("tile_order")
    val ACTIVE_PROFILE_ID       = stringPreferencesKey("active_profile_id")
    val PROFILES_JSON           = stringPreferencesKey("profiles_json")
    val ULTRA_FOCUS_END_TIME    = longPreferencesKey("ultra_focus_end_time")
    val EMERGENCY_DRAWER_OPENS  = intPreferencesKey("emergency_drawer_opens")
}

class DotzPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<DotzSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val overrides = (0..17).mapNotNull { id ->
                prefs[PrefsKeys.tileOverride(id)]?.let { id to it }
            }.toMap()

            val labels = (0..17).mapNotNull { id ->
                prefs[PrefsKeys.tileLabel(id)]?.let { id to it }
            }.toMap()

            val orderString = prefs[PrefsKeys.TILE_ORDER]
            val order = if (orderString != null) {
                try {
                    orderString.split(",").map { it.toInt() }
                } catch (e: Exception) { (0..17).toList() }
            } else {
                (0..17).toList()
            }

            val profilesJson = prefs[PrefsKeys.PROFILES_JSON]
            val profilesList = if (profilesJson != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<LauncherProfile>>() {}.type
                    Gson().fromJson<List<LauncherProfile>>(profilesJson, type)
                } catch (e: Exception) { emptyList<LauncherProfile>() }
            } else {
                emptyList<LauncherProfile>()
            }

            DotzSettings(
                showNotificationDots = prefs[PrefsKeys.SHOW_NOTIFICATION_DOTS] ?: true,
                showNumericalCounts  = prefs[PrefsKeys.SHOW_NUMERICAL_COUNTS]  ?: true,
                grayscaleMode        = prefs[PrefsKeys.GRAYSCALE_MODE]          ?: false,
                iconPackPackage      = prefs[PrefsKeys.ICON_PACK_PACKAGE],
                themeId              = prefs[PrefsKeys.THEME_ID]                ?: "default",
                useAdaptiveTheme     = prefs[PrefsKeys.USE_ADAPTIVE_THEME]      ?: false,
                notificationFilterEnabled = prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] ?: false,
                isLightMode          = prefs[PrefsKeys.IS_LIGHT_MODE]          ?: false,
                verticalScrolling    = prefs[PrefsKeys.VERTICAL_SCROLLING]      ?: false,
                enableExtraPage      = prefs[PrefsKeys.ENABLE_EXTRA_PAGE]        ?: false,
                extraTileCount       = prefs[PrefsKeys.EXTRA_TILE_COUNT]         ?: 6,
                showWeatherInfo      = prefs[PrefsKeys.SHOW_WEATHER_INFO]        ?: false,
                showMindfulUsage     = prefs[PrefsKeys.SHOW_MINDFUL_USAGE]       ?: true,
                showWallpaper        = prefs[PrefsKeys.SHOW_WALLPAPER]           ?: false,
                enableTimeline       = prefs[PrefsKeys.ENABLE_TIMELINE]          ?: false,
                homeHeaderMode       = prefs[PrefsKeys.HOME_HEADER_MODE]         ?: "toggles",
                tileTransparency     = prefs[PrefsKeys.TILE_TRANSPARENCY]        ?: 1.0f,
                layoutStyle          = prefs[PrefsKeys.LAYOUT_STYLE]             ?: "classic",
                tileOverrides        = overrides,
                tileLabels           = labels,
                focusStreak          = prefs[PrefsKeys.FOCUS_STREAK] ?: 0,
                lastUsedDate         = prefs[PrefsKeys.LAST_USED_DATE] ?: 0,
                focusTimeToday       = prefs[PrefsKeys.FOCUS_TIME_TODAY] ?: 0,
                lastWeatherFetchTime = prefs[PrefsKeys.LAST_WEATHER_FETCH_TIME] ?: 0,
                isPremium            = prefs[PrefsKeys.IS_PREMIUM] ?: false,
                premiumExpiry        = prefs[PrefsKeys.PREMIUM_EXPIRY] ?: 0,
                useCircadianTheming  = prefs[PrefsKeys.USE_CIRCADIAN_THEMING] ?: false,
                autoGrayscale        = prefs[PrefsKeys.AUTO_GRAYSCALE]        ?: false,
                useLiquidGlass       = prefs[PrefsKeys.USE_LIQUID_GLASS]       ?: false,
                enableAppDrawer      = prefs[PrefsKeys.ENABLE_APP_DRAWER]      ?: false,
                appDrawerEnabledAt   = prefs[PrefsKeys.APP_DRAWER_ENABLED_AT]   ?: 0L,
                appDrawerOpenCount   = prefs[PrefsKeys.APP_DRAWER_OPEN_COUNT]   ?: 0,
                lastAppDrawerOpenDate = prefs[PrefsKeys.LAST_APP_DRAWER_OPEN_DATE] ?: 0L,
                hasAcceptedAppDisclosure = prefs[PrefsKeys.HAS_ACCEPTED_APP_DISCLOSURE] ?: false,
                hasSeenOnboarding = prefs[PrefsKeys.HAS_SEEN_ONBOARDING] ?: false,
                lastSponsoredShowTime = prefs[PrefsKeys.LAST_SPONSORED_SHOW_TIME] ?: 0,
                tileOrder            = order,
                activeProfileId      = prefs[PrefsKeys.ACTIVE_PROFILE_ID] ?: "default",
                profiles             = profilesList,
                ultraFocusEndTime    = prefs[PrefsKeys.ULTRA_FOCUS_END_TIME] ?: 0L,
                emergencyDrawerOpens = prefs[PrefsKeys.EMERGENCY_DRAWER_OPENS] ?: 0
            )
        }

    suspend fun setShowNotificationDots(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_NOTIFICATION_DOTS] = value }
    }

    suspend fun setShowNumericalCounts(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_NUMERICAL_COUNTS] = value }
    }

    suspend fun setGrayscaleMode(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.GRAYSCALE_MODE] = value }
    }

    suspend fun setIconPackPackage(value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(PrefsKeys.ICON_PACK_PACKAGE)
            else prefs[PrefsKeys.ICON_PACK_PACKAGE] = value
        }
    }

    suspend fun setThemeId(value: String) {
        context.dataStore.edit { it[PrefsKeys.THEME_ID] = value }
    }

    suspend fun setUseAdaptiveTheme(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_ADAPTIVE_THEME] = value }
    }

    suspend fun setNotificationFilterEnabled(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.NOTIFICATION_FILTER_ENABLED] = value }
    }

    suspend fun setIsLightMode(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.IS_LIGHT_MODE] = value }
    }

    suspend fun setVerticalScrolling(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.VERTICAL_SCROLLING] = value }
    }

    suspend fun setEnableExtraPage(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ENABLE_EXTRA_PAGE] = value }
    }

    suspend fun setExtraTileCount(value: Int) {
        context.dataStore.edit { it[PrefsKeys.EXTRA_TILE_COUNT] = value.coerceIn(1, 6) }
    }

    suspend fun setShowWeatherInfo(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_WEATHER_INFO] = value }
    }

    suspend fun setLastWeatherFetchTime(value: Long) {
        context.dataStore.edit { it[PrefsKeys.LAST_WEATHER_FETCH_TIME] = value }
    }

    suspend fun setShowWallpaper(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_WALLPAPER] = value }
    }

    suspend fun setEnableTimeline(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ENABLE_TIMELINE] = value }
    }

    suspend fun setHomeHeaderMode(value: String) {
        context.dataStore.edit { it[PrefsKeys.HOME_HEADER_MODE] = value }
    }

    suspend fun setShowMindfulUsage(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_MINDFUL_USAGE] = value }
    }

    suspend fun setPremium(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.IS_PREMIUM] = value }
    }

    suspend fun setPremiumExpiry(value: Long) {
        context.dataStore.edit { it[PrefsKeys.PREMIUM_EXPIRY] = value }
    }

    suspend fun setUseCircadianTheming(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_CIRCADIAN_THEMING] = value }
    }

    suspend fun setAutoGrayscale(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.AUTO_GRAYSCALE] = value }
    }

    suspend fun setUseLiquidGlass(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.USE_LIQUID_GLASS] = value }
    }

    suspend fun setEnableAppDrawer(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.ENABLE_APP_DRAWER] = value
            if (value) {
                prefs[PrefsKeys.APP_DRAWER_ENABLED_AT] = System.currentTimeMillis()
            } else {
                prefs[PrefsKeys.APP_DRAWER_ENABLED_AT] = 0L
            }
        }
    }

    suspend fun incrementAppDrawerOpenCount() {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val today = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)

        context.dataStore.edit { prefs ->
            val lastDate = prefs[PrefsKeys.LAST_APP_DRAWER_OPEN_DATE] ?: 0L
            val lastCalendar = java.util.Calendar.getInstance().apply { timeInMillis = lastDate }
            val lastDay = lastCalendar.get(java.util.Calendar.DAY_OF_YEAR)
            val lastYear = lastCalendar.get(java.util.Calendar.YEAR)

            if (today != lastDay || year != lastYear) {
                prefs[PrefsKeys.APP_DRAWER_OPEN_COUNT] = 1
            } else {
                val current = prefs[PrefsKeys.APP_DRAWER_OPEN_COUNT] ?: 0
                prefs[PrefsKeys.APP_DRAWER_OPEN_COUNT] = current + 1
            }
            prefs[PrefsKeys.LAST_APP_DRAWER_OPEN_DATE] = now
        }
    }

    suspend fun setHasAcceptedAppDisclosure(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.HAS_ACCEPTED_APP_DISCLOSURE] = value }
    }

    suspend fun setHasSeenOnboarding(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.HAS_SEEN_ONBOARDING] = value }
    }

    suspend fun setLastSponsoredShowTime(value: Long) {
        context.dataStore.edit { it[PrefsKeys.LAST_SPONSORED_SHOW_TIME] = value }
    }

    suspend fun setTileTransparency(value: Float) {
        context.dataStore.edit { it[PrefsKeys.TILE_TRANSPARENCY] = value }
    }

    suspend fun setLayoutStyle(value: String) {
        context.dataStore.edit { it[PrefsKeys.LAYOUT_STYLE] = value }
    }

    suspend fun setTileOverride(tileId: Int, packageName: String, label: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.tileOverride(tileId)] = packageName
            prefs[PrefsKeys.tileLabel(tileId)]    = label
        }
    }

    suspend fun setTileOrder(order: List<Int>) {
        context.dataStore.edit { it[PrefsKeys.TILE_ORDER] = order.joinToString(",") }
    }

    suspend fun createProfile(name: String): String {
        val current = settingsFlow.first()
        val newId = UUID.randomUUID().toString()
        val newProfile = LauncherProfile(
            id = newId,
            name = name,
            tileOverrides = current.tileOverrides,
            tileLabels = current.tileLabels,
            tileOrder = current.tileOrder,
            grayscaleMode = current.grayscaleMode,
            notificationFilterEnabled = current.notificationFilterEnabled,
            layoutStyle = current.layoutStyle,
            extraTileCount = current.extraTileCount,
            enableExtraPage = current.enableExtraPage
        )
        val newList = current.profiles + newProfile
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.PROFILES_JSON] = Gson().toJson(newList)
        }
        return newId
    }

    suspend fun deleteProfile(id: String) {
        val current = settingsFlow.first()
        if (current.activeProfileId == id) {
            switchProfile("default")
        }
        val newList = current.profiles.filter { it.id != id }
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.PROFILES_JSON] = Gson().toJson(newList)
        }
    }

    suspend fun switchProfile(targetId: String) {
        val current = settingsFlow.first()
        val oldId = current.activeProfileId
        
        context.dataStore.edit { prefs ->
            // 1. Prepare profiles list (ensure old profile is updated)
            val updatedProfiles = current.profiles.toMutableList()
            
            // Capture current state
            val currentState = LauncherProfile(
                id = oldId,
                name = if (oldId == "default") "Default" else (current.profiles.find { it.id == oldId }?.name ?: "Unknown"),
                tileOverrides = current.tileOverrides,
                tileLabels = current.tileLabels,
                tileOrder = current.tileOrder,
                grayscaleMode = current.grayscaleMode,
                notificationFilterEnabled = current.notificationFilterEnabled,
                layoutStyle = current.layoutStyle,
                extraTileCount = current.extraTileCount,
                enableExtraPage = current.enableExtraPage
            )

            // Update or add the old profile state to the list
            val oldIndex = updatedProfiles.indexOfFirst { it.id == oldId }
            if (oldIndex != -1) {
                updatedProfiles[oldIndex] = currentState
            } else {
                updatedProfiles.add(currentState)
            }

            // 2. Load the target profile values into top-level keys
            val targetProfile = updatedProfiles.find { it.id == targetId }
            
            if (targetProfile != null) {
                // Apply target profile values to individual keys
                prefs[PrefsKeys.GRAYSCALE_MODE] = targetProfile.grayscaleMode
                prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] = targetProfile.notificationFilterEnabled
                prefs[PrefsKeys.LAYOUT_STYLE] = targetProfile.layoutStyle
                prefs[PrefsKeys.TILE_ORDER] = targetProfile.tileOrder.joinToString(",")
                prefs[PrefsKeys.EXTRA_TILE_COUNT] = targetProfile.extraTileCount
                prefs[PrefsKeys.ENABLE_EXTRA_PAGE] = targetProfile.enableExtraPage
                
                // Clear and apply overrides
                (0..17).forEach { id ->
                    prefs.remove(PrefsKeys.tileOverride(id))
                    prefs.remove(PrefsKeys.tileLabel(id))
                }
                targetProfile.tileOverrides.forEach { (id, pkg) ->
                    prefs[PrefsKeys.tileOverride(id)] = pkg
                }
                targetProfile.tileLabels.forEach { (id, label) ->
                    prefs[PrefsKeys.tileLabel(id)] = label
                }
                
                // Update active ID
                prefs[PrefsKeys.ACTIVE_PROFILE_ID] = targetId
            }
            
            // Save the updated list (Always including all profiles including default)
            prefs[PrefsKeys.PROFILES_JSON] = Gson().toJson(updatedProfiles)
        }
    }

    suspend fun updateFocusStats(streak: Int, lastDate: Long, timeToday: Long, resetDrawerCount: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.FOCUS_STREAK] = streak
            prefs[PrefsKeys.LAST_USED_DATE] = lastDate
            prefs[PrefsKeys.FOCUS_TIME_TODAY] = timeToday
            if (resetDrawerCount) {
                prefs[PrefsKeys.APP_DRAWER_OPEN_COUNT] = 0
                prefs[PrefsKeys.EMERGENCY_DRAWER_OPENS] = 0
            }
        }
    }

    suspend fun incrementEmergencyDrawerOpens() {
        context.dataStore.edit { prefs ->
            val current = prefs[PrefsKeys.EMERGENCY_DRAWER_OPENS] ?: 0
            prefs[PrefsKeys.EMERGENCY_DRAWER_OPENS] = current + 1
        }
    }

    suspend fun setUltraFocusEndTime(time: Long) {
        context.dataStore.edit { it[PrefsKeys.ULTRA_FOCUS_END_TIME] = time }
    }

    /** Read a single tile label override (used for display in app grid) */
    fun tileLabelFlow(tileId: Int): Flow<String?> =
        context.dataStore.data.map { it[PrefsKeys.tileLabel(tileId)] }

    suspend fun exportSettings(): String {
        val settings = settingsFlow.first()
        return Gson().toJson(settings)
    }

    suspend fun importSettings(json: String): Boolean {
        return try {
            val settings = Gson().fromJson(json, DotzSettings::class.java)
            context.dataStore.edit { prefs ->
                prefs[PrefsKeys.SHOW_NOTIFICATION_DOTS] = settings.showNotificationDots
                prefs[PrefsKeys.SHOW_NUMERICAL_COUNTS] = settings.showNumericalCounts
                prefs[PrefsKeys.GRAYSCALE_MODE] = settings.grayscaleMode
                if (settings.iconPackPackage != null) {
                    prefs[PrefsKeys.ICON_PACK_PACKAGE] = settings.iconPackPackage
                } else {
                    prefs.remove(PrefsKeys.ICON_PACK_PACKAGE)
                }
                prefs[PrefsKeys.THEME_ID] = settings.themeId
                prefs[PrefsKeys.USE_ADAPTIVE_THEME] = settings.useAdaptiveTheme
                prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] = settings.notificationFilterEnabled
                prefs[PrefsKeys.IS_LIGHT_MODE] = settings.isLightMode
                prefs[PrefsKeys.VERTICAL_SCROLLING] = settings.verticalScrolling
                prefs[PrefsKeys.ENABLE_EXTRA_PAGE] = settings.enableExtraPage
                prefs[PrefsKeys.EXTRA_TILE_COUNT] = settings.extraTileCount
                prefs[PrefsKeys.SHOW_WEATHER_INFO] = settings.showWeatherInfo
                prefs[PrefsKeys.SHOW_WALLPAPER] = settings.showWallpaper
                prefs[PrefsKeys.ENABLE_TIMELINE] = settings.enableTimeline
                prefs[PrefsKeys.HOME_HEADER_MODE] = settings.homeHeaderMode
                prefs[PrefsKeys.TILE_TRANSPARENCY] = settings.tileTransparency
                prefs[PrefsKeys.LAYOUT_STYLE] = settings.layoutStyle
                prefs[PrefsKeys.USE_CIRCADIAN_THEMING] = settings.useCircadianTheming
                prefs[PrefsKeys.AUTO_GRAYSCALE] = settings.autoGrayscale
                prefs[PrefsKeys.USE_LIQUID_GLASS] = settings.useLiquidGlass
                prefs[PrefsKeys.ENABLE_APP_DRAWER] = settings.enableAppDrawer
                prefs[PrefsKeys.TILE_ORDER] = settings.tileOrder.joinToString(",")
                prefs[PrefsKeys.ACTIVE_PROFILE_ID] = settings.activeProfileId
                prefs[PrefsKeys.PROFILES_JSON] = Gson().toJson(settings.profiles)

                // Clear and re-apply overrides
                (0..17).forEach { id ->
                    prefs.remove(PrefsKeys.tileOverride(id))
                    prefs.remove(PrefsKeys.tileLabel(id))
                }
                settings.tileOverrides.forEach { (id, pkg) ->
                    prefs[PrefsKeys.tileOverride(id)] = pkg
                }
                settings.tileLabels.forEach { (id, label) ->
                    prefs[PrefsKeys.tileLabel(id)] = label
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
