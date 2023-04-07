package com.yurii.youtubemusic.screens.youtube.playlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.model.YouTubePlaylist
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemPlaylistBinding

class PlaylistsAdapter(private val onSelectedPlaylist: (YouTubePlaylist) -> Unit) :
    PagingDataAdapter<YouTubePlaylist, PlaylistsAdapter.PlaylistViewHolder>(Comparator) {

    var selectedPlaylist: YouTubePlaylist? = null

    private object Comparator : DiffUtil.ItemCallback<YouTubePlaylist>() {
        override fun areItemsTheSame(oldItem: YouTubePlaylist, newItem: YouTubePlaylist) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: YouTubePlaylist, newItem: YouTubePlaylist) =
            oldItem == newItem
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: YouTubePlaylist) {
            val backgroundColor = ContextCompat.getColor(
                binding.root.context, if (selectedPlaylist == playlist) R.color.lightGray else R.color.white
            )
            binding.apply {
                this.playlist = playlist
                root.setBackgroundColor(backgroundColor)
                root.setOnClickListener {
                    selectedPlaylist = playlist
                    onSelectedPlaylist.invoke(playlist)
                }
            }
        }
    }
}