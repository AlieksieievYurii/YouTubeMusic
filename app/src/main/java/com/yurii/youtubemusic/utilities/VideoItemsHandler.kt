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
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.DownloaderInteroperableInterface
import com.yurii.youtubemusic.services.MusicDownloaderService

import com.yurii.youtubemusic.videoslist.VideoItemInterface
import com.yurii.youtubemusic.videoslist.VideosListAdapter

interface Loader {
    fun onLoadMoreVideoItems(pageToken: String?)
}

class VideoItemsHandler(private val recyclerView: RecyclerView, private val loader: Loader) : VideoItemInterface, BroadcastReceiver() {
    private val context: Context = recyclerView.context
    private var isLoadingNewVideoItems = true
    private val videoListAdapter: VideosListAdapter = VideosListAdapter(this)
    private var nextPageToken: String? = null

    init {
        val layoutManager = LinearLayoutManager(context)
        recyclerView.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = videoListAdapter
        }

        recyclerView.addOnScrollListener(object : VideosListAdapter.PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean {
                return nextPageToken.isNullOrBlank()
            }

            override fun isLoading(): Boolean {
                return isLoadingNewVideoItems
            }

            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                recyclerView.post { videoListAdapter.setLoadingState() }
                loader.onLoadMoreVideoItems(nextPageToken)
            }
        })
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

    private fun findVideoItemView(videoItem: VideoItem, onFound: ((ItemVideoBinding) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION)
                continue

            if (isLoadingNewVideoItems && videoListAdapter.videos.lastIndex == position)
            // When new video items are loading, the last list item is empty Video item, because it is for "loading item"
                continue

            if (videoListAdapter.videos[position].videoId == videoItem.videoId) {
                val videoItemView = (recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as VideosListAdapter.VideoViewHolder).videoItemView
                val binding = DataBindingUtil.getBinding<ItemVideoBinding>(videoItemView)
                binding?.let { onFound.invoke(it) }
            }
        }
    }

    fun setNewVideoItems(videoItems: List<VideoItem>, nextPageToken: String?) {
        videoListAdapter.setNewVideoItems(videoItems)
        isLoadingNewVideoItems = false
        this.nextPageToken = nextPageToken
    }

    fun addMoreVideoItems(videoItems: List<VideoItem>, nextPageToken: String?) {
        if (isLoadingNewVideoItems) {
            videoListAdapter.removeLoadingState()
            isLoadingNewVideoItems = false
        }
        videoListAdapter.addVideoItems(videoItems)
        this.nextPageToken = nextPageToken
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

    override fun isLoading(videoItem: VideoItem): Boolean =
        MusicDownloaderService.Instance.serviceInterface?.isLoading(videoItem) ?: false


    override fun getCurrentProgress(videoItem: VideoItem): Int =
        MusicDownloaderService.Instance.serviceInterface?.getProgress(videoItem) ?: DownloaderInteroperableInterface.NO_PROGRESS

}