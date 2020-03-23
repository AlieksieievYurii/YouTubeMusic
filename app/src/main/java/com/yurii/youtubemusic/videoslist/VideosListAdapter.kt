package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.VideoItemBinding
import com.yurii.youtubemusic.models.VideoItem
import java.lang.IllegalStateException

interface VideoItemInterface {
    fun onItemClickDownload(videoItem: VideoItem)
    fun exists(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
}

class VideosListAdapter(private val videos: List<VideoItem>, private val videoItemInterface: VideoItemInterface) :
    RecyclerView.Adapter<VideosListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val videoItem = DataBindingUtil.inflate<VideoItemBinding>(inflater, R.layout.video_item, parent, false)

        return ViewHolder(videoItem.root)
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoItem = videos[position]
        val binding = DataBindingUtil.getBinding<VideoItemBinding>(holder.videoItem)
        binding?.let { videoItemView ->
            videoItemView.title.text = videoItem.title
            videoItemView.channelTitle.text = videoItem.authorChannelTitle
            Picasso.get().load(videoItem.thumbnail).into(videoItemView.thumbnail)
            binding.download.setOnClickListener {
                videoItemInterface.onItemClickDownload(videoItem)
                setLoadingState(videoItemView)
            }
            if (videoItemInterface.isLoading(videoItem))
                setLoadingState(videoItemView)
            else
                setReadyToDownloadState(videoItemView)
        } ?: throw IllegalStateException("PlayListItem binding item cannot be null")
    }

    private fun setReadyToDownloadState(binding: VideoItemBinding) {
        binding.download.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setLoadingState(binding: VideoItemBinding) {
        binding.download.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    class ViewHolder(val videoItem: View) : RecyclerView.ViewHolder(videoItem)
}