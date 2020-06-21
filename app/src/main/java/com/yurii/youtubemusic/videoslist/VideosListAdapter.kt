package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.downloader.Progress
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException

enum class ItemState {
    DOWNLOAD, EXISTS, IS_LOADING
}

interface VideoItemInterface {
    fun onItemClickDownload(videoItem: VideoItem)
    fun remove(videoItem: VideoItem)
    fun isExisted(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
    fun getCurrentProgress(videoItem: VideoItem): Progress?
    fun cancelDownloading(videoItem: VideoItem)
}


class VideosListAdapter(context: Context, private val videoItemInterface: VideoItemInterface) : RecyclerView.Adapter<BaseViewHolder>() {
    companion object {
        private const val NO_POSITION = -1
    }

    val videos: MutableList<VideoItem> = mutableListOf()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var recyclerView: RecyclerView
    private var isLoaderVisible: Boolean = false
    private var expandedPosition = NO_POSITION


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL ->
                VideoViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_video, parent, false))
            VIEW_TYPE_LOADING ->
                LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
            else -> throw IllegalStateException("Illegal view type")
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int = if (isLoaderVisible)
        if (position == videos.lastIndex) VIEW_TYPE_LOADING else VIEW_TYPE_NORMAL
    else
        VIEW_TYPE_NORMAL


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

    override fun getItemCount(): Int = videos.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_LOADING)
            return

        val videoItem = videos[position]
        val videoViewHolder = holder as VideoViewHolder

        expandItem(videoViewHolder, position == expandedPosition, animate = false)

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

//        videoViewHolder.setOnRemoveClickListener(View.OnClickListener {
//            videoItemInterface.remove(videoItem)
//            videoViewHolder.bind(videoItem, position)
//        })
//
//        videoViewHolder.setOnDownloadClickListener(View.OnClickListener {
//            videoItemInterface.onItemClickDownload(videoItem)
//            videoViewHolder.bind(videoItem, position, state = ItemState.IS_LOADING)
//        })
//
//        videoViewHolder.setOnCancelClickListener(View.OnClickListener {
//            videoItemInterface.cancelDownloading(videoItem)
//            videoViewHolder.bind(videoItem, position)
//        })

        when {
            videoItemInterface.isExisted(videoItem) -> videoViewHolder.setData(videoItem, state = ItemState.EXISTS)
            videoItemInterface.isLoading(videoItem) -> {
                videoViewHolder.setData(
                    videoItem,
                    progress = videoItemInterface.getCurrentProgress(videoItem),
                    state = ItemState.IS_LOADING
                )
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

    class VideoViewHolder(val videoItemVideoBinding: ItemVideoBinding) : BaseViewHolder(videoItemVideoBinding.root) {
        val cardContainer: View = videoItemVideoBinding.cardContainer
        val expandableLayout: View = videoItemVideoBinding.expandableLayout
        fun setData(videoItem: VideoItem, progress: Progress? = null, state: ItemState = ItemState.DOWNLOAD) =
            videoItemVideoBinding.apply {
                this.videoItem = videoItem
                this.state = state
                this.progress = progress
            }.executePendingBindings()
    }
}