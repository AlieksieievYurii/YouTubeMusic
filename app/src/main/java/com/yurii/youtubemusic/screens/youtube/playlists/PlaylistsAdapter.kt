package com.yurii.youtubemusic.screens.youtube.playlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemPlaylistBinding

class PlaylistsAdapter(private val onSelectedPlaylist: (Playlist) -> Unit) :
    PagingDataAdapter<Playlist, PlaylistsAdapter.PlaylistViewHolder>(Comparator) {

    var selectedPlaylist: Playlist? = null

    private object Comparator : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem == newItem
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist) {
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