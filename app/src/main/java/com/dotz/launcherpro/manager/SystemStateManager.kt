package com.dotz.launcherpro.manager

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
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
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemStateManager(private val app: Application) {

    private val wifiManager = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _batteryLevel = MutableStateFlow(-1)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _isMobileDataEnabled = MutableStateFlow(isMobileDataEnabled())
    val isMobileDataEnabled = _isMobileDataEnabled.asStateFlow()

    private val _networkStatus = MutableStateFlow("None")
    val networkStatus = _networkStatus.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(wifiManager.isWifiEnabled)
    val isWifiEnabled = _isWifiEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _ringerMode = MutableStateFlow(audioManager.ringerMode)
    val ringerMode = _ringerMode.asStateFlow()

    private val _isSilentMode = MutableStateFlow(audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL)
    val isSilentMode = _isSilentMode.asStateFlow()

    private val _isAirplaneModeOn = MutableStateFlow(Settings.Global.getInt(app.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0)
    val isAirplaneModeOn = _isAirplaneModeOn.asStateFlow()

    private val _isDarkModeOn = MutableStateFlow((app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
    val isDarkModeOn = _isDarkModeOn.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn = _isTorchOn.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                _batteryLevel.value = ((level * 100) / scale.toFloat()).toInt()
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> _isWifiEnabled.value = wifiManager.isWifiEnabled
                BluetoothAdapter.ACTION_STATE_CHANGED -> _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = audioManager.ringerMode
                    _ringerMode.value = mode
                    _isSilentMode.value = mode != AudioManager.RINGER_MODE_NORMAL
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> _isAirplaneModeOn.value = intent.getBooleanExtra("state", false)
            }
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            _isTorchOn.value = enabled
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { updateNetwork() }
        override fun onLost(network: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateNetwork() }
    }

    private fun updateNetwork() {
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        _isMobileDataEnabled.value = isMobileDataEnabled()
        _networkStatus.value = when {
            caps == null -> "None"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Eth"
            else -> "Online"
        }
    }

    fun start() {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(batteryReceiver, batteryFilter)
        }

        val systemFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        app.registerReceiver(systemReceiver, systemFilter)

        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) { e.printStackTrace() }

        try {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
        
        updateNetwork()
    }

    fun stop() {
        try {
            app.unregisterReceiver(batteryReceiver)
            app.unregisterReceiver(systemReceiver)
            cameraManager.unregisterTorchCallback(torchCallback)
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun isMobileDataEnabled(): Boolean {
        return try {
            val method = connectivityManager.javaClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            method.invoke(connectivityManager) as Boolean
        } catch (e: Exception) {
            val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
    }
}
