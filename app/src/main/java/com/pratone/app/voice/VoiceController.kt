package com.pratone.app.voice

import android.content.Context
import com.pratone.app.playback.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates: WakeWordDetector -> (short pause) -> CommandSpeechRecognizer -> VoiceCommandParser
 * -> PlayerManager, matching the architecture in the spec. This class owns no UI and no audio
 * device details itself — it composes the pieces that do.
 */
class VoiceController(
    private val context: Context,
    private val playerManager: PlayerManager,
    porcupineAccessKey: String = "",
) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var porcupine = PorcupineWakeWordDetector(context, porcupineAccessKey)
    private val fallback = SpeechRecognizerWakeWordDetector(context)
    private val commandRecognizer = CommandSpeechRecognizer(context)

    /** Allows Settings to update the key without recreating the whole controller/service. */
    fun updatePorcupineAccessKey(key: String) {
        porcupine.stop()
        porcupine = PorcupineWakeWordDetector(context, key)
    }

    /** Which engine is actually active, exposed so Settings can show it truthfully. */
    val activeEngineName: String
        get() = if (porcupine.isAvailable()) "Porcupine (on-device)" else "Basic (SpeechRecognizer fallback)"

    private val wakeWordDetector: WakeWordDetector
        get() = if (porcupine.isAvailable()) porcupine else fallback

    private var listening = false

    fun start() {
        if (listening) return
        listening = true
        _state.value = VoiceState.ListeningForWakeWord
        if (AudioEffectsHelper.shouldDuckAsFallback()) {
            // Only duck while actively listening for the wake word/command, not during idle playback.
        }
        wakeWordDetector.start(
            onWakeWordDetected = ::onWakeWordDetected,
            onError = { err -> _state.value = VoiceState.Error(err.message ?: "Voice error") },
        )
    }

    fun stop() {
        listening = false
        wakeWordDetector.stop()
        commandRecognizer.cancel()
        playerManager.restoreVolume()
        _state.value = VoiceState.Idle
    }

    private fun onWakeWordDetected() {
        if (!listening) return
        _state.value = VoiceState.WakeWordDetected
        wakeWordDetector.stop() // free the mic before opening a full recognition session
        if (AudioEffectsHelper.shouldDuckAsFallback()) playerManager.duckVolume()

        _state.value = VoiceState.ListeningForCommand
        commandRecognizer.listenOnce(
            onResult = { text -> handleRecognizedText(text) },
            onError = {
                _state.value = VoiceState.Error("I couldn't hear you")
                resumeListeningForWakeWord()
            },
        )
    }

    private fun handleRecognizedText(text: String?) {
        _state.value = VoiceState.ProcessingCommand
        playerManager.restoreVolume()

        val command = text?.let { VoiceCommandParser.parse(it) } ?: VoiceCommand.UNKNOWN
        if (command != VoiceCommand.UNKNOWN) {
            executeCommand(command)
        }
        _state.value = VoiceState.CommandExecuted(command)
        resumeListeningForWakeWord()
    }

    private fun executeCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.PLAY -> playerManager.play()
            VoiceCommand.PAUSE -> playerManager.pause()
            VoiceCommand.NEXT -> playerManager.next()
            VoiceCommand.PREVIOUS -> playerManager.previous()
            VoiceCommand.STOP -> playerManager.stop()
            VoiceCommand.SHUFFLE -> playerManager.setShuffle(!playerManager.uiState.value.shuffleEnabled)
            VoiceCommand.REPEAT -> playerManager.cycleRepeatMode()
            VoiceCommand.UNKNOWN -> Unit
        }
    }

    private fun resumeListeningForWakeWord() {
        if (!listening) return
        _state.value = VoiceState.ListeningForWakeWord
        wakeWordDetector.start(
            onWakeWordDetected = ::onWakeWordDetected,
            onError = { err -> _state.value = VoiceState.Error(err.message ?: "Voice error") },
        )
    }

    companion object {
        // A process-wide singleton, mirroring PlayerManager: the UI (running in the main
        // activity/process) and VoiceListeningService both need to observe/drive the exact same
        // controller instance and its VoiceState, not independent copies of it.
        @Volatile private var instance: VoiceController? = null

        fun get(context: Context, playerManager: PlayerManager): VoiceController = instance ?: synchronized(this) {
            instance ?: VoiceController(context.applicationContext, playerManager).also { instance = it }
        }
    }
}
