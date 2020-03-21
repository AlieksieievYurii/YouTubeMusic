package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.PlaylistItem
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.services.MusicDownloaderService
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.VideoItemBinding
import com.yurii.youtubemusic.models.VideoItem
import java.lang.IllegalStateException

class VideosListAdapter(private val videos: List<PlaylistItem>, private val context: Context) : RecyclerView.Adapter<VideosListAdapter.ViewHolder>() {
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
            it.download.setOnClickListener {
                val intent = Intent(context, MusicDownloaderService::class.java)
                val b = Bundle()
                b.putSerializable(MusicDownloaderService.EXTRA_VIDEO_ITEM, VideoItem(playListItem.snippet.resourceId.videoId))
                intent.putExtras(b)
                context.startService(intent)
            }
        } ?: throw IllegalStateException("PlayListItem binding item cannot be null")
    }


    class ViewHolder(val videoItem: View) : RecyclerView.ViewHolder(videoItem)
}