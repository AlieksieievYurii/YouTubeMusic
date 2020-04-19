package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.utilities.*
import java.lang.IllegalStateException

enum class ItemState {
    DOWNLOAD, EXISTS, IS_LOADING
}

interface VideoItemInterface {
    fun onItemClickDownload(videoItem: VideoItem)
    fun exists(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
    fun getCurrentProgress(videoItem: VideoItem): Int
}


class VideosListAdapter(private val videoItemInterface: VideoItemInterface) : RecyclerView.Adapter<BaseViewHolder>() {
    companion object {
        private const val NO_POSITION = -1
        private var expandedPosition = NO_POSITION
    }

    init {
        expandedPosition = NO_POSITION
    }

    val videos: MutableList<VideoItem> = mutableListOf()
    private var isLoaderVisible: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL ->
                VideoViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_video, parent, false)) {
                    notifyDataSetChanged()
                }
            VIEW_TYPE_LOADING ->
                LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
            else -> throw IllegalStateException("Illegal view type")
        }
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
        val videoItem = videos[position]
        if (getItemViewType(position) == VIEW_TYPE_NORMAL) {
            val videoViewHolder = holder as VideoViewHolder
            when {
                videoItemInterface.exists(videoItem) -> videoViewHolder.bind(videoItem, position, state = ItemState.EXISTS)
                videoItemInterface.isLoading(videoItem) -> {
                    videoItem.downloadingProgress = videoItemInterface.getCurrentProgress(videoItem)
                    videoViewHolder.bind(videoItem, position, state = ItemState.IS_LOADING)
                }
                else -> videoViewHolder.let { viewHolder ->
                    viewHolder.bind(videoItem, position, state = ItemState.DOWNLOAD)

                    viewHolder.setOnDownloadClickListener(View.OnClickListener {
                        videoItemInterface.onItemClickDownload(videoItem)
                        viewHolder.bind(videoItem, position, state = ItemState.IS_LOADING)
                    })

                    viewHolder.setOnRemoveClickListener(View.OnClickListener {

                    })
                }
            }
        }
    }

    class VideoViewHolder(val videoItemVideoBinding: ItemVideoBinding, private val onItemChange: ((position: Int) -> Unit)) :
        BaseViewHolder(videoItemVideoBinding.root) {
        private var isExpanded = false

        fun bind(videoItem: VideoItem, position: Int, state: ItemState = ItemState.DOWNLOAD) {
            videoItemVideoBinding.apply {
                this.videoItem = videoItem
                this.state = state

                if (position == expandedPosition)
                    expandDetails().also { isExpanded = true }
                else
                    collapseDetails().also { isExpanded = false }

                this.root.setOnClickListener {
                    expandedPosition = if (isExpanded) NO_POSITION else position
                    if (expandedPosition == NO_POSITION)
                        collapseDetails().also { isExpanded = false; onItemChange.invoke(position) }
                    else
                        expandDetails().also { isExpanded = true; onItemChange.invoke(position) }
                }
            }.executePendingBindings()
        }


        private fun expandDetails() {
            videoItemVideoBinding.detailsPartLayout.visibility = View.VISIBLE
        }

        private fun collapseDetails() {
            videoItemVideoBinding.detailsPartLayout.visibility = View.GONE
        }

        fun setOnDownloadClickListener(onClickListener: View.OnClickListener) {
            videoItemVideoBinding.download.setOnClickListener(onClickListener)
        }

        fun setOnRemoveClickListener(onClickListener: View.OnClickListener) {
            videoItemVideoBinding.remove.setOnClickListener(onClickListener)
        }
    }
}