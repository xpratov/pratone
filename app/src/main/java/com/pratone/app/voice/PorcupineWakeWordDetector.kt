package com.pratone.app.voice

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context
import android.util.Log

/**
 * Real, on-device wake-word engine using Picovoice Porcupine.
 *
 * WHY this engine: Porcupine runs a tiny (~a few hundred KB) keyword-spotting model continuously
 * and locally — no network call, no full ASR session — so it's cheap enough to run 24/7 in a
 * foreground service. This is categorically different from restarting Android's SpeechRecognizer
 * in a loop (see [SpeechRecognizerWakeWordDetector]): Porcupine is built for exactly this job.
 *
 * SETUP REQUIRED (cannot be shipped pre-configured — see README):
 * 1. Create a free account at https://console.picovoice.ai and copy your personal AccessKey.
 *    Paste it into Settings → Voice → "Porcupine Access Key". Picovoice's free tier is for
 *    personal/development use; check their terms before shipping a commercial release.
 * 2. Porcupine's built-in keyword list does not include "Hey Pratone" — custom phrases must be
 *    trained on the Picovoice Console (free, takes a couple of minutes) which produces an
 *    Android `.ppn` model file. Drop that file into `app/src/main/assets/hey_pratone.ppn`.
 *    Until that file exists, [isAvailable] returns false and the app automatically falls back to
 *    [SpeechRecognizerWakeWordDetector].
 *
 * APK size impact: the Porcupine Android SDK + native libs add roughly 3-5MB to the APK
 * (multiple ABIs); the keyword model itself is under 50KB. Offline: yes, fully, once the
 * AccessKey has been validated once (it performs a one-time online validation, then works
 * offline). Android compatibility: API 21+, arm/x86, so it covers this app's minSdk 26 fine.
 * Licensing: Porcupine's Android SDK is Apache-2.0; the AccessKey/model usage itself is governed
 * by Picovoice's own terms (free for personal/dev use, paid tiers for commercial distribution at
 * scale) — read https://picovoice.ai/pricing/ before shipping this to the Play Store.
 */
class PorcupineWakeWordDetector(
    private val context: Context,
    private val accessKey: String,
) : WakeWordDetector {

    private var manager: PorcupineManager? = null

    private val keywordAssetPath = "hey_pratone.ppn"

    override fun isAvailable(): Boolean {
        if (accessKey.isBlank()) return false
        return try {
            context.assets.open(keywordAssetPath).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun start(onWakeWordDetected: () -> Unit, onError: (Throwable) -> Unit) {
        if (!isAvailable()) {
            onError(IllegalStateException("Porcupine not configured: missing AccessKey or $keywordAssetPath"))
            return
        }
        try {
            manager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(keywordAssetPath)
                .setSensitivity(0.6f) // 0..1, higher = more sensitive but more false positives
                .build(context, PorcupineManagerCallback { _ -> onWakeWordDetected() })
            manager?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Porcupine", e)
            onError(e)
        }
    }

    override fun stop() {
        try {
            manager?.stop()
            manager?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Porcupine cleanly", e)
        }
        manager = null
    }

    companion object {
        private const val TAG = "PorcupineWakeWord"
    }
}
