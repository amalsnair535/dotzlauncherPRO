package com.dotz.launcherpro.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotz.launcherpro.data.*
import com.dotz.launcherpro.services.DotzNotificationService
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
    val weatherTemp: String? = null,
    val weatherCondition: String? = null,
    val activeNotifications: List<com.dotz.launcherpro.services.NotificationItem> = emptyList(),
    val blockedNotificationsCount: Int = 0,
    val nowPlayingTitle: String = "Not Playing",
    val nowPlayingArtist: String = "",
    val nowPlayingAlbum: String = "",
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0,
    val playbackDuration: Long = 0,
    val aiResponse: String? = null,
    val isAiLoading: Boolean = false,
    val focusTimeToday: String = "0h 0m",
    val focusTimeMillis: Long = 0,
    val focusStreak: Int = 0,
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String? = null,
    val updateApkUrl: String? = null,
    val isCheckingForUpdate: Boolean = false,
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
    private val mediaSessionManager = application.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo(metadata, activeController?.playbackState)
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo(activeController?.metadata, state)
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

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
    private val _weatherTemp = MutableStateFlow<String?>(null)
    private val _weatherCondition = MutableStateFlow<String?>(null)
    private val _nowPlaying = MutableStateFlow<Triple<String, String, String>>(Triple("Not Playing", "", ""))
    private val _playbackState = MutableStateFlow<Triple<Boolean, Long, Long>>(Triple(false, 0L, 0L))
    private val _aiResponse = MutableStateFlow<String?>(null)
    private val _isAiLoading = MutableStateFlow(false)
    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _updateAvailable = MutableStateFlow(false)
    private val _latestVersion = MutableStateFlow<String?>(null)
    private val _updateUrl = MutableStateFlow<String?>(null)
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

    private var sessionStartTime = System.currentTimeMillis()

    init {
        val app = getApplication<Application>()
        
        // Update streak and reset focus time if it's a new day
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val now = System.currentTimeMillis()
            val lastDate = settings.lastUsedDate
            
            val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val calendarLast = java.util.Calendar.getInstance().apply { timeInMillis = lastDate }
            
            val isSameDay = calendarNow.get(java.util.Calendar.DAY_OF_YEAR) == calendarLast.get(java.util.Calendar.DAY_OF_YEAR) &&
                           calendarNow.get(java.util.Calendar.YEAR) == calendarLast.get(java.util.Calendar.YEAR)
            
            val isNextDay = !isSameDay && (now - lastDate < 48 * 60 * 60 * 1000) // roughly next day check
            
            val newStreak = if (isNextDay) settings.focusStreak + 1 else if (isSameDay) settings.focusStreak else 1
            val newFocusTime = if (isSameDay) settings.focusTimeToday else 0L
            
            prefs.updateFocusStats(newStreak, now, newFocusTime)
        }

        // Periodic update of focus time
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000) // Update every minute
                updateSessionTime()
            }
        }
        
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

        fetchWeather()
        
        // Register Media Session Listener
        val componentName = ComponentName(app, DotzNotificationService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            updateActiveController(mediaSessionManager.getActiveSessions(componentName))
        } catch (e: Exception) { e.printStackTrace() }

        // Periodic playback position and session check
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val current = _playbackState.value
                if (current.first) {
                    val newPos = activeController?.playbackState?.position ?: current.second
                    _playbackState.value = Triple(current.first, newPos, current.third)
                }

                // If no active controller, or current one is not playing, check for other playing sessions
                if (activeController == null || !current.first) {
                    val app = getApplication<Application>()
                    val componentName = ComponentName(app, DotzNotificationService::class.java)
                    try {
                        val sessions = mediaSessionManager.getActiveSessions(componentName)
                        if (sessions.isNotEmpty()) {
                            val playingSession = sessions.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                            if (playingSession != null && playingSession != activeController) {
                                updateActiveController(sessions)
                            } else if (activeController == null) {
                                updateActiveController(sessions)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Listen to notifications
        viewModelScope.launch {
            DotzNotificationService.notifications.collect {
                refreshState()
            }
        }

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
                _weatherTemp,
                _weatherCondition,
                DotzNotificationService.notifications,
                DotzNotificationService.blockedCount,
                _nowPlaying,
                _playbackState,
                _aiResponse,
                _isAiLoading,
                _isCheckingUpdate,
                _updateAvailable,
                _latestVersion,
                _updateUrl,
                _refreshTrigger
            ) { args: Array<Any?> ->
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
                val temp = args[10] as String?
                val condition = args[11] as String?
                @Suppress("UNCHECKED_CAST")
                val notifications = args[12] as List<com.dotz.launcherpro.services.NotificationItem>
                val blocked = args[13] as Int
                @Suppress("UNCHECKED_CAST")
                val nowPlaying = args[14] as Triple<String, String, String>
                @Suppress("UNCHECKED_CAST")
                val playback = args[15] as Triple<Boolean, Long, Long>
                val aiResp = args[16] as String?
                val aiLoading = args[17] as Boolean
                val checkingUpdate = args[18] as Boolean
                val updateAvail = args[19] as Boolean
                val latestVer = args[20] as String?
                val updateUrl = args[21] as String?
                // args[22] is _refreshTrigger

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
                    isDefaultLauncher = isDefault,
                    weatherTemp = temp,
                    weatherCondition = condition,
                    activeNotifications = notifications,
                    blockedNotificationsCount = blocked,
                    nowPlayingTitle = nowPlaying.first,
                    nowPlayingArtist = nowPlaying.second,
                    nowPlayingAlbum = nowPlaying.third,
                    isPlaying = playback.first,
                    playbackPosition = playback.second,
                    playbackDuration = playback.third,
                    aiResponse = aiResp,
                    isAiLoading = aiLoading,
                    focusTimeToday = formatDuration(settings.focusTimeToday + (System.currentTimeMillis() - sessionStartTime)),
                    focusTimeMillis = settings.focusTimeToday + (System.currentTimeMillis() - sessionStartTime),
                    focusStreak = settings.focusStreak,
                    isCheckingForUpdate = checkingUpdate,
                    isUpdateAvailable = updateAvail,
                    latestVersionName = latestVer,
                    updateApkUrl = updateUrl
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/amalsnair535/dotzlauncherPRO/main/version.json")
                        .build()
                    client.newCall(request).execute().use { it.body?.string() }
                }

                if (response != null) {
                    val json = Gson().fromJson(response, JsonObject::class.java)
                    val remoteVersionCode = json.get("versionCode").asInt
                    val remoteVersionName = json.get("versionName").asString
                    val apkUrl = json.get("apkUrl").asString

                    val packageInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }

                    if (remoteVersionCode > currentVersionCode) {
                        _updateAvailable.value = true
                        _latestVersion.value = remoteVersionName
                        _updateUrl.value = apkUrl
                    } else {
                        _updateAvailable.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun downloadUpdate(url: String) {
        val app = getApplication<Application>()
        try {
            // Remove existing file if it exists to avoid conflicts
            val destinationFile = java.io.File(app.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "dotz_update.apk")
            if (destinationFile.exists()) destinationFile.delete()

            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle("Dotz Launcher Update")
                .setDescription("Downloading version 5.2.1...")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = app.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val downloadId = dm.enqueue(request)

            // Register receiver to open installer when finished
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(destinationFile)
                        app.unregisterReceiver(this)
                    }
                }
            }
            app.registerReceiver(
                onComplete, 
                IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_EXPORTED else 0
            )

            Toast.makeText(app, "Update download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        }
    }

    private fun installApk(file: java.io.File) {
        val app = getApplication<Application>()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }

    private fun updateSessionTime() {
        val now = System.currentTimeMillis()
        val duration = now - sessionStartTime
        sessionStartTime = now
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            prefs.updateFocusStats(settings.focusStreak, now, settings.focusTimeToday + duration)
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(mediaCallback)
        // Prefer the one that is currently playing
        activeController = controllers?.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()
        activeController?.registerCallback(mediaCallback)
        updateMediaInfo(activeController?.metadata, activeController?.playbackState)
    }

    private fun updateMediaInfo(metadata: MediaMetadata?, state: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?: if (activeController != null) {
                activeController?.packageName?.substringAfterLast('.')?.uppercase() ?: "ACTIVE SESSION"
            } else "Not Playing"
            
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: if (activeController != null) "Ready to play" else "Play something to see info"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        
        _nowPlaying.value = Triple(title, artist, album)
        
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        
        _playbackState.value = Triple(isPlaying, position, duration)
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        try {
            app.unregisterReceiver(batteryReceiver)
            app.unregisterReceiver(systemReceiver)
            cameraManager.unregisterTorchCallback(torchCallback)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            activeController?.unregisterCallback(mediaCallback)
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

    // ── Media Controls ────────────────────────────────────────────────────────

    fun mediaPlayPause() {
        if (_playbackState.value.first) activeController?.transportControls?.pause()
        else activeController?.transportControls?.play()
    }

    fun mediaSkipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun mediaSkipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    // ── DOTZ AI (Cloud Powered) ──────────────────────────────────────────

    private val client = OkHttpClient()
    private val CLOUDFLARE_WORKER_URL = "https://dotzlauncher.amalsnair535.workers.dev"

    fun askAi(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = "Thinking..."
            try {
                val responseText = withContext(Dispatchers.IO) {
                    val requestBody = Gson().toJson(mapOf("prompt" to prompt))
                        .toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(CLOUDFLARE_WORKER_URL)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        val body = response.body?.string() ?: throw Exception("Empty body")
                        val json = Gson().fromJson(body, JsonObject::class.java)
                        json.get("text").asString
                    }
                }
                _aiResponse.value = responseText
            } catch (e: Exception) {
                _aiResponse.value = "Error: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAi() {
        _aiResponse.value = null
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

    fun openWeatherApp() {
        val app = getApplication<Application>()
        val intents = listOf(
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("dynact://weather") }, // Google Weather
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("googleweather://") },
            Intent().apply { setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.staticpages.Paths") },
            pm.getLaunchIntentForPackage("com.google.android.apps.magellan"),
            pm.getLaunchIntentForPackage("com.accuweather.android"),
            pm.getLaunchIntentForPackage("com.weather.Weather"),
            pm.getLaunchIntentForPackage("com.samsung.android.weather"),
            pm.getLaunchIntentForPackage("com.miui.weather2"),
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://www.google.com/search?q=weather") } // Fallback to browser
        )

        for (intent in intents) {
            try {
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            try {
                // For a production app, we'd use FusedLocationProvider. 
                // For now, let's use a default or try to get last known location.
                // Using a simple URL for a common city or hardcoded lat/lon if location permission not yet granted.
                // We'll use Open-Meteo with a default location (e.g., London) if we can't get one.
                val lat = 51.5074
                val lon = 0.1278
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                
                // Simple network fetch in a coroutine
                val result = java.net.URL(url).readText()
                val json = Gson().fromJson(result, JsonObject::class.java)
                val current = json.getAsJsonObject("current_weather")
                val temp = current.get("temperature").getAsDouble()
                val code = current.get("weathercode").getAsInt()
                
                _weatherTemp.value = "${temp.toInt()}°C"
                _weatherCondition.value = translateWeatherCode(code)
            } catch (e: Exception) {
                Log.e("DotzWeather", "Failed to fetch weather", e)
                // Default fallback
                _weatherTemp.value = "28°C"
                _weatherCondition.value = "Cloudy"
            }
        }
    }

    private fun translateWeatherCode(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Mainly Clear"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snowy"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Cloudy"
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
        val app = getApplication<Application>()
        val componentName = ComponentName(app, DotzNotificationService::class.java)
        try {
            updateActiveController(mediaSessionManager.getActiveSessions(componentName))
        } catch (_: Exception) {}
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

    fun setShowWeatherInfo(value: Boolean) = viewModelScope.launch {
        prefs.setShowWeatherInfo(value)
    }

    fun setEnableDashboard(value: Boolean) = viewModelScope.launch {
        prefs.setEnableDashboard(value)
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
        
        val filtered = when (tileId) {
            0 -> filterByIntent(allApps, Intent(Intent.ACTION_DIAL)) + 
                 filterByIntent(allApps, Intent(Intent.ACTION_VIEW).apply { data = "tel:".toUri() }) +
                 filterByKeywords(allApps, listOf("phone", "dialer", "call", "contact"))
            1 -> filterByKeywords(allApps, listOf("chat", "whatsapp", "telegram", "signal", "discord", "viber", "messenger", "social", "facebook", "insta", "whatsapp"))
            2 -> filterByIntent(allApps, Intent(Intent.ACTION_SENDTO).apply { data = "smsto:".toUri() }) +
                 filterByKeywords(allApps, listOf("messag", "sms", "mms", "text"))
            3 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) }) +
                 filterByKeywords(allApps, listOf("map", "navig", "gps", "waze", "uber", "lyft", "traffic", "location"))
            4 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }) +
                 filterByIntent(allApps, Intent("android.intent.action.MUSIC_PLAYER")) +
                 filterByKeywords(allApps, listOf("music", "audio", "player", "spotify", "sound", "radio", "podcast", "yt music", "youtube music", "wynk", "jio saavn", "gaana"))
            5 -> filterByKeywords(allApps, listOf("pay", "wallet", "bank", "finance", "cash", "money", "card", "crypto", "binance", "paypal", "gpay", "phonepe", "phonepay", "paytm", "bhim", "yono", "hdfc", "icici", "sbi", "axis", "kotak", "pnb", "bob", "canara"))
            6 -> filterByIntent(allApps, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) +
                 filterByKeywords(allApps, listOf("camera", "cam", "lens", "gallery", "photo"))
            7 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }) +
                 filterByKeywords(allApps, listOf("calc"))
            8 -> filterByKeywords(allApps, listOf("clock", "alarm", "timer", "watch"))
            9 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }) +
                 filterByKeywords(allApps, listOf("calen"))
            10 -> filterByKeywords(allApps, listOf("note", "keep", "memo", "todo", "sticky", "journal", "list", "writ"))
            11 -> filterByKeywords(allApps, listOf("settings", "config", "manage", "setup", "launcher", "dotz"))
            else -> return allApps // Tiles 12+ (Extra Page) can select any app
        }.asSequence().distinctBy { it.first }.sortedBy { it.second }.toList()

        // If for some reason the filtered list is empty (e.g., no matching app), 
        // allow all apps so the user isn't stuck with an unassignable tile.
        return if (filtered.isEmpty()) allApps else filtered
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
