package com.yurii.youtubemusic.utilities

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
                videoItem?.let {
                    findVideoItemView(videoItem) {
                        it.progressBar.apply {
                            visibility = View.VISIBLE
                            setProgress(progress)
                        }
                    }
                }
            }
            MusicDownloaderService.DOWNLOADING_FINISHED_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as? VideoItem
                videoItem?.let {
                    findVideoItemView(videoItem) {
                        it.download.visibility = View.GONE
                        it.progressBar.visibility = View.GONE
                        it.loading.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun findVideoItemView(videoItem: VideoItem, onFound: ((VideoItemBinding) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
            if (videoItems!![position].videoId == videoItem.videoId) {
                val videoItemView = (recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as VideosListAdapter.ViewHolder).videoItem
                val binding = DataBindingUtil.getBinding<VideoItemBinding>(videoItemView)
                binding?.let { onFound.invoke(it) }
            }
        }
    }

    fun setVideoItems(videoItems: List<VideoItem>) {
        this.videoItems = videoItems
        recyclerView.adapter = VideosListAdapter(videoItems, this)
    }

    fun onStart() {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, IntentFilter().also {
            it.addAction(MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION)
            it.addAction(MusicDownloaderService.DOWNLOADING_FINISHED_ACTION)
        })
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
        DataStorage.getMusicStorage(context).walk().forEach {
            Regex(".*(?=\\.)").find(it.name)?.let { regex ->
                if (regex.value == videoItem.videoId)
                    return true
            }
        }
        return false
    }

    override fun isLoading(videoItem: VideoItem): Boolean {
        MusicDownloaderService.Instance.serviceInterface?.let { return it.isLoading(videoItem) }
        return false
    }
}