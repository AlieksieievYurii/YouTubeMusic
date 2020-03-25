package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.ItemVideoBinding
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.DownloaderInteroperableInterface
import com.yurii.youtubemusic.utilities.BaseViewHolder
import com.yurii.youtubemusic.utilities.LoadingViewHolder
import com.yurii.youtubemusic.utilities.VIEW_TYPE_LOADING
import com.yurii.youtubemusic.utilities.VIEW_TYPE_NORMAL
import java.lang.IllegalStateException

interface VideoItemInterface {
    fun onItemClickDownload(videoItem: VideoItem)
    fun exists(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
    fun getCurrentProgress(videoItem: VideoItem): Int
}


class VideosListAdapter(private val videoItemInterface: VideoItemInterface) : RecyclerView.Adapter<BaseViewHolder>() {
    val videos: MutableList<VideoItem> = mutableListOf()
    private var isLoaderVisible: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> VideoViewHolder(DataBindingUtil.inflate<ItemVideoBinding>(inflater, R.layout.item_video, parent, false).root)
            VIEW_TYPE_LOADING -> LoadingViewHolder(DataBindingUtil.inflate<ItemLoadingBinding>(inflater, R.layout.item_loading, parent, false).root)
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
                videoItemInterface.exists(videoItem) -> videoViewHolder.bind(videoItem, VideoViewHolder.EXISTS)
                videoItemInterface.isLoading(videoItem) -> {
                    videoItem.downloadingProgress = videoItemInterface.getCurrentProgress(videoItem)
                    videoViewHolder.bind(videoItem, VideoViewHolder.IS_LOADING)}
                else -> videoViewHolder.let { viewHolder ->
                    viewHolder.bind(videoItem, VideoViewHolder.DOWNLOAD)
                    viewHolder.setOnDownloadClickListener(View.OnClickListener {
                        videoItemInterface.onItemClickDownload(videoItem)
                        viewHolder.bind(videoItem, VideoViewHolder.IS_LOADING)
                    })
                }
            }
        }
    }

    class VideoViewHolder(val videoItemView: View) : BaseViewHolder(videoItemView) {
        companion object {
            const val DOWNLOAD: Int = -1
            const val EXISTS: Int = 0
            const val IS_LOADING: Int = 1
        }

        private val binding = DataBindingUtil.getBinding<ItemVideoBinding>(videoItemView)
        fun bind(videoItem: VideoItem, mode: Int = DOWNLOAD) {
            binding?.let {
                it.title.text = videoItem.title
                it.channelTitle.text = videoItem.authorChannelTitle
                Picasso.get().load(videoItem.thumbnail).into(it.thumbnail)

                when (mode) {
                    EXISTS -> {
                        it.download.visibility = View.GONE
                        it.loading.visibility = View.GONE
                        it.progressBar.visibility = View.GONE
                    }
                    IS_LOADING -> {
                        it.download.visibility = View.GONE
                        it.loading.visibility = View.VISIBLE
                        it.progressBar.visibility = View.VISIBLE
                        if (videoItem.downloadingProgress != DownloaderInteroperableInterface.NO_PROGRESS)
                            binding.progressBar.progress = videoItem.downloadingProgress
                    }
                    DOWNLOAD -> {
                        binding.download.visibility = View.VISIBLE
                        binding.loading.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }

        }

        fun setOnDownloadClickListener(onClickListener: View.OnClickListener) {
            binding?.apply { download.setOnClickListener(onClickListener) }
        }
    }
}