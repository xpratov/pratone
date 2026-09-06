package com.pratone.app.voice

sealed interface VoiceState {
    data object Idle : VoiceState
    data object ListeningForWakeWord : VoiceState
    data object WakeWordDetected : VoiceState
    data object ListeningForCommand : VoiceState
    data object ProcessingCommand : VoiceState
    data class CommandExecuted(val command: VoiceCommand) : VoiceState
    data class Error(val message: String) : VoiceState
}

/** Short strings for the on-screen voice status line. Keep these terse per the product's UX rule. */
fun VoiceState.statusText(): String = when (this) {
    VoiceState.Idle -> "Voice control off"
    VoiceState.ListeningForWakeWord -> "Listening for \"Hey Pratone\""
    VoiceState.WakeWordDetected -> "I'm listening..."
    VoiceState.ListeningForCommand -> "I'm listening..."
    VoiceState.ProcessingCommand -> "..."
    is VoiceState.CommandExecuted -> when (command) {
        VoiceCommand.PLAY -> "Playing"
        VoiceCommand.PAUSE -> "Paused"
        VoiceCommand.NEXT -> "Next song"
        VoiceCommand.PREVIOUS -> "Previous song"
        VoiceCommand.STOP -> "Stopped"
        VoiceCommand.SHUFFLE -> "Shuffle toggled"
        VoiceCommand.REPEAT -> "Repeat changed"
        VoiceCommand.UNKNOWN -> "Unknown command"
    }
    is VoiceState.Error -> message
}
