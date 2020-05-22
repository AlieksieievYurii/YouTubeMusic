package com.yurii.youtubemusic.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.DownloaderInteroperableInterface
import com.yurii.youtubemusic.services.MusicDownloaderService
import com.yurii.youtubemusic.videoslist.ItemState
import com.yurii.youtubemusic.videoslist.VideoItemInterface
import com.yurii.youtubemusic.videoslist.VideosListAdapter
import java.lang.RuntimeException

interface Loader {
    fun onLoadMoreVideoItems()
}

class VideoItemsHandler(private val recyclerView: RecyclerView, private val loader: Loader) : VideoItemInterface, BroadcastReceiver() {
    private val context: Context = recyclerView.context
    private var isLoadingNewVideoItems = true
    private val videoListAdapter: VideosListAdapter = VideosListAdapter(this)
    private var isLast: Boolean = false
    init {
        val layoutManager = LinearLayoutManager(context)
        recyclerView.apply {
            this.setHasFixedSize(true)
            this.layoutManager = layoutManager
            this.adapter = videoListAdapter
        }

        recyclerView.addOnScrollListener(object : PaginationListener(layoutManager) {
            override fun isLastPage(): Boolean {
                return isLast
            }

            override fun isLoading(): Boolean {
                return isLoadingNewVideoItems
            }

            override fun loadMoreItems() {
                isLoadingNewVideoItems = true
                recyclerView.post { videoListAdapter.setLoadingState() }
                loader.onLoadMoreVideoItems()
            }
        })
    }

    fun setOnScrollListener(scrollListener: RecyclerView.OnScrollListener) = recyclerView.addOnScrollListener(scrollListener)

    fun isVideosEmpty() = videoListAdapter.videos.isEmpty()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MusicDownloaderService.DOWNLOADING_PROGRESS_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as? VideoItem
                val progress = intent.getIntExtra(MusicDownloaderService.EXTRA_PROGRESS, 0)
                videoItem?.let {
                    findVideoItemView(videoItem) {
                        it.progressBar.progress = progress
                    }
                }
            }
            MusicDownloaderService.DOWNLOADING_FINISHED_ACTION -> {
                val videoItem = intent.getSerializableExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM) as? VideoItem
                videoItem?.let {

                    addTag(videoItem)

                    findVideoItemView(videoItem) {
                        it.state = ItemState.EXISTS
                        it.executePendingBindings()
                    }
                }
            }
        }
    }

    private fun addTag(videoItem: VideoItem) {
        val file = DataStorage.getMusic(context, videoItem)
        val tag = Tag(title = videoItem.title, authorChannel = videoItem.authorChannelTitle)
        TaggerV1(file).writeTag(tag)
    }

    private fun findVideoItemView(videoItem: VideoItem, onFound: ((ItemVideoBinding) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION)
                continue

            if (isLoadingNewVideoItems && videoListAdapter.videos.lastIndex == position)
            // When new video items are loading, the last list's item is empty Video item, because it is for "loading item"
                continue

            if (videoListAdapter.videos[position].videoId == videoItem.videoId) {
                val binding =
                    (recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as VideosListAdapter.VideoViewHolder).videoItemVideoBinding
                onFound.invoke(binding)
            }
        }
    }

    fun removeAllVideos() {
        videoListAdapter.videos.clear()
    }

    fun setNewVideoItems(videoItems: List<VideoItem>, isLast: Boolean) {
        this.isLast = isLast
        videoListAdapter.setNewVideoItems(videoItems)
        isLoadingNewVideoItems = false
    }

    fun addMoreVideoItems(videoItems: List<VideoItem>, isLast: Boolean) {
        this.isLast = isLast
        if (isLoadingNewVideoItems) {
            videoListAdapter.removeLoadingState()
            isLoadingNewVideoItems = false
        }
        videoListAdapter.addVideoItems(videoItems)
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

    override fun isExisted(videoItem: VideoItem): Boolean {
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

    override fun remove(videoItem: VideoItem) {
        val file = DataStorage.getMusic(context, videoItem)
        if (!file.delete())
            throw RuntimeException("Cannot remove the music file $file")
    }
}