package fulguris.media

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import fulguris.R

/**
 * Alir floating video player: plays a sniffed video URL with ExoPlayer and
 * enters picture-in-picture when the user leaves, so the video keeps floating
 * over other apps (and over the browser).
 */
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        supportActionBar?.hide()
        playerView = findViewById(R.id.player_view)
        val item = MediaPlayback.itemFrom(intent) ?: run {
            finish()
            return
        }
        player = MediaPlayback.createPlayer(this, item).also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaPlayback.mediaItem(item))
            exo.prepare()
            exo.play()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            player?.let {
                if (it.isPlaying) {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // In picture-in-picture the video must keep playing; otherwise pause.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            return
        }
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
