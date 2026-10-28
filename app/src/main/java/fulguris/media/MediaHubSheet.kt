package fulguris.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import fulguris.R

/**
 * Alir media hub: bottom sheet listing every playable media URL the sniffer
 * has seen. Tap an item to play it — video opens the floating player,
 * audio keeps playing in the background service.
 */
class MediaHubSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: Adapter
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_media_hub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyView = view.findViewById(R.id.media_empty)
        adapter = Adapter { item ->
            dismiss()
            if (item.type == MediaSniffer.Type.VIDEO) {
                MediaPlayback.playVideo(requireContext(), item.url)
            } else {
                MediaPlayback.playAudio(requireContext(), item.url)
            }
        }
        val list = view.findViewById<RecyclerView>(R.id.media_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) refresh()
    }

    private fun refresh() {
        val items = MediaSniffer.items()
        adapter.submit(items)
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private class Adapter(private val onClick: (MediaSniffer.Item) -> Unit) :
        RecyclerView.Adapter<Holder>() {

        private var items: List<MediaSniffer.Item> = emptyList()

        fun submit(newItems: List<MediaSniffer.Item>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.title.text = displayName(item.url)
            val typeLabel = holder.itemView.context.getString(
                if (item.type == MediaSniffer.Type.VIDEO) R.string.media_type_video
                else R.string.media_type_audio
            )
            val pageHost = Uri.parse(item.pageUrl).host ?: ""
            holder.sub.text =
                if (pageHost.isEmpty()) typeLabel else "$typeLabel • $pageHost"
            holder.icon.setImageResource(
                if (item.type == MediaSniffer.Type.VIDEO) R.drawable.ic_videocam
                else R.drawable.ic_play_arrow
            )
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.media_title)
        val sub: TextView = view.findViewById(R.id.media_sub)
        val icon: ImageView = view.findViewById(R.id.media_icon)
    }

    companion object {
        /** "example.com/clip.mp4" style label, query stripped. */
        fun displayName(url: String): String {
            val uri = Uri.parse(url)
            val host = uri.host ?: ""
            val file = uri.lastPathSegment ?: ""
            return when {
                host.isEmpty() && file.isEmpty() -> url
                file.isEmpty() -> host
                else -> "$host/$file"
            }
        }
    }
}
