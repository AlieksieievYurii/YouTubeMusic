package com.youtubemusic.feature.youtube_downloader.playlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.model.YouTubePlaylist
import com.youtubemusic.feature.youtube_downloader.databinding.ItemYoutubePlaylistBinding

class YouTubePlaylistsAdapter(private val onSelectedPlaylist: (YouTubePlaylist) -> Unit) :
    PagingDataAdapter<YouTubePlaylist, YouTubePlaylistsAdapter.PlaylistViewHolder>(Comparator) {

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
        return PlaylistViewHolder(ItemYoutubePlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class PlaylistViewHolder(val binding: ItemYoutubePlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: YouTubePlaylist) {
            binding.apply {
                this.playlist = playlist
                root.setOnClickListener { onSelectedPlaylist.invoke(playlist) }
            }
        }
    }
}