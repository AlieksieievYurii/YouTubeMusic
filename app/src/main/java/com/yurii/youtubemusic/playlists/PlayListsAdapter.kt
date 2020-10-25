package com.yurii.youtubemusic.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemPlaylistBinding
import com.yurii.youtubemusic.utilities.BaseViewHolder
import com.yurii.youtubemusic.utilities.LoadingViewHolder
import com.yurii.youtubemusic.utilities.VIEW_TYPE_LOADING
import com.yurii.youtubemusic.utilities.VIEW_TYPE_NORMAL
import java.lang.IllegalStateException

interface OnClickPlayListListener {
    fun onClickPlayList(playlist: Playlist)
}

class PlayListsAdapter(private val onClickPlayListListener: OnClickPlayListListener, private var currentPlaylist: Playlist? = null) :
    RecyclerView.Adapter<BaseViewHolder>() {
    private var isLoaderVisible: Boolean = false
    private val playLists: MutableList<Playlist> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> PlayListViewHolder(DataBindingUtil.inflate<ItemPlaylistBinding>(inflater, R.layout.item_playlist, parent, false).root)
            VIEW_TYPE_LOADING -> LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
            else -> throw IllegalStateException("Illegal view type")
        }
    }

    override fun getItemViewType(position: Int): Int = if (isLoaderVisible)
        if (position == playLists.lastIndex) VIEW_TYPE_LOADING else VIEW_TYPE_NORMAL
    else
        VIEW_TYPE_NORMAL

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
            val playlist = playLists[position]
            (holder as PlayListViewHolder).bind(playlist, onClickPlayListListener, isAlreadySelected = currentPlaylist?.id == playlist.id)
        }
    }

    class PlayListViewHolder(playListItem: View) : BaseViewHolder(playListItem) {
        val binding = DataBindingUtil.getBinding<ItemPlaylistBinding>(playListItem)
        fun bind(playList: Playlist, onClickPlayListListener: OnClickPlayListListener, isAlreadySelected: Boolean) {
            binding?.apply {
                val context = this.root.context
                this.playlist = playList
                root.setBackgroundColor(ContextCompat.getColor(context, if(isAlreadySelected) R.color.lightGray else  R.color.white))
                root.setOnClickListener {
                    onClickPlayListListener.onClickPlayList(playList)
                }
                executePendingBindings()
            }
        }
    }
}