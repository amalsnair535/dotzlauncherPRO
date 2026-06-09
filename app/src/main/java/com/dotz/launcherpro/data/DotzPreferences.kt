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

data class DotzSettings(
    val showNotificationDots: Boolean = true,
    val showNumericalCounts: Boolean = true,
    val grayscaleMode: Boolean = false,
    val iconPackPackage: String? = null,
    val themeId: String = "default",
    val useAdaptiveTheme: Boolean = false,
    val notificationFilterEnabled: Boolean = false,
    val isLightMode: Boolean = false,
    val is24HourFormat: Boolean = true,
    val verticalScrolling: Boolean = false,
    val enableExtraPage: Boolean = false,
    val extraTileCount: Int = 6,
    val showWeatherInfo: Boolean = true,
    val showWallpaper: Boolean = true,
    val enableDashboard: Boolean = false,
    val tileTransparency: Float = 1.0f,
    val layoutStyle: String = "classic",
    /** JSON-serialized map of tileId -> packageName overrides */
    val tileOverrides: Map<Int, String> = emptyMap(),
    val tileLabels: Map<Int, String> = emptyMap(),
    val focusStreak: Int = 0,
    val lastUsedDate: Long = 0,
    val focusTimeToday: Long = 0, // in milliseconds
    val isPremium: Boolean = false,
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
    val IS_24_HOUR_FORMAT       = booleanPreferencesKey("is_24_hour_format")
    val VERTICAL_SCROLLING      = booleanPreferencesKey("vertical_scrolling")
    val ENABLE_EXTRA_PAGE       = booleanPreferencesKey("enable_extra_page")
    val EXTRA_TILE_COUNT        = intPreferencesKey("extra_tile_count")
    val SHOW_WEATHER_INFO       = booleanPreferencesKey("show_weather_info")
    val SHOW_WALLPAPER          = booleanPreferencesKey("show_wallpaper")
    val ENABLE_DASHBOARD        = booleanPreferencesKey("enable_dashboard")
    val TILE_TRANSPARENCY       = floatPreferencesKey("tile_transparency")
    val LAYOUT_STYLE            = stringPreferencesKey("layout_style")
    // Tile overrides stored as individual keys: tile_override_0, tile_override_1, …
    fun tileOverride(id: Int) = stringPreferencesKey("tile_override_$id")
    fun tileLabel(id: Int)    = stringPreferencesKey("tile_label_$id")

    val FOCUS_STREAK = intPreferencesKey("focus_streak")
    val LAST_USED_DATE = longPreferencesKey("last_used_date")
    val FOCUS_TIME_TODAY = longPreferencesKey("focus_time_today")
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
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

            DotzSettings(
                showNotificationDots = prefs[PrefsKeys.SHOW_NOTIFICATION_DOTS] ?: true,
                showNumericalCounts  = prefs[PrefsKeys.SHOW_NUMERICAL_COUNTS]  ?: true,
                grayscaleMode        = prefs[PrefsKeys.GRAYSCALE_MODE]          ?: false,
                iconPackPackage      = prefs[PrefsKeys.ICON_PACK_PACKAGE],
                themeId              = prefs[PrefsKeys.THEME_ID]                ?: "default",
                useAdaptiveTheme     = prefs[PrefsKeys.USE_ADAPTIVE_THEME]      ?: false,
                notificationFilterEnabled = prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] ?: false,
                isLightMode          = prefs[PrefsKeys.IS_LIGHT_MODE]          ?: false,
                is24HourFormat       = prefs[PrefsKeys.IS_24_HOUR_FORMAT]       ?: true,
                verticalScrolling    = prefs[PrefsKeys.VERTICAL_SCROLLING]      ?: false,
                enableExtraPage      = prefs[PrefsKeys.ENABLE_EXTRA_PAGE]        ?: false,
                extraTileCount       = prefs[PrefsKeys.EXTRA_TILE_COUNT]         ?: 6,
                showWeatherInfo      = prefs[PrefsKeys.SHOW_WEATHER_INFO]        ?: true,
                showWallpaper        = prefs[PrefsKeys.SHOW_WALLPAPER]           ?: true,
                enableDashboard      = prefs[PrefsKeys.ENABLE_DASHBOARD]         ?: false,
                tileTransparency     = prefs[PrefsKeys.TILE_TRANSPARENCY]        ?: 1.0f,
                layoutStyle          = prefs[PrefsKeys.LAYOUT_STYLE]             ?: "classic",
                tileOverrides        = overrides,
                tileLabels           = labels,
                focusStreak          = prefs[PrefsKeys.FOCUS_STREAK] ?: 0,
                lastUsedDate         = prefs[PrefsKeys.LAST_USED_DATE] ?: 0,
                focusTimeToday       = prefs[PrefsKeys.FOCUS_TIME_TODAY] ?: 0,
                isPremium            = prefs[PrefsKeys.IS_PREMIUM] ?: false
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

    suspend fun setIs24HourFormat(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.IS_24_HOUR_FORMAT] = value }
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

    suspend fun setShowWallpaper(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_WALLPAPER] = value }
    }

    suspend fun setEnableDashboard(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ENABLE_DASHBOARD] = value }
    }

    suspend fun setPremium(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.IS_PREMIUM] = value }
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

    suspend fun updateFocusStats(streak: Int, lastDate: Long, timeToday: Long) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.FOCUS_STREAK] = streak
            prefs[PrefsKeys.LAST_USED_DATE] = lastDate
            prefs[PrefsKeys.FOCUS_TIME_TODAY] = timeToday
        }
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
                prefs[PrefsKeys.IS_24_HOUR_FORMAT] = settings.is24HourFormat
                prefs[PrefsKeys.VERTICAL_SCROLLING] = settings.verticalScrolling
                prefs[PrefsKeys.ENABLE_EXTRA_PAGE] = settings.enableExtraPage
                prefs[PrefsKeys.EXTRA_TILE_COUNT] = settings.extraTileCount
                prefs[PrefsKeys.SHOW_WEATHER_INFO] = settings.showWeatherInfo
                prefs[PrefsKeys.SHOW_WALLPAPER] = settings.showWallpaper
                prefs[PrefsKeys.ENABLE_DASHBOARD] = settings.enableDashboard
                prefs[PrefsKeys.TILE_TRANSPARENCY] = settings.tileTransparency
                prefs[PrefsKeys.LAYOUT_STYLE] = settings.layoutStyle

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
