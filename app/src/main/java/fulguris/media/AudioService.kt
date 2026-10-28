package fulguris.media

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Alir background audio: a media session service, so sniffed audio keeps
 * playing while the user browses, with playback controls in the notification.
 * The service moves itself to the foreground when playback starts (media3
 * posts the media notification for the active session).
 */
class AudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(MediaPlayback.EXTRA_URL)?.let { url ->
            mediaSession?.player?.apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                play()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
