package fulguris.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Entry points for playing a sniffed media item.
 * Video opens the floating player (supports picture-in-picture) or, via
 * "listen only", goes straight to the background audio service.
 * Audio starts the background service, so it keeps playing while browsing.
 *
 * Playback replays the request the way the browser made it: the page's
 * Referer, the WebView User-Agent and the media host's cookies travel as
 * HTTP headers, because many hosts answer a bare URL with 403.
 */
object MediaPlayback {

    const val EXTRA_URL = "alir_media_url"
    const val EXTRA_REFERER = "alir_media_referer"
    const val EXTRA_USER_AGENT = "alir_media_ua"
    const val EXTRA_COOKIE = "alir_media_cookie"
    const val EXTRA_ACTION = "alir_media_action"
    const val ACTION_TOGGLE = "toggle"
    const val ACTION_STOP = "stop"

    fun playVideo(context: Context, item: MediaSniffer.Item) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putItemExtras(this, item)
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun playAudio(context: Context, item: MediaSniffer.Item) {
        val intent = Intent(context, AudioService::class.java).apply {
            putItemExtras(this, item)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /** Control intent for the running audio service (toggle play/pause, stop). */
    fun sendAction(context: Context, action: String) {
        val intent = Intent(context, AudioService::class.java).apply {
            putExtra(EXTRA_ACTION, action)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun itemFrom(intent: Intent): MediaSniffer.Item? {
        val url = intent.getStringExtra(EXTRA_URL) ?: return null
        val type = MediaSniffer.detectType(url) ?: return null
        val referer = intent.getStringExtra(EXTRA_REFERER) ?: ""
        return MediaSniffer.Item(
            url = url,
            pageUrl = referer,
            type = type,
            referer = referer,
            userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: "",
            cookie = intent.getStringExtra(EXTRA_COOKIE)
        )
    }

    fun mediaItem(item: MediaSniffer.Item): MediaItem = MediaItem.fromUri(item.url)

    /**
     * Player whose HTTP requests (including HLS/DASH playlist and segment
     * fetches, which share this factory) carry the item's headers.
     */
    fun createPlayer(context: Context, item: MediaSniffer.Item): ExoPlayer {
        val props = mutableMapOf<String, String>()
        if (item.referer.isNotEmpty()) props["Referer"] = item.referer
        if (item.userAgent.isNotEmpty()) props["User-Agent"] = item.userAgent
        item.cookie?.takeIf { it.isNotEmpty() }?.let { props["Cookie"] = it }
        val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(props)
        val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)
        return ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
    }

    private fun putItemExtras(intent: Intent, item: MediaSniffer.Item) {
        intent.putExtra(EXTRA_URL, item.url)
        intent.putExtra(EXTRA_REFERER, item.referer)
        intent.putExtra(EXTRA_USER_AGENT, item.userAgent)
        item.cookie?.let { intent.putExtra(EXTRA_COOKIE, it) }
    }
}
