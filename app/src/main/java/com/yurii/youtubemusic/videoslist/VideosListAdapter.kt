package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.ui.DownloadButton
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

enum class ItemState {
    DOWNLOAD, DOWNLOADED, DOWNLOADING
}

interface VideoItemInterface {
    fun download(videoItem: VideoItem)
    fun cancelDownload(videoItem: VideoItem)
    fun remove(videoItem: VideoItem)
    fun exists(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
    fun getCurrentProgress(videoItem: VideoItem): Progress?
}


class VideosListAdapter(context: Context, private val videoItemInterface: VideoItemInterface) : RecyclerView.Adapter<BaseViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val videos: MutableList<VideoItem> = mutableListOf()
    private var isLoaderVisible: Boolean = false
    private var expandedPosition = NO_POSITION
    private lateinit var recyclerView: RecyclerView

    fun setLoadingState() {
        isLoaderVisible = true
        videos.add(VideoItem())
        notifyItemInserted(videos.lastIndex)
    }

    fun removeLoadingState() {
        isLoaderVisible = false
        val position = videos.lastIndex
        videos.removeAt(position)
        notifyItemRemoved(position)
    }

    fun setNewVideoItems(videoItems: List<VideoItem>) {
        expandedPosition = NO_POSITION
        videos.clear()
        videos.addAll(videoItems)
        notifyDataSetChanged()
    }

    fun addVideoItems(videoItems: List<VideoItem>) {
        videos.addAll(videoItems)
        notifyDataSetChanged()
    }

    fun removeAllVideoItem() {
        videos.clear()
    }

    fun isListOfVideoItemsEmpty(): Boolean = videos.isEmpty()

    fun setProgress(videoItem: VideoItem, progress: Progress) {
        findVideoItemView(videoItem) {
            it.setProgress(progress)
        }
    }

    fun setFinishedState(videoItem: VideoItem) {
        findVideoItemView(videoItem) {
            it.setState(state = ItemState.DOWNLOADED)
            it.setProgress(null)
        }
    }

