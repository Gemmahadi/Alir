package fulguris.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Entry points for playing a sniffed media URL.
 * Video opens the floating player (supports picture-in-picture).
 * Audio starts the background service, so it keeps playing while browsing.
 */
object MediaPlayback {

    const val EXTRA_URL = "alir_media_url"

    fun playVideo(context: Context, url: String) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(EXTRA_URL, url)
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun playAudio(context: Context, url: String) {
        val intent = Intent(context, AudioService::class.java).apply {
            putExtra(EXTRA_URL, url)
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
