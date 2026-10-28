package fulguris.media

/**
 * Tiny playback-state bus between [AudioService] and the browser mini-player.
 * The service updates it on play/pause/error/stop; the browser activity
 * observes it through [listener] to show or hide the mini-player bar.
 * Audio only — video has its own player UI and picture-in-picture.
 */
object NowPlaying {

    var title: String? = null
        private set

    var isPlaying: Boolean = false
        private set

    var listener: (() -> Unit)? = null

    fun update(title: String?, isPlaying: Boolean) {
        this.title = title
        this.isPlaying = isPlaying
        listener?.invoke()
    }

    fun clear() {
        title = null
        isPlaying = false
        listener?.invoke()
    }
}
