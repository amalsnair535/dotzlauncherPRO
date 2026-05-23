package com.dotz.launcherpro.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotz.launcherpro.data.*
import com.dotz.launcherpro.services.DotzNotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LauncherUiState(
    val page0Tiles: List<AppTile> = DefaultApps.page0Defaults,
    val page1Tiles: List<AppTile> = DefaultApps.page1Defaults,
    val page2Tiles: List<AppTile> = DefaultApps.page2Defaults,
    val settings: DotzSettings = DotzSettings(),
    val notificationCounts: Map<String, Int> = emptyMap(),
    val batteryLevel: Int = -1,
    val networkStatus: String = "None",
    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isSilentMode: Boolean = false,
    val isTorchOn: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isDarkModeOn: Boolean = false,
    val isDefaultLauncher: Boolean = false,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DotzPreferencesRepository(application)
    val iconCache = IconCacheManager(application)
    private val pm: PackageManager = application.packageManager
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _batteryLevel = MutableStateFlow(-1)
    private val _networkStatus = MutableStateFlow("None")
    private val _isWifiEnabled = MutableStateFlow(value = false)
    private val _isBluetoothEnabled = MutableStateFlow(value = false)
    private val _isSilentMode = MutableStateFlow(value = false)
    private val _isTorchOn = MutableStateFlow(value = false)
    private val _isAirplaneModeOn = MutableStateFlow(value = false)
    private val _isDarkModeOn = MutableStateFlow(value = false)
    private val _refreshTrigger = MutableStateFlow(value = Unit)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if ((level != -1) && (scale != -1)) {
                _batteryLevel.value = ((level * 100) / scale.toFloat()).toInt()
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    _isWifiEnabled.value = wifiManager.isWifiEnabled
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    _isAirplaneModeOn.value = intent.getBooleanExtra("state", false)
                }
            }
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            _isTorchOn.value = enabled
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { updateNetwork() }
        override fun onLost(network: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateNetwork() }

        private fun updateNetwork() {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            _networkStatus.value = when {
                caps == null -> "None"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Eth"
                else -> "Online"
            }
        }
    }

    init {
        val app = getApplication<Application>()
        
        // Register Battery Receiver
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(batteryReceiver, batteryFilter)
        }

        // Register System Toggles Receiver
        val systemFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        app.registerReceiver(systemReceiver, systemFilter)

        // Initial System States
        _isWifiEnabled.value = wifiManager.isWifiEnabled
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        _isAirplaneModeOn.value = Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        _isDarkModeOn.value = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // Torch state tracking
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) { e.printStackTrace() }

        // Register Network Callback
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        } catch (e: Exception) { e.printStackTrace() }

        // Main UI State combination
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                DotzNotificationService.notificationCounts,
                _batteryLevel,
                _networkStatus,
                _isWifiEnabled,
                _isBluetoothEnabled,
                _isSilentMode,
                _isTorchOn,
                _isAirplaneModeOn,
                _isDarkModeOn,
                _refreshTrigger
            ) { args: Array<Any> ->
                val settings = args[0] as DotzSettings
                @Suppress("UNCHECKED_CAST")
                val notifCounts = args[1] as Map<String, Int>
                val battery = args[2] as Int
                val network = args[3] as String
                val wifi = args[4] as Boolean
                val bt = args[5] as Boolean
                val silent = args[6] as Boolean
                val torch = args[7] as Boolean
                val airplane = args[8] as Boolean
                val dark = args[9] as Boolean
                // args[10] is _refreshTrigger

                val isDefault = isDefaultLauncher()

                val allTiles = buildTiles(DefaultApps.allDefaults, settings, notifCounts)

                val p0 = allTiles.take(6)
                val p1 = allTiles.drop(6).take(6)
                val p2 = if (settings.enableExtraPage) allTiles.drop(12).take(settings.extraTileCount) else emptyList()
                
                LauncherUiState(
                    page0Tiles = p0,
                    page1Tiles = p1,
                    page2Tiles = p2,
                    settings = settings,
                    notificationCounts = notifCounts,
                    batteryLevel = battery,
                    networkStatus = network,
                    isWifiEnabled = wifi,
                    isBluetoothEnabled = bt,
                    isSilentMode = silent,
                    isTorchOn = torch,
                    isAirplaneModeOn = airplane,
                    isDarkModeOn = dark,
                    isDefaultLauncher = isDefault
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        try {
            app.unregisterReceiver(batteryReceiver)
            app.unregisterReceiver(systemReceiver)
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) { e.printStackTrace() }

        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── System Toggles ────────────────────────────────────────────────────────

    fun toggleWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    fun toggleBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleSilentMode() {
        val app = getApplication<Application>()
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            return
        }

        val newMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager.RINGER_MODE_VIBRATE
        } else {
            AudioManager.RINGER_MODE_NORMAL
        }
        audioManager.ringerMode = newMode
    }

    fun toggleTorch() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, !_isTorchOn.value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun toggleAirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleDarkMode() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun openMobileDataSettings() {
        val app = getApplication<Application>()
        Log.d("DotzAction", "openMobileDataSettings triggered")
        
        // Strategy: Try the most direct intent, then fall back to the most general ones.
        // On some devices, opening a specific settings sub-page without proper permissions
        // or if it doesn't exist can cause a SecurityException or ActivityNotFoundException.
        
        val actions = listOf(
            Settings.ACTION_DATA_ROAMING_SETTINGS,
            "android.settings.DATA_ROAMING_SETTINGS",
            "android.settings.NETWORK_OPERATOR_SETTINGS",
            Settings.ACTION_WIRELESS_SETTINGS,
            Settings.ACTION_SETTINGS
        )

        for (action in actions) {
            try {
                Log.d("DotzAction", "Trying action: $action")
                val intent = Intent(action)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                Log.d("DotzAction", "Success with action: $action")
                return
            } catch (e: Exception) {
                Log.w("DotzAction", "Failed action $action: ${e.message}")
            }
        }
    }

    fun openDefaultLauncherSettings() {
        val app = getApplication<Application>()
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(fallback)
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val res = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentDefault = res?.activityInfo?.packageName
        val myPackage = getApplication<Application>().packageName
        
        return if (currentDefault == "android" || currentDefault == "com.android.settings" || currentDefault == "com.google.android.permissioncontroller") {
            // It's the system resolver, not a specific launcher
            false
        } else {
            currentDefault == myPackage
        }
    }

    // ── Logic ────────────────────────────────────────────────────────────

    fun refreshState() {
        _refreshTrigger.value = Unit
    }

    // ── App Logic ─────────────────────────────────────────────────────────────

    private fun buildTiles(
        defaults: List<AppTile>,
        settings: DotzSettings,
        notifCounts: Map<String, Int>
    ): List<AppTile> {
        return defaults.map { tile ->
            val pkg = settings.tileOverrides[tile.tileId] ?: resolvePackage(tile.packageName)
            val label = settings.tileLabels[tile.tileId] ?: tile.label
            val installed = isInstalled(pkg) || pkg == getApplication<Application>().packageName
            val count = if (settings.showNotificationDots) {
                val raw = notifCounts[pkg] ?: -1
                if (raw > 0 && settings.showNumericalCounts && DefaultApps.numericBadgePackages.contains(pkg)) {
                    raw
                } else if (raw >= 0) {
                    0
                } else {
                    -1
                }
            } else {
                -1
            }
            tile.copy(packageName = pkg, label = label, badgeCount = count, isInstalled = installed)
        }
    }

    private fun resolvePackage(preferred: String): String {
        if (isInstalled(preferred)) return preferred
        DefaultApps.packageFallbacks[preferred]?.forEach { fallback ->
            if (isInstalled(fallback)) return fallback
        }
        return preferred
    }

    private fun isInstalled(pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun onTileTapped(tile: AppTile) {
        DotzNotificationService.clearBadge(tile.packageName)
        DotzNotificationService.cancelNotificationsForPackage(tile.packageName)
    }

    fun updateTileOverride(tileId: Int, packageName: String, label: String) {
        viewModelScope.launch {
            prefs.setTileOverride(tileId, packageName, label)
        }
    }

    fun setShowNotificationDots(value: Boolean) = viewModelScope.launch {
        prefs.setShowNotificationDots(value)
    }

    fun setShowNumericalCounts(value: Boolean) = viewModelScope.launch {
        prefs.setShowNumericalCounts(value)
    }

    fun setNotificationFilterEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setNotificationFilterEnabled(value)
    }

    fun setDynamicBackgroundEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setDynamicBackgroundEnabled(value)
    }

    fun setTileOpacity(value: Float) = viewModelScope.launch {
        prefs.setTileOpacity(value)
    }

    fun setGrayscaleMode(value: Boolean) = viewModelScope.launch {
        prefs.setGrayscaleMode(value)
        iconCache.clearCache()
    }

    fun setVerticalScrolling(value: Boolean) = viewModelScope.launch {
        prefs.setVerticalScrolling(value)
    }

    fun setEnableExtraPage(value: Boolean) = viewModelScope.launch {
        prefs.setEnableExtraPage(value)
    }

    fun setExtraTileCount(value: Int) = viewModelScope.launch {
        prefs.setExtraTileCount(value)
    }

    fun setIconPackPackage(value: String?) = viewModelScope.launch {
        prefs.setIconPackPackage(value)
        iconCache.clearCache()
    }

    suspend fun exportSettings(): String {
        return prefs.exportSettings()
    }

    suspend fun importSettings(json: String): Boolean {
        return prefs.importSettings(json)
    }

    fun getInstalledApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName to (it.loadLabel(pm).toString()) }
            .distinctBy { it.first }
            .sortedBy { it.second }
            .toList()
    }

    fun getInstalledAppsForTile(tileId: Int): List<Pair<String, String>> {
        val allApps = getInstalledApps()
        
        val suggested = when (tileId) {
            0 -> filterByIntent(allApps, Intent(Intent.ACTION_DIAL)) + 
                 filterByIntent(allApps, Intent(Intent.ACTION_VIEW).apply { data = "tel:".toUri() }) +
                 filterByKeywords(allApps, listOf("phone", "dialer", "call", "contact"))
            1 -> filterByKeywords(allApps, listOf("chat", "whatsapp", "telegram", "signal", "discord", "viber", "messenger", "social", "facebook", "insta"))
            2 -> filterByIntent(allApps, Intent(Intent.ACTION_SENDTO).apply { data = "smsto:".toUri() }) +
                 filterByKeywords(allApps, listOf("messag", "sms", "mms", "text"))
            3 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) }) +
                 filterByKeywords(allApps, listOf("map", "navig", "gps", "waze", "uber", "lyft"))
            4 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }) +
                 filterByIntent(allApps, Intent("android.intent.action.MUSIC_PLAYER")) +
                 filterByKeywords(allApps, listOf("music", "audio", "player", "spotify", "sound", "radio", "podcast", "yt music", "youtube music"))
            5 -> filterByKeywords(allApps, listOf("pay", "wallet", "bank", "finance", "cash", "money", "card", "crypto", "binance", "paypal"))
            6 -> filterByIntent(allApps, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) +
                 filterByKeywords(allApps, listOf("camera", "cam", "lens"))
            7 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }) +
                 filterByKeywords(allApps, listOf("calc"))
            8 -> filterByKeywords(allApps, listOf("clock", "alarm", "timer", "watch"))
            9 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }) +
                 filterByKeywords(allApps, listOf("calen"))
            10 -> filterByKeywords(allApps, listOf("note", "keep", "memo", "todo", "sticky", "journal", "list", "writ"))
            else -> emptyList()
        }.asSequence().distinctBy { it.first }.sortedBy { it.second }.toList()

        // Combine suggested with all others
        val suggestedPackages = suggested.map { it.first }.toSet()
        val others = allApps.filter { it.first !in suggestedPackages }
        
        return suggested + others
    }

    private fun filterByIntent(apps: List<Pair<String, String>>, intent: Intent): List<Pair<String, String>> {
        val resolved = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
        return apps.filter { resolved.contains(it.first) }
    }

    private fun filterByKeywords(apps: List<Pair<String, String>>, keywords: List<String>): List<Pair<String, String>> {
        return apps.filter { (pkg, label) ->
            keywords.any { kw -> 
                label.contains(kw, ignoreCase = true) || pkg.contains(kw, ignoreCase = true)
            }
        }
    }

    fun getInstalledIconPacks(): List<Pair<String, String>> {
        val iconPacks = mutableListOf<Pair<String, String>>()
        val intent = Intent("com.novalauncher.THEME")
        val infos = pm.queryIntentActivities(intent, 0)
        for (info in infos) {
            iconPacks.add(info.activityInfo.packageName to info.loadLabel(pm).toString())
        }

        val adwIntent = Intent("org.adw.launcher.THEMES")
        val adwInfos = pm.queryIntentActivities(adwIntent, 0)
        for (info in adwInfos) {
            val pkg = info.activityInfo.packageName
            if (iconPacks.none { it.first == pkg }) {
                iconPacks.add(pkg to info.loadLabel(pm).toString())
            }
        }

        return iconPacks.sortedBy { it.second }
    }
}
