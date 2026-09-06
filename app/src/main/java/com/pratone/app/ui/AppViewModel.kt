package com.pratone.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pratone.app.data.local.MediaPermissions
import com.pratone.app.data.repository.MusicRepository
import com.pratone.app.domain.model.Song
import com.pratone.app.playback.PlayerManager
import com.pratone.app.playback.PlaybackUiState
import com.pratone.app.voice.VoiceController
import com.pratone.app.voice.VoiceListeningService
import com.pratone.app.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide ViewModel. There's a single instance shared by every screen (via the Activity's
 * ViewModelProvider), which is deliberate: playback and voice state must be identical no matter
 * which screen is currently visible, and re-instantiating [PlayerManager]/repositories per screen
 * would fight the Media3 controller's own singleton connection lifecycle.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val playerManager: PlayerManager = PlayerManager.get(application)

    private val voiceController = VoiceController.get(application, playerManager)
    val voiceState: StateFlow<VoiceState> = voiceController.state
    val activeVoiceEngineName: String get() = voiceController.activeEngineName

    private val _voiceEnabled = MutableStateFlow(false)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _porcupineAccessKey = MutableStateFlow("")
    val porcupineAccessKey: StateFlow<String> = _porcupineAccessKey.asStateFlow()

    val hasReadAudioPermission = MutableStateFlow(MediaPermissions.hasReadAudio(application))
    val hasRecordAudioPermission = MutableStateFlow(MediaPermissions.hasRecordAudio(application))

    val playbackState: StateFlow<PlaybackUiState> = playerManager.uiState

    val favoriteSongIds: StateFlow<Set<Long>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        playerManager.connect()
    }

    fun onPermissionsUpdated() {
        val app = getApplication<Application>()
        hasReadAudioPermission.value = MediaPermissions.hasReadAudio(app)
        hasRecordAudioPermission.value = MediaPermissions.hasRecordAudio(app)
        if (hasReadAudioPermission.value) rescanLibrary()
    }

    fun rescanLibrary() {
        viewModelScope.launch { repository.rescan() }
    }

    fun playSong(song: Song, fromList: List<Song> = repository.songs.value) {
        val index = fromList.indexOf(song).coerceAtLeast(0)
        playerManager.playQueue(fromList, index)
        viewModelScope.launch { repository.markPlayed(song.id) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { repository.toggleFavorite(songId) }
    }

    fun setPorcupineAccessKey(key: String) {
        _porcupineAccessKey.value = key
        if (_voiceEnabled.value) restartVoiceService()
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _voiceEnabled.value = enabled
        val app = getApplication<Application>()
        if (enabled && hasRecordAudioPermission.value) {
            VoiceListeningService.start(app, _porcupineAccessKey.value)
        } else {
            VoiceListeningService.stop(app)
        }
    }

    private fun restartVoiceService() {
        val app = getApplication<Application>()
        VoiceListeningService.stop(app)
        VoiceListeningService.start(app, _porcupineAccessKey.value)
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }
}
