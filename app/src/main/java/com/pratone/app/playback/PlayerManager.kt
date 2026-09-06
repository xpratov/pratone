package com.pratone.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.pratone.app.domain.model.RepeatMode
import com.pratone.app.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
)

/**
 * The one gateway every part of the app (Compose UI, voice command executor) goes through to
 * control playback. Internally this is a thin, lifecycle-safe wrapper around a Media3
 * [MediaController] bound to [MusicPlaybackService] — never a direct ExoPlayer reference — so
 * commands work identically whether they come from a tapped button, the lock screen, or a
 * recognized voice command, and playback survives the controlling component being destroyed.
 */
class PlayerManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = currentQueue.firstOrNull { it.id.toString() == mediaItem?.mediaId }
            _uiState.value = _uiState.value.copy(
                currentSong = song,
                durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L,
            )
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.value = _uiState.value.copy(repeatMode = repeatMode.toDomainRepeatMode())
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(durationMs = controller?.duration?.coerceAtLeast(0) ?: 0L)
        }
    }

    fun connect(onConnected: () -> Unit = {}) {
        if (controller != null) { onConnected(); return }
        val token = SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
            onConnected()
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    /** Returns the live playback position; UI polls this on a ticker since Media3 has no position stream. */
    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        currentQueue = songs
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.artworkUri)
                        .build(),
                )
                .build()
        }
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
        _uiState.value = _uiState.value.copy(queue = songs, currentSong = songs.getOrNull(startIndex))
    }

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)
    fun stop() = controller?.stop()

    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode.toDomainRepeatMode()) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF
        }
    }

    fun hasNext(): Boolean = controller?.hasNextMediaItem() ?: false
    fun hasPrevious(): Boolean = controller?.hasPreviousMediaItem() ?: false

    private var preDuckVolume: Float = 1f

    /**
     * Temporarily lowers playback volume while the voice system is listening, mitigating the
     * "microphone hears the music playing through the speaker" problem on devices where
     * acoustic echo cancellation isn't available (see AudioEffectsHelper). Not a substitute for
     * AEC — just a pragmatic fallback that measurably reduces false wake/command misfires.
     */
    fun duckVolume() {
        val c = controller ?: return
        if (c.volume > 0.25f) preDuckVolume = c.volume
        c.volume = 0.15f
    }

    fun restoreVolume() {
        controller?.volume = preDuckVolume
    }

    companion object {
        @Volatile private var instance: PlayerManager? = null
        fun get(context: Context): PlayerManager = instance ?: synchronized(this) {
            instance ?: PlayerManager(context).also { instance = it }
        }
    }
}

private fun Int.toDomainRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    else -> RepeatMode.OFF
}
