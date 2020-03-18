package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.PlaylistItem
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.VideoItemBinding
import java.lang.IllegalStateException

class VideosListAdapter(private val videos: List<PlaylistItem>) : RecyclerView.Adapter<VideosListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val videoItem = DataBindingUtil.inflate<VideoItemBinding>(inflater, R.layout.video_item, parent, false)

        return ViewHolder(videoItem.root)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playListItem = videos[position]
        val binding = DataBindingUtil.getBinding<VideoItemBinding>(holder.videoItem)
        binding?.let {
            it.title.text = playListItem.snippet.title
            //TODO channel title must be name of channel which owns current video
            it.channelTitle.text = playListItem.snippet.channelTitle
            Picasso.get().load(playListItem.snippet.thumbnails.default.url).into(it.thumbnail)
        }?: throw IllegalStateException("PlayListItem binding item cannot be null")
    }

    class ViewHolder(val videoItem: View) : RecyclerView.ViewHolder(videoItem)
}