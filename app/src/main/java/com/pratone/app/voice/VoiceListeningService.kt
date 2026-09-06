package com.pratone.app.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.pratone.app.MainActivity
import com.pratone.app.R
import com.pratone.app.playback.PlayerManager

/**
 * Dedicated foreground service (type "microphone") for background wake-word listening. Kept
 * separate from [com.pratone.app.playback.MusicPlaybackService] intentionally:
 *  - it has a different Android 14+ foreground-service-type requirement (microphone vs mediaPlayback)
 *  - it must be independently start/stoppable — a user can play music with voice control off,
 *    or (less usefully, but the architecture allows it) listen for commands with nothing queued
 *  - per section 12 of the spec, active mic use must be clearly communicated to the user: this
 *    service's persistent notification is that communication, and Android requires it regardless.
 *
 * Starting this service does NOT itself request RECORD_AUDIO at runtime — that must already have
 * been granted (see PermissionsScreen); this service will stop itself if the permission is
 * missing rather than silently failing.
 */
class VoiceListeningService : Service() {

    private var voiceController: VoiceController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        val playerManager = PlayerManager.get(applicationContext)
        val controller = VoiceController.get(applicationContext, playerManager)
        voiceController = controller
        val accessKey = intent?.getStringExtra(EXTRA_PORCUPINE_KEY).orEmpty()
        if (accessKey.isNotBlank()) controller.updatePorcupineAccessKey(accessKey)
        playerManager.connect { controller.start() }
        return START_STICKY
    }

    override fun onDestroy() {
        voiceController?.stop()
        voiceController = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "voice_listening"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_voice),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.voice_listening_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val EXTRA_PORCUPINE_KEY = "porcupine_access_key"

        fun start(context: android.content.Context, porcupineAccessKey: String = "") {
            val intent = Intent(context, VoiceListeningService::class.java)
                .putExtra(EXTRA_PORCUPINE_KEY, porcupineAccessKey)
            context.startForegroundService(intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, VoiceListeningService::class.java))
        }
    }
}
