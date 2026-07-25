package com.dotz.launcherpro.manager

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.dotz.launcherpro.services.DotzNotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaManager(private val app: Application) {

    private val mediaSessionManager = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeController: MediaController? = null

    private val _nowPlaying = MutableStateFlow<Triple<String, String, String>>(Triple("Not Playing", "", ""))
    val nowPlaying = _nowPlaying.asStateFlow()

    private val _nowPlayingPackage = MutableStateFlow<String?>(null)
    val nowPlayingPackage = _nowPlayingPackage.asStateFlow()

    private val _playbackState = MutableStateFlow<Triple<Boolean, Long, Long>>(Triple(false, 0L, 0L))
    val playbackState = _playbackState.asStateFlow()

    private val _lastPositionUpdateTime = MutableStateFlow<Long>(0L)
    val lastPositionUpdateTime = _lastPositionUpdateTime.asStateFlow()

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

    fun start() {
        val componentName = ComponentName(app, DotzNotificationService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            updateActiveController(mediaSessionManager.getActiveSessions(componentName))
        } catch (_: Exception) {}
    }

    fun stop() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        activeController?.unregisterCallback(mediaCallback)
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        android.util.Log.d("MediaManager", "Updating active controller. Total sessions: ${controllers?.size ?: 0}")
        activeController?.unregisterCallback(mediaCallback)
        activeController = controllers?.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()
        
        activeController?.let {
            android.util.Log.d("MediaManager", "Selected controller: ${it.packageName}")
        }
        
        activeController?.registerCallback(mediaCallback)
        updateMediaInfo(activeController?.metadata, activeController?.playbackState)
    }

    private fun updateMediaInfo(metadata: MediaMetadata?, state: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) // Fallback to artist if no title? No, maybe not.
            ?: if (activeController != null) {
                activeController?.packageName?.substringAfterLast('.')?.uppercase() ?: "ACTIVE SESSION"
            } else "Not Playing"
            
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
            ?: if (activeController != null) "Ready to play" else "Play something to see info"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        
        _nowPlaying.value = Triple(title, artist, album)
        _nowPlayingPackage.value = activeController?.packageName
        
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
