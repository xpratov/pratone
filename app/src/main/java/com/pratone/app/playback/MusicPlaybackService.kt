package com.pratone.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pratone.app.MainActivity

/**
 * The single foreground service responsible for audio playback. Media3's [MediaSessionService]
 * handles, out of the box and without any custom NotificationManager code from us:
 *  - promoting itself to a foreground service while a session is active
 *  - building/updating the system media notification (title/artist/art/prev/play-pause/next)
 *  - publishing MediaSession state so the lock screen and Bluetooth/headset buttons work
 *  - surviving the app being swiped away from Recents while music is playing
 *
 * This is intentionally the *only* place ExoPlayer is constructed. UI and voice control never
 * touch ExoPlayer directly — they talk to this service through a MediaController (see
 * PlayerManager), which is what keeps the player usable even if the process hosting the UI dies.
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true, // ExoPlayer auto pauses/ducks/resumes on focus changes
            )
            .setHandleAudioBecomingNoisy(true) // auto-pause when headphones are unplugged
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Media3 stops the service automatically once playback ends and no session is bound, per its docs. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
