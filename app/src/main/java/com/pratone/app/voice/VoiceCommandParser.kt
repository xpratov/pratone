package com.pratone.app.voice

/**
 * Turns raw speech-recognition text into a [VoiceCommand]. Deliberately dumb and predictable —
 * this is a fixed command grammar, not an NLU model, matching the "not a general assistant"
 * requirement. Structured so a language pack is just another map passed to [forLanguage], should
 * non-English support be added later.
 */
object VoiceCommandParser {

    // Words that carry no command meaning and can be stripped before matching.
    private val fillerWords = setOf(
        "please", "the", "music", "song", "now", "can", "you", "could", "a", "to", "this", "that",
    )

    private val commandPhrases: Map<VoiceCommand, Set<String>> = mapOf(
        VoiceCommand.PLAY to setOf("play", "start", "resume", "unpause"),
        VoiceCommand.PAUSE to setOf("pause", "stop the music", "hold"),
        VoiceCommand.NEXT to setOf("next", "skip", "next song", "forward"),
        VoiceCommand.PREVIOUS to setOf("previous", "back", "last song", "go back"),
        VoiceCommand.STOP to setOf("stop"),
        VoiceCommand.SHUFFLE to setOf("shuffle"),
        VoiceCommand.REPEAT to setOf("repeat", "loop"),
    )

    fun parse(rawText: String): VoiceCommand {
        val normalized = normalize(rawText)
        if (normalized.isBlank()) return VoiceCommand.UNKNOWN

        // Exact match against known short phrases first (handles "next song", "stop the music").
        for ((command, phrases) in commandPhrases) {
            if (normalized in phrases) return command
        }

        // Then match against the filler-stripped bag of words, keyword-first so "please play music"
        // and "start music please" both resolve correctly regardless of word order.
        val words = normalized.split(" ").filterNot { it in fillerWords }
        val stripped = words.joinToString(" ")

        for ((command, phrases) in commandPhrases) {
            if (stripped in phrases) return command
            if (phrases.any { phrase -> words.containsAll(phrase.split(" ")) }) return command
        }

        return VoiceCommand.UNKNOWN
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .trim()
        .replace(Regex("[.,!?;:]"), "")
        .replace(Regex("\\s+"), " ")
}
