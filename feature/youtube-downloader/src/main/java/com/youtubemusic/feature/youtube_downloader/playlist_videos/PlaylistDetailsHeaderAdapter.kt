package com.youtubemusic.feature.youtube_downloader.playlist_videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.model.YouTubePlaylistDetails
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.HeaderPlaylistDetailsBinding

class PlaylistDetailsHeaderAdapter : RecyclerView.Adapter<PlaylistDetailsHeaderAdapter.Content>() {

    var data: YouTubePlaylistDetails? = null
    set(value) {
        if (value != field)
            notifyItemChanged(0)
        field = value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Content {
        val inflater = LayoutInflater.from(parent.context)
        return Content(DataBindingUtil.inflate(inflater, R.layout.header_playlist_details, parent, false) )
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: Content, position: Int) {
        data?.let { holder.onBind(it) }
    }

    inner class Content(private val binding: HeaderPlaylistDetailsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(playlistDetails: YouTubePlaylistDetails) {
            binding.playlist = playlistDetails
        }
    }

}