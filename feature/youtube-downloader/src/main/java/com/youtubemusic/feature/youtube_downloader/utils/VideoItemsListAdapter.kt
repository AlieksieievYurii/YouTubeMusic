package com.youtubemusic.feature.youtube_downloader.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.common.getValueAnimator
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.feature.youtube_downloader.databinding.ItemVideoBinding

internal class VideoItemsListAdapter(private val callback: Callback) :
    PagingDataAdapter<VideoItem, VideoItemsListAdapter.VideoItemViewHolder>(Comparator) {
    interface Callback {
        fun getDownloadingJobState(videoItem: VideoItem): DownloadManager.State
        fun onDownload(videoItem: VideoItem)
        fun onDownloadAndAssignedCategories(videoItem: VideoItem)
        fun onCancelDownloading(videoItem: VideoItem)
        fun onDelete(videoItem: VideoItem)
        fun onShowErrorDetail(videoItem: VideoItem)
    }

    private var expandedItem: VideoItem? = null
    private lateinit var recyclerView: RecyclerView

    fun updateItem(videoItemStatus: DownloadManager.Status) {
        findVisibleViewHolder(videoItemStatus.videoId)?.updateStatus(videoItemStatus.state)
    }

    private fun findVisibleViewHolder(videoId: String): VideoItemViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1) {
                val viewHolder = recyclerView.findViewHolderForLayoutPosition(it) as? VideoItemViewHolder
                if (viewHolder?.currentData?.id == videoId)
                    return viewHolder
            }
        }
        return null
    }

    object Comparator : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem == newItem
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    inner class VideoItemViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        var currentData: VideoItem? = null
            private set

        fun bind(videoItem: VideoItem) {
            currentData = videoItem
            binding.videoItem = videoItem
            updateStatus(callback.getDownloadingJobState(videoItem))
            expandItem(this, expandedItem == videoItem, animate = false)

            binding.cardContainer.setOnClickListener {
                collapseOrExpandItem(videoItem)
            }

            binding.btnDownload.setOnLongClickDownloadLister {
                callback.onDownloadAndAssignedCategories(videoItem)
            }

            binding.btnDownload.setOnClickStateListener {
                when (binding.btnDownload.state) {
                    is DownloadButton.State.Download -> callback.onDownload(videoItem)
                    is DownloadButton.State.Downloading -> callback.onCancelDownloading(videoItem)
                    is DownloadButton.State.Downloaded -> callback.onDelete(videoItem)
                    is DownloadButton.State.Failed -> callback.onShowErrorDetail(videoItem)
                }
            }
        }

        fun updateStatus(videoItemStatus: DownloadManager.State) {
            binding.btnDownload.state = when (videoItemStatus) {
                is DownloadManager.State.Download -> DownloadButton.State.Download
                is DownloadManager.State.Downloaded -> DownloadButton.State.Downloaded(videoItemStatus.size)
                is DownloadManager.State.Downloading -> DownloadButton.State.Downloading(videoItemStatus.currentSize, videoItemStatus.size)
                is DownloadManager.State.Failed -> DownloadButton.State.Failed
            }
        }

        private fun collapseOrExpandItem(data: VideoItem) {
            expandedItem = if (expandedItem == data) {
                expandItem(this, expand = false, animate = true)
                null
            } else {
                collapseExpandedItem()
                expandItem(this, expand = true, animate = true)
                data
            }
        }

        private fun collapseExpandedItem() {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
                if (getItem(it) == expandedItem) {
                    val expandedVisibleViewHolder = recyclerView.findViewHolderForLayoutPosition(it) as VideoItemViewHolder
                    expandItem(expandedVisibleViewHolder, expand = false, animate = true)
                }
            }
        }

        private fun expandItem(view: VideoItemViewHolder, expand: Boolean, animate: Boolean) {
            view.binding.expandableLayout.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val expandedItemHeight: Int = view.binding.expandableLayout.measuredHeight

            if (animate) {
                val animator = getValueAnimator(expand, 200L, AccelerateDecelerateInterpolator()) {
                    setExpandedProgress(view, expandedItemHeight, it)
                }

                if (expand) animator.doOnStart { view.binding.expandableLayout.isVisible = true }
                else animator.doOnEnd { view.binding.expandableLayout.isVisible = false }

                animator.start()

            } else setExpandedProgress(view, expandedItemHeight, if (expand) 1f else 0f)
        }

        private fun setExpandedProgress(view: VideoItemViewHolder, expandedHeight: Int, progress: Float) {
            view.binding.expandableLayout.layoutParams.height =
                if (progress == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (expandedHeight * progress).toInt()
            view.binding.expandableLayout.requestLayout()
        }
    }

    override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
        return VideoItemViewHolder(ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
}