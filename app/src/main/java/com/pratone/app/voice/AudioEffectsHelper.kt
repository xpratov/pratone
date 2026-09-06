package com.pratone.app.voice

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor

/**
 * Android exposes AEC/NS/AGC as optional platform audio effects — not every device implements
 * them (this depends on the OEM's audio HAL), so we must check availability at runtime rather
 * than assume. When [AcousticEchoCanceler] is unavailable, [com.pratone.app.playback.PlayerManager.duckVolume]
 * is used as a fallback mitigation instead — see its doc comment for why that's imperfect.
 *
 * Android's [android.speech.SpeechRecognizer] and most on-device wake-word engines (including
 * Porcupine) read from [android.media.AudioRecord] sessions that the platform itself applies
 * these effects to when available and requested via the session id — we don't attach the
 * effects to our own AudioRecord instance directly (neither SpeechRecognizer nor Porcupine
 * exposes their internal session id for that), so this class is used purely to detect and
 * surface availability to Settings/logging and to decide whether ducking is needed as a fallback.
 */
object AudioEffectsHelper {
    fun isAecAvailable(): Boolean = AcousticEchoCanceler.isAvailable()
    fun isNoiseSuppressorAvailable(): Boolean = NoiseSuppressor.isAvailable()
    fun isAgcAvailable(): Boolean = AutomaticGainControl.isAvailable()

    /** If true, the app should duck playback volume while listening as a fallback mitigation. */
    fun shouldDuckAsFallback(): Boolean = !isAecAvailable()
}