    private fun findVideoItemView(videoItem: VideoItem, onFound: ((VideoViewHolder) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION || videos.isEmpty())
                continue

            if (isLoaderVisible && videos.lastIndex == position)
            // When new video items are loading, the last list's item is empty Video item, because it is for "loading item"
                continue

            if (videos[position].videoId == videoItem.videoId) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as VideoViewHolder
                onFound.invoke(viewHolder)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> VideoViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_video, parent, false))
            VIEW_TYPE_LOADING -> LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
            else -> throw IllegalStateException("Illegal view type")
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoaderVisible && position == videos.lastIndex)
            VIEW_TYPE_LOADING
        else
            VIEW_TYPE_NORMAL
    }

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (isViewItemLoadingType(position))
            return

        val videoItem = videos[position]
        val videoViewHolder = holder as VideoViewHolder

        setExpandOrCollapse(videoViewHolder, position)
        setItemClickListener(videoViewHolder, position)
        setButtonClickListener(videoViewHolder, videoItem)
        setVideoItemState(videoViewHolder, videoItem)
    }

    private fun isViewItemLoadingType(position: Int): Boolean = getItemViewType(position) == VIEW_TYPE_LOADING

    private fun setExpandOrCollapse(videoViewHolder: VideoViewHolder, position: Int) {
        val isExpanded: Boolean = position == expandedPosition
        expandItem(videoViewHolder, isExpanded, animate = false)
    }

    private fun setItemClickListener(videoViewHolder: VideoViewHolder, position: Int) {
        videoViewHolder.cardContainer.setOnClickListener {
            when (expandedPosition) {
                NO_POSITION -> {
                    expandedPosition = position
                    expandItem(videoViewHolder, expand = true, animate = true)
                }
                position -> {
                    expandedPosition = NO_POSITION
                    expandItem(videoViewHolder, expand = false, animate = true)
                }
                else -> {
                    val expandedItem = recyclerView.findViewHolderForLayoutPosition(expandedPosition) as? VideoViewHolder
                    if (expandedItem != null)
                        expandItem(expandedItem, expand = false, animate = true)

                    expandItem(videoViewHolder, expand = true, animate = true)
                    expandedPosition = position
                }
            }
        }
    }

    private fun setButtonClickListener(videoViewHolder: VideoViewHolder, videoItem: VideoItem) {
        videoViewHolder.downloadButton.setOnClickStateListener(object : DownloadButton.OnClickListener {
            override fun onClick(view: View, currentState: Int) {
                when (currentState) {
                    DownloadButton.STATE_DOWNLOAD -> {
                        videoItemInterface.download(videoItem)
                        videoViewHolder.setData(videoItem, state = ItemState.DOWNLOADING)
                        videoViewHolder.downloadButton.state = DownloadButton.STATE_DOWNLOADING
                    }

                    DownloadButton.STATE_DOWNLOADED -> {
                        videoItemInterface.remove(videoItem)
                        videoViewHolder.setData(videoItem)
                        videoViewHolder.downloadButton.state = DownloadButton.STATE_DOWNLOAD
                    }

                    DownloadButton.STATE_DOWNLOADING -> {
                        videoItemInterface.cancelDownload(videoItem)
                        videoViewHolder.setData(videoItem)
                        videoViewHolder.downloadButton.state = DownloadButton.STATE_DOWNLOAD
                    }
                    else -> throw IllegalArgumentException("Unknown called state!")
                }
            }
        })
    }

    private fun setVideoItemState(videoViewHolder: VideoViewHolder, videoItem: VideoItem) {
        when {
            videoItemInterface.exists(videoItem) -> videoViewHolder.setData(videoItem, state = ItemState.DOWNLOADED)
            videoItemInterface.isLoading(videoItem) -> {
                videoViewHolder.setData(videoItem, progress = videoItemInterface.getCurrentProgress(videoItem), state = ItemState.DOWNLOADING)
            }
            else -> videoViewHolder.setData(videoItem, state = ItemState.DOWNLOAD)
        }
    }


    private fun expandItem(view: VideoViewHolder, expand: Boolean, animate: Boolean) {
        view.expandableLayout.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val expandedItemHeight: Int = view.expandableLayout.measuredHeight

        if (animate) {
            val animator = getValueAnimator(expand, 200L, AccelerateDecelerateInterpolator()) { setExpandedProgress(view, expandedItemHeight, it) }

            if (expand) animator.doOnStart { view.expandableLayout.isVisible = true }
            else animator.doOnEnd { view.expandableLayout.isVisible = false }

            animator.start()

        } else setExpandedProgress(view, expandedItemHeight, if (expand) 1f else 0f)
    }

    private fun setExpandedProgress(view: VideoViewHolder, expandedHeight: Int, progress: Float) {
        view.expandableLayout.layoutParams.height = if (progress == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (expandedHeight * progress).toInt()
        view.expandableLayout.requestLayout()
    }

    class VideoViewHolder(private val videoItemVideoBinding: ItemVideoBinding) : BaseViewHolder(videoItemVideoBinding.root) {
        val cardContainer: View = videoItemVideoBinding.cardContainer
        val expandableLayout: View = videoItemVideoBinding.expandableLayout
        val downloadButton: DownloadButton = videoItemVideoBinding.btnDownload

        fun setProgress(progress: Progress?) {
            videoItemVideoBinding.progress = progress
        }

        fun setState(state: ItemState) {
            downloadButton.state = when (state) {
                ItemState.DOWNLOAD -> DownloadButton.STATE_DOWNLOAD
                ItemState.DOWNLOADING -> DownloadButton.STATE_DOWNLOADING
                ItemState.DOWNLOADED -> DownloadButton.STATE_DOWNLOADED
            }
        }

        fun setData(videoItem: VideoItem, progress: Progress? = null, state: ItemState = ItemState.DOWNLOAD) {
            setState(state)

            videoItemVideoBinding.apply {
                this.videoItem = videoItem
                this.progress = progress
            }.executePendingBindings()
        }
    }

    companion object {
        private const val NO_POSITION = -1
    }
}