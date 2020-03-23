package com.yurii.youtubemusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.databinding.VideoItemBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.MusicDownloaderService
import com.yurii.youtubemusic.videoslist.VideoItemInterface
import com.yurii.youtubemusic.videoslist.VideosListAdapter

class VideoItemsHandler(private val recyclerView: RecyclerView) : VideoItemInterface, BroadcastReceiver() {
    private val context: Context = recyclerView.context
    private var videoItems: List<VideoItem>? = null

    init {
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as? VideoItem
                val progress = intent.getIntExtra(MusicDownloaderService.EXTRA_PROGRESS, 0)
                videoItem?.let {findAndSetProgress(videoItem, progress)}
            }
        }
    }

    private fun findAndSetProgress(videoItem: VideoItem, progress: Int) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
            if (videoItems!![position].videoId == videoItem.videoId) {
                val videoItemView = (recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as VideosListAdapter.ViewHolder).videoItem
                val binding = DataBindingUtil.getBinding<VideoItemBinding>(videoItemView)
                binding?.progressBar?.apply {
                    visibility = View.VISIBLE
                    setProgress(progress)
                }
            }
        }
    }

    fun setVideoItems(videoItems: List<VideoItem>) {
        this.videoItems = videoItems
        recyclerView.adapter = VideosListAdapter(videoItems, this)
    }

    fun onStart() {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, IntentFilter(MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION))
    }

    fun onStop() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
    }

    override fun onItemClickDownload(videoItem: VideoItem) {
        check(!isLoading(videoItem)) { "Video item is already downloading" }

        context.startService(Intent(context, MusicDownloaderService::class.java).also {
            it.putExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM, videoItem)
        })
    }

    override fun exists(videoItem: VideoItem): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isLoading(videoItem: VideoItem): Boolean {
        MusicDownloaderService.Instance.serviceInterface?.let { return it.isLoading(videoItem) }
        return false
    }
}