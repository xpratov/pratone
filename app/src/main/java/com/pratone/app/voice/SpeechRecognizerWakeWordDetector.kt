package com.pratone.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Fallback wake-word detection built on Android's built-in [SpeechRecognizer], used automatically
 * whenever [PorcupineWakeWordDetector] is not configured.
 *
 * IMPORTANT — read before relying on this: this is NOT a real wake-word engine. It repeatedly
 * runs short recognition sessions and checks whether the transcript contains the wake phrase.
 * Consequences, stated plainly rather than hidden:
 *   - Noticeably higher latency (each session has startup/teardown overhead, typically
 *     300ms-1.5s, versus Porcupine's near-instant continuous detection).
 *   - Higher battery cost, since each session may involve more processing than a keyword spotter.
 *   - Reliability varies by OEM: some Android skins throttle or kill repeated
 *     recognizer restarts in the background more aggressively than others.
 *   - `EXTRA_PREFER_OFFLINE` is requested so it can run without network where an offline language
 *     pack is installed, but Android does not guarantee an offline model is present — if it isn't,
 *     the system recognizer may silently use network recognition instead.
 *
 * This exists so the app is fully functional out of the box with zero external setup, per the
 * "do not fake it, but also do not require a mandatory third-party account before anything works"
 * balance requested. Swap to Porcupine in Settings for meaningfully better behavior.
 */
class SpeechRecognizerWakeWordDetector(
    private val context: Context,
    // Substring-matched against the recognizer's transcript (see containsWakePhrase below), so an
    // uncommon/invented word like "Pratone" is more prone to being misheard/misspelled by the
    // recognizer than a common word would be. If false negatives are common in testing, add
    // phonetic variants here (e.g. "pratoni", "pratona") rather than relying on one exact spelling.
    private val wakePhrase: String = "hey pratone",
) : WakeWordDetector {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var onDetected: (() -> Unit)? = null
    private var onErr: ((Throwable) -> Unit)? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(onWakeWordDetected: () -> Unit, onError: (Throwable) -> Unit) {
        if (!isAvailable()) {
            onError(IllegalStateException("No speech recognition service available on this device"))
            return
        }
        onDetected = onWakeWordDetected
        onErr = onError
        running = true
        startSession()
    }

    private fun startSession() {
        if (!running) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            scheduleRestart()
        }
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean =
        candidates.any { it.lowercase().contains(wakePhrase) }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle) {
            val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
            if (containsWakePhrase(matches)) {
                onDetected?.invoke()
                // Caller is expected to call stop()/restart the cycle after handling detection.
            }
        }

        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            if (containsWakePhrase(matches)) {
                onDetected?.invoke()
            } else {
                scheduleRestart()
            }
        }

        override fun onError(error: Int) {
            // ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT are expected constantly in a listening loop —
            // they just mean "nothing recognized this round", not a real failure.
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> scheduleRestart()
                SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart(delayMs = 500)
                else -> {
                    onErr?.invoke(RuntimeException("SpeechRecognizer error code $error"))
                    scheduleRestart(delayMs = 1000)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun scheduleRestart(delayMs: Long = 150) {
        if (!running) return
        handler.postDelayed({ startSession() }, delayMs)
    }

    override fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    companion object {
        private const val TAG = "SpeechRecWakeWord"
    }
}
