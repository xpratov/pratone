package com.pratone.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Runs one short recognition session to capture a single command ("next", "pause", ...) after
 * the wake word has fired. Deliberately separate from [SpeechRecognizerWakeWordDetector] — this
 * one only ever runs briefly and on-demand, so its cost profile is completely different from a
 * continuous listening loop.
 */
class CommandSpeechRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listenOnce(onResult: (String?) -> Unit, onError: (Throwable) -> Unit) {
        if (!isAvailable()) {
            onError(IllegalStateException("No speech recognition service available on this device"))
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    onResult(text)
                }

                override fun onError(error: Int) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        onResult(null) // "I didn't catch that" — handled upstream as UNKNOWN
                    } else {
                        onError(RuntimeException("SpeechRecognizer error code $error"))
                    }
                }
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }
        recognizer?.startListening(intent)
    }

    fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}
