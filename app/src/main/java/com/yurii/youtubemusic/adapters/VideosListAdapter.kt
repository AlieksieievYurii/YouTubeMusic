package com.yurii.youtubemusic.adapters

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
import com.yurii.youtubemusic.ui.getValueAnimator
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException

enum class ItemState {
    DOWNLOAD, DOWNLOADED, DOWNLOADING, FAILED
}


class VideosListAdapter(context: Context, private val callback: CallBack) : RecyclerView.Adapter<BaseViewHolder>() {
    interface CallBack {
        fun onDownload(videoItem: VideoItem)
        fun onDownloadAndAddCategories(videoItem: VideoItem)
        fun onCancelDownload(videoItem: VideoItem)
        fun onNotifyFailedToDownload(videoItem: VideoItem)
        fun onRemove(videoItem: VideoItem)
        fun exists(videoItem: VideoItem): Boolean
        fun isLoading(videoItem: VideoItem): Boolean
        fun isDownloadingFailed(videoItem: VideoItem): Boolean
        fun getCurrentProgress(videoItem: VideoItem): Progress?
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val videos: MutableList<VideoItem> = mutableListOf()
    private var isLoaderVisible: Boolean = false
    private var expandedPosition = NO_POSITION
    private lateinit var recyclerView: RecyclerView

    fun setLoadingState() {
        isLoaderVisible = true
        videos.add(VideoItem.createMock())
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

    fun removeAllVideoItem() = videos.clear()

    fun isVideosEmpty(): Boolean = videos.isEmpty()

    fun setProgress(videoItem: VideoItem, progress: Progress) {
        findVideoItemViewHolder(videoItem.videoId) {
            it.setProgress(progress)
        }
    }

    fun setDownloadedState(videoItem: VideoItem) {
        findVideoItemViewHolder(videoItem.videoId) {
            it.setState(state = ItemState.DOWNLOADED)
            it.setProgress(null)
        }
    }

    fun setFailedState(videoItem: VideoItem) {
        findVideoItemViewHolder(videoItem.videoId) {
            it.setState(ItemState.FAILED)
        }
    }

    fun setDownloadState(videoItemId: String) {
        findVideoItemViewHolder(videoItemId) {
            it.setState(ItemState.DOWNLOAD)
        }
    }

    fun setDownloadingState(videoItem: VideoItem) {
        findVideoItemViewHolder(videoItem.videoId) {
            it.setState(ItemState.DOWNLOADING)
        }
    }

    private fun findVideoItemViewHolder(videoItemId: String, onFound: ((VideoViewHolder) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION || videos.isEmpty())
                continue

            if (isLoaderVisible && videos.lastIndex == position)
            // When new video items are loading, the last list's item is empty Video item, because it is for "loading item"
                continue

            if (videos[position].videoId == videoItemId) {
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
        videoViewHolder.downloadButton.setOnLongClickDownloadLister {
            callback.onDownloadAndAddCategories(videoItem)
        }

        videoViewHolder.downloadButton.setOnClickStateListener {
            when (videoViewHolder.downloadButton.state) {
                DownloadButton.STATE_DOWNLOAD -> callback.onDownload(videoItem)
                DownloadButton.STATE_DOWNLOADING -> callback.onCancelDownload(videoItem)
                DownloadButton.STATE_DOWNLOADED -> callback.onRemove(videoItem)
                DownloadButton.STATE_FAILED -> callback.onNotifyFailedToDownload(videoItem)
                else -> throw IllegalStateException("Unhandled state")
            }
        }
    }

    private fun setVideoItemState(videoViewHolder: VideoViewHolder, videoItem: VideoItem) {
        when {
            callback.exists(videoItem) -> videoViewHolder.setData(videoItem, state = ItemState.DOWNLOADED)
            callback.isLoading(videoItem) -> videoViewHolder.setData(videoItem, callback.getCurrentProgress(videoItem), ItemState.DOWNLOADING)
            callback.isDownloadingFailed(videoItem) -> videoViewHolder.setData(videoItem, state = ItemState.FAILED)
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
                ItemState.FAILED -> DownloadButton.STATE_FAILED
            }
            videoItemVideoBinding.state = state
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