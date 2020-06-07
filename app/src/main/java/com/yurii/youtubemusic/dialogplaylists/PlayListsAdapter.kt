package com.yurii.youtubemusic.dialogplaylists

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.Playlist
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemPlaylistBinding
import com.yurii.youtubemusic.utilities.BaseViewHolder
import com.yurii.youtubemusic.utilities.LoadingViewHolder
import com.yurii.youtubemusic.utilities.VIEW_TYPE_LOADING
import com.yurii.youtubemusic.utilities.VIEW_TYPE_NORMAL
import java.lang.IllegalStateException

class PlayListsAdapter(private val onClickListener: View.OnClickListener) : RecyclerView.Adapter<BaseViewHolder>() {
    val playLists: MutableList<Playlist> = mutableListOf()
    private var isLoaderVisible: Boolean = false

    var currentPlaylist: Playlist? = null

    override fun getItemViewType(position: Int): Int = if (isLoaderVisible)
        if (position == playLists.lastIndex) VIEW_TYPE_LOADING else VIEW_TYPE_NORMAL
    else
        VIEW_TYPE_NORMAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> PlayListViewHolder(DataBindingUtil.inflate<ItemPlaylistBinding>(inflater, R.layout.item_playlist, parent, false).root)
            VIEW_TYPE_LOADING -> LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
            else -> throw IllegalStateException("Illegal view type")
        }
    }

    fun setLoadingState() {
        isLoaderVisible = true
        playLists.add(Playlist())
        notifyItemInserted(playLists.lastIndex)
    }

    fun removeLoadingState() {
        isLoaderVisible = false
        val position = playLists.lastIndex
        playLists.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addPlayLists(playListsItems: List<Playlist>) {
        playLists.addAll(playListsItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = playLists.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_NORMAL) {
            (holder as PlayListViewHolder).apply {
                bind(playLists[position], currentPlaylist)
                setOnClickListener(onClickListener)
            }
        }
    }

    class PlayListViewHolder(playListItem: View) : BaseViewHolder(playListItem) {
        val binding = DataBindingUtil.getBinding<ItemPlaylistBinding>(playListItem)
        fun bind(playListItem: Playlist, currentPlayList: Playlist?) {
            binding?.let {
                if (playListItem.id == currentPlayList?.id)
                    it.root.setBackgroundColor(Color.GRAY)
                else
                    it.root.setBackgroundColor(Color.WHITE)

                it.title.text = playListItem.snippet.title

                it.videosCount.text = it.root.context.resources.getString(
                    R.string.label_videos_count,
                    playListItem.contentDetails.itemCount
                )

                Picasso.get().load(playListItem.snippet.thumbnails.default.url).into(it.image)
            }
        }

        fun setOnClickListener(onClickListener: View.OnClickListener) {
            binding?.root?.setOnClickListener(onClickListener)
        }
    }
}