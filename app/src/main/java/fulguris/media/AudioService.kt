package fulguris.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import fulguris.R

/**
 * Alir background audio: a media session service, so sniffed audio keeps
 * playing while the user browses, with playback controls in the notification.
 *
 * Hardening (Android 12+ kills a foreground service that never shows its
 * notification in time): the service goes foreground immediately with a
 * loading notification when a URL arrives, and a failed load (e.g. a 403)
 * stops the service cleanly instead of lingering without a notification.
 * The browser mini-player follows playback through [NowPlaying].
 */
class AudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var currentTitle: String? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            NowPlaying.update(currentTitle, isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            NowPlaying.clear()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(MediaPlayback.EXTRA_ACTION)) {
            MediaPlayback.ACTION_TOGGLE -> {
                mediaSession?.player?.let { player ->
                    if (player.isPlaying) player.pause() else player.play()
                }
                return START_STICKY
            }
            MediaPlayback.ACTION_STOP -> {
                NowPlaying.clear()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val item = intent?.let { MediaPlayback.itemFrom(it) }
        if (item == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Session (and its header-carrying player) is built on first play, so
        // playback replays the request the way the browser made it.
        if (mediaSession == null) {
            val player = MediaPlayback.createPlayer(this, item).also { exo ->
                exo.addListener(playerListener)
            }
            mediaSession = MediaSession.Builder(this, player).build()
        }
        currentTitle = MediaHubSheet.displayName(item.url)
        goForeground(currentTitle)
        mediaSession?.player?.apply {
            setMediaItem(MediaPlayback.mediaItem(item))
            prepare()
            play()
        }
        NowPlaying.update(currentTitle, true)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.removeListener(playerListener)
            session.player.release()
            session.release()
        }
        mediaSession = null
        currentTitle = null
        NowPlaying.clear()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alir Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Foreground immediately, so a slow or failing load never trips the Android 12+ timeout. */
    private fun goForeground(title: String?) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setContentTitle("Alir")
            .setContentText(title ?: "Memuat audio…")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "alir_audio"
    }
}
