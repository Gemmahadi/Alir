package fulguris.media

/**
 * Alir Media Sniffer: records playable media URLs seen while browsing.
 *
 * Detection is extension-based because [android.webkit.WebViewClient.shouldInterceptRequest]
 * only sees the request URL, not response headers. Only non-blocked requests are
 * recorded (the hook in WebPageClient skips anything the ad blocker stopped).
 * Safe to call from any thread: shouldInterceptRequest runs off the UI thread.
 *
 * Each item also keeps the playback context the page itself used (Referer,
 * WebView User-Agent, media-host cookies) so ExoPlayer can replay the request
 * the way the browser did — plain URLs without those headers get 403 from
 * many hotlink-protected hosts.
 */
object MediaSniffer {

    enum class Type { VIDEO, AUDIO }

    data class Item(
        val url: String,
        val pageUrl: String,
        val type: Type,
        /** Page URL, sent back as Referer on playback. */
        val referer: String = "",
        /** WebView User-Agent, sent so hosts don't reject the player as a bot. */
        val userAgent: String = "",
        /** Cookies for the media host, if the WebView holds any. */
        val cookie: String? = null,
        val foundAt: Long = System.currentTimeMillis()
    )

    private val lock = Any()
    private val itemsByUrl = LinkedHashMap<String, Item>()
    private val listeners = mutableListOf<() -> Unit>()

    /** All recorded items, newest first. */
    fun items(): List<Item> = synchronized(lock) {
        itemsByUrl.values.sortedByDescending { it.foundAt }
    }

    fun count(): Int = synchronized(lock) { itemsByUrl.size }

    /** Items sniffed on pages of [host] (for the per-page toolbar badge). */
    fun countForHost(host: String): Int = synchronized(lock) {
        if (host.isEmpty()) return 0
        itemsByUrl.values.count { android.net.Uri.parse(it.pageUrl).host == host }
    }

    fun clear() = synchronized(lock) { itemsByUrl.clear() }

    /**
     * Change listener (e.g. the toolbar badge). Invoked on the calling thread,
     * so UI listeners must post to the main thread themselves.
     */
    fun addListener(listener: () -> Unit) = synchronized(lock) { listeners += listener }

    fun removeListener(listener: () -> Unit) = synchronized(lock) { listeners -= listener }

    /**
     * Record [url] if it looks like playable media. Returns the item, or null
     * when the URL is not playable media. Repeated URLs are de-duplicated.
     */
    fun sniff(
        url: String,
        pageUrl: String,
        userAgent: String = "",
        cookie: String? = null
    ): Item? {
        val type = detectType(url) ?: return null
        val item: Item
        synchronized(lock) {
            itemsByUrl[url]?.let { return it }
            item = Item(url, pageUrl, type, referer = pageUrl, userAgent = userAgent, cookie = cookie)
            itemsByUrl[url] = item
            // Cap memory: drop the oldest items beyond 200.
            while (itemsByUrl.size > 200) {
                val oldest = itemsByUrl.values.minByOrNull { it.foundAt }?.url ?: break
                itemsByUrl.remove(oldest)
            }
        }
        synchronized(lock) { listeners.toList() }.forEach { it() }
        return item
    }

    /** Extension-based detection on the URL path (query and fragment stripped). */
    fun detectType(rawUrl: String): Type? {
        val path = rawUrl.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mov") ||
                path.endsWith(".m3u8") || path.endsWith(".mpd") -> Type.VIDEO
            path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".ogg") ||
                path.endsWith(".oga") || path.endsWith(".wav") || path.endsWith(".flac") ||
                path.endsWith(".aac") -> Type.AUDIO
            else -> null
        }
    }
}
