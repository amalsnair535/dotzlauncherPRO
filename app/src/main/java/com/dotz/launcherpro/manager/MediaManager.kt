package com.dotz.launcherpro.manager

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.dotz.launcherpro.services.DotzNotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaManager(private val app: Application) {

    private val mediaSessionManager = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()

    private val _nowPlaying = MutableStateFlow<Triple<String, String, String>>(Triple("Not Playing", "", ""))
    val nowPlaying = _nowPlaying.asStateFlow()

    private val _nowPlayingPackage = MutableStateFlow<String?>(null)
    val nowPlayingPackage = _nowPlayingPackage.asStateFlow()

    private val _playbackState = MutableStateFlow<Triple<Boolean, Long, Long>>(Triple(false, 0L, 0L))
    val playbackState = _playbackState.asStateFlow()

    private val _lastPositionUpdateTime = MutableStateFlow<Long>(0L)
    val lastPositionUpdateTime = _lastPositionUpdateTime.asStateFlow()

    private fun createCallback(controller: MediaController) = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            android.util.Log.d("MediaManager", "Metadata changed: ${controller.packageName}")
            // Refresh sessions safely
            refresh()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            android.util.Log.d("MediaManager", "Playback state changed: ${controller.packageName} -> ${state?.state}")
            refresh()
        }

        override fun onSessionDestroyed() {
            android.util.Log.d("MediaManager", "Session destroyed: ${controller.packageName}")
            refresh()
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private fun getComponentName(): ComponentName {
        return ComponentName(app, DotzNotificationService::class.java)
    }

    fun start() {
        val componentName = getComponentName()
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            refresh()
        } catch (_: Exception) {}
    }

    fun stop() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        clearAllCallbacks()
    }

    /** 
     * Manually trigger a refresh of media sessions. 
     * Handles SecurityException if permission is not yet granted.
     */
    fun refresh() {
        try {
            val sessions = mediaSessionManager.getActiveSessions(getComponentName())
            updateActiveController(sessions)
        } catch (e: SecurityException) {
            android.util.Log.w("MediaManager", "Permission to control media not granted yet.")
        } catch (e: Exception) {
            android.util.Log.e("MediaManager", "Failed to refresh media sessions", e)
        }
    }

    private fun clearAllCallbacks() {
        controllerCallbacks.forEach { (_, pair) ->
            pair.first.unregisterCallback(pair.second)
        }
        controllerCallbacks.clear()
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        val currentControllers = controllers ?: emptyList()
        val currentTokens = currentControllers.map { it.sessionToken }.toSet()

        // 1. Sync callbacks
        val iterator = controllerCallbacks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentTokens.contains(entry.key)) {
                entry.value.first.unregisterCallback(entry.value.second)
                iterator.remove()
            }
        }

        currentControllers.forEach { controller ->
            if (!controllerCallbacks.containsKey(controller.sessionToken)) {
                val callback = createCallback(controller)
                controller.registerCallback(callback)
                controllerCallbacks[controller.sessionToken] = Pair(controller, callback)
            }
        }

        // 2. Advanced Selection Logic
        val playingController = currentControllers.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val bufferingController = currentControllers.find { it.playbackState?.state == PlaybackState.STATE_BUFFERING }
        
        // Prioritize Playing > Buffering > Last Known > First Available
        activeController = playingController ?: bufferingController ?: activeController?.let { last ->
            currentControllers.find { it.sessionToken == last.sessionToken }
        } ?: currentControllers.firstOrNull()

        // 3. Update States
        if (activeController != null) {
            updateMediaInfo(activeController?.metadata, activeController?.playbackState)
        } else {
            _nowPlaying.value = Triple("Not Playing", "", "")
            _nowPlayingPackage.value = null
            _playbackState.value = Triple(false, 0L, 0L)
        }
    }

    private fun updateMediaInfo(metadata: MediaMetadata?, state: PlaybackState?) {
        val currentActive = activeController ?: return
        
        // Drip-feed metadata: check multiple common keys
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?: currentActive.packageName.substringAfterLast('.').uppercase()
            
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
            ?: "Ready to play"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        
        // Ensure state change is emitted even if metadata is the same (force a refresh)
        _nowPlaying.value = Triple(title, artist, album)
        _nowPlayingPackage.value = currentActive.packageName
        
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        _playbackState.value = Triple(isPlaying, position, duration)
        _lastPositionUpdateTime.value = System.currentTimeMillis()
    }

    fun playPause() {
        activeController?.let {
            if (it.playbackState?.state == PlaybackState.STATE_PLAYING) {
                it.transportControls.pause()
            } else {
                it.transportControls.play()
            }
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }
}
