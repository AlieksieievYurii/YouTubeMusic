package com.yurii.youtubemusic.screens.youtube

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
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.ui.DownloadButton
import com.yurii.youtubemusic.ui.getValueAnimator

class VideoItemsListAdapter(private val callback: Callback) :
    PagingDataAdapter<VideoItem, VideoItemsListAdapter.MyViewHolder>(Comparator) {
    interface Callback {
        fun getItemStatus(videoItem: VideoItem): VideoItemStatus
        fun onDownload(videoItem: VideoItem)
        fun onDownloadAndAssignedCategories(videoItem: VideoItem)
        fun onCancelDownloading(videoItem: VideoItem)
        fun onDelete(videoItem: VideoItem)
        fun onShowErrorDetail(videoItem: VideoItem)
    }

    private var expandedItem: VideoItem? = null
    private lateinit var recyclerView: RecyclerView

    fun updateItem(videoItemStatus: VideoItemStatus) {
        findVisibleViewHolder(videoItemStatus.videoItem.id)?.updateStatus(videoItemStatus)
    }

    private fun findVisibleViewHolder(videoId: String): MyViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1 && getItem(it)?.id == videoId) {
                return recyclerView.findViewHolderForLayoutPosition(it) as MyViewHolder
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

    inner class MyViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(videoItem: VideoItem) {
            binding.videoItem = videoItem
            updateStatus(callback.getItemStatus(videoItem))
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

        fun updateStatus(videoItemStatus: VideoItemStatus) {
            binding.btnDownload.state = when (videoItemStatus) {
                is VideoItemStatus.Download -> DownloadButton.State.Download
                is VideoItemStatus.Downloaded -> DownloadButton.State.Downloaded(videoItemStatus.size)
                is VideoItemStatus.Downloading -> DownloadButton.State.Downloading(videoItemStatus.currentSize, videoItemStatus.size)
                is VideoItemStatus.Failed -> DownloadButton.State.Failed
                else -> throw IllegalStateException("Unhandled status: $videoItemStatus")
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
                    val expandedVisibleViewHolder = recyclerView.findViewHolderForLayoutPosition(it) as MyViewHolder
                    expandItem(expandedVisibleViewHolder, expand = false, animate = true)
                }
            }
        }

        private fun expandItem(view: MyViewHolder, expand: Boolean, animate: Boolean) {
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

        private fun setExpandedProgress(view: MyViewHolder, expandedHeight: Int, progress: Float) {
            view.binding.expandableLayout.layoutParams.height =
                if (progress == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (expandedHeight * progress).toInt()
            view.binding.expandableLayout.requestLayout()
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
}