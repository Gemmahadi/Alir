package fulguris.media

/**
 * Alir Media Sniffer: records playable media URLs seen while browsing.
 *
 * Detection is extension-based because [android.webkit.WebViewClient.shouldInterceptRequest]
 * only sees the request URL, not response headers. Only non-blocked requests are
 * recorded (the hook in WebPageClient skips anything the ad blocker stopped).
 * Safe to call from any thread: shouldInterceptRequest runs off the UI thread.
 */
object MediaSniffer {

    enum class Type { VIDEO, AUDIO }

    data class Item(
        val url: String,
        val pageUrl: String,
        val type: Type,
        val foundAt: Long = System.currentTimeMillis()
    )

    private val lock = Any()
    private val itemsByUrl = LinkedHashMap<String, Item>()

    /** All recorded items, newest first. */
    fun items(): List<Item> = synchronized(lock) {
        itemsByUrl.values.sortedByDescending { it.foundAt }
    }

    fun count(): Int = synchronized(lock) { itemsByUrl.size }

    fun clear() = synchronized(lock) { itemsByUrl.clear() }

    /**
     * Record [url] if it looks like playable media. Returns the item, or null
     * when the URL is not playable media. Repeated URLs are de-duplicated.
     */
    fun sniff(url: String, pageUrl: String): Item? {
        val type = detectType(url) ?: return null
        synchronized(lock) {
            itemsByUrl[url]?.let { return it }
            val item = Item(url, pageUrl, type)
            itemsByUrl[url] = item
            // Cap memory: drop the oldest items beyond 200.
            while (itemsByUrl.size > 200) {
                val oldest = itemsByUrl.values.minByOrNull { it.foundAt }?.url ?: break
                itemsByUrl.remove(oldest)
            }
            return item
        }
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
