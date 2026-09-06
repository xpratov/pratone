package com.pratone.app.voice

/**
 * Abstraction over "is the wake phrase being said right now". Kept deliberately narrow so the
 * detection engine can be swapped without touching anything downstream (command recognition,
 * parsing, playback). Two implementations ship with this project:
 *
 *  - [PorcupineWakeWordDetector]: a real on-device wake-word engine (Picovoice Porcupine).
 *    Low latency, low battery cost, works fully offline. Requires a free personal AccessKey.
 *  - [SpeechRecognizerWakeWordDetector]: a fallback built on Android's SpeechRecognizer, used
 *    automatically when no Porcupine key is configured. Higher latency, more battery, and
 *    reliability varies by OEM — see README "Wake word: engine vs fallback".
 */
interface WakeWordDetector {
    /** True if this detector is usable on this device right now (model present, mic free, etc). */
    fun isAvailable(): Boolean

    /** Begin listening. [onWakeWordDetected] is invoked on detection; the detector keeps running
     *  until [stop] is called — callers restart it after handling a detection if they want to
     *  keep listening. */
    fun start(onWakeWordDetected: () -> Unit, onError: (Throwable) -> Unit)

    fun stop()
}
