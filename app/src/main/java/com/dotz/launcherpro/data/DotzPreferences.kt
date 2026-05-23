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
    val tileOpacity: Float = 1.0f,
    val grayscaleMode: Boolean = false,
    val iconPackPackage: String? = null,
    val themeId: String = "default",
    val useAdaptiveTheme: Boolean = false,
    val notificationFilterEnabled: Boolean = false,
    val dynamicBackgroundEnabled: Boolean = false,
    val verticalScrolling: Boolean = false,
    val enableExtraPage: Boolean = false,
    /** JSON-serialized map of tileId -> packageName overrides */
    val tileOverrides: Map<Int, String> = emptyMap(),
    val tileLabels: Map<Int, String> = emptyMap(),
)

object PrefsKeys {
    val SHOW_NOTIFICATION_DOTS  = booleanPreferencesKey("show_notification_dots")
    val SHOW_NUMERICAL_COUNTS   = booleanPreferencesKey("show_numerical_counts")
    val TILE_OPACITY            = floatPreferencesKey("tile_opacity")
    val GRAYSCALE_MODE          = booleanPreferencesKey("grayscale_mode")
    val ICON_PACK_PACKAGE       = stringPreferencesKey("icon_pack_package")
    val THEME_ID                = stringPreferencesKey("theme_id")
    val USE_ADAPTIVE_THEME      = booleanPreferencesKey("use_adaptive_theme")
    val NOTIFICATION_FILTER_ENABLED = booleanPreferencesKey("notification_filter_enabled")
    val DYNAMIC_BACKGROUND_ENABLED = booleanPreferencesKey("dynamic_background_enabled")
    val VERTICAL_SCROLLING      = booleanPreferencesKey("vertical_scrolling")
    val ENABLE_EXTRA_PAGE       = booleanPreferencesKey("enable_extra_page")
    // Tile overrides stored as individual keys: tile_override_0, tile_override_1, …
    fun tileOverride(id: Int) = stringPreferencesKey("tile_override_$id")
    fun tileLabel(id: Int)    = stringPreferencesKey("tile_label_$id")
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
                tileOpacity          = prefs[PrefsKeys.TILE_OPACITY]            ?: 1.0f,
                grayscaleMode        = prefs[PrefsKeys.GRAYSCALE_MODE]          ?: false,
                iconPackPackage      = prefs[PrefsKeys.ICON_PACK_PACKAGE],
                themeId              = prefs[PrefsKeys.THEME_ID]                ?: "default",
                useAdaptiveTheme     = prefs[PrefsKeys.USE_ADAPTIVE_THEME]      ?: false,
                notificationFilterEnabled = prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] ?: false,
                dynamicBackgroundEnabled = prefs[PrefsKeys.DYNAMIC_BACKGROUND_ENABLED] ?: false,
                verticalScrolling    = prefs[PrefsKeys.VERTICAL_SCROLLING]      ?: false,
                enableExtraPage      = prefs[PrefsKeys.ENABLE_EXTRA_PAGE]        ?: false,
                tileOverrides        = overrides,
                tileLabels           = labels
            )
        }

    suspend fun setShowNotificationDots(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_NOTIFICATION_DOTS] = value }
    }

    suspend fun setShowNumericalCounts(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.SHOW_NUMERICAL_COUNTS] = value }
    }

    suspend fun setTileOpacity(value: Float) {
        context.dataStore.edit { it[PrefsKeys.TILE_OPACITY] = value.coerceIn(0.6f, 1.0f) }
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

    suspend fun setDynamicBackgroundEnabled(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.DYNAMIC_BACKGROUND_ENABLED] = value }
    }

    suspend fun setVerticalScrolling(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.VERTICAL_SCROLLING] = value }
    }

    suspend fun setEnableExtraPage(value: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ENABLE_EXTRA_PAGE] = value }
    }

    suspend fun setTileOverride(tileId: Int, packageName: String, label: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefsKeys.tileOverride(tileId)] = packageName
            prefs[PrefsKeys.tileLabel(tileId)]    = label
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
                prefs[PrefsKeys.TILE_OPACITY] = settings.tileOpacity
                prefs[PrefsKeys.GRAYSCALE_MODE] = settings.grayscaleMode
                if (settings.iconPackPackage != null) {
                    prefs[PrefsKeys.ICON_PACK_PACKAGE] = settings.iconPackPackage
                } else {
                    prefs.remove(PrefsKeys.ICON_PACK_PACKAGE)
                }
                prefs[PrefsKeys.THEME_ID] = settings.themeId
                prefs[PrefsKeys.USE_ADAPTIVE_THEME] = settings.useAdaptiveTheme
                prefs[PrefsKeys.NOTIFICATION_FILTER_ENABLED] = settings.notificationFilterEnabled
                prefs[PrefsKeys.VERTICAL_SCROLLING] = settings.verticalScrolling
                prefs[PrefsKeys.ENABLE_EXTRA_PAGE] = settings.enableExtraPage

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
