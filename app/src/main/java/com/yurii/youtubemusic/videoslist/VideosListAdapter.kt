package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.ItemsHandler
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.VideoItemBinding
import com.yurii.youtubemusic.models.VideoItem
import java.lang.IllegalStateException

class VideosListAdapter(private val videos: List<VideoItem>, private val itemsHandler: ItemsHandler? = null) : RecyclerView.Adapter<VideosListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val videoItem = DataBindingUtil.inflate<VideoItemBinding>(inflater, R.layout.video_item, parent, false)

        return ViewHolder(videoItem.root)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoItem = videos[position]
        val binding = DataBindingUtil.getBinding<VideoItemBinding>(holder.videoItem)
        binding?.let {
            it.title.text = videoItem.title
            it.channelTitle.text = videoItem.authorChannelTitle
            Picasso.get().load(videoItem.thumbnail).into(it.thumbnail)
            itemsHandler?.let { itemsHandler ->
                if (itemsHandler.isLoading(videoItem))
                    binding.download.visibility = View.INVISIBLE
            }

            it.download.setOnClickListener {
                itemsHandler?.download(videoItem)
            }
        } ?: throw IllegalStateException("PlayListItem binding item cannot be null")
    }

    class ViewHolder(val videoItem: View) : RecyclerView.ViewHolder(videoItem)
}