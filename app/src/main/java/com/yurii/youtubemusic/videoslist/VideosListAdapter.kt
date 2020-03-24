package com.yurii.youtubemusic.videoslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemLoadingBinding
import com.yurii.youtubemusic.databinding.VideoItemBinding
import com.yurii.youtubemusic.models.VideoItem
import java.lang.IllegalStateException

interface VideoItemInterface {
    fun onItemClickDownload(videoItem: VideoItem)
    fun exists(videoItem: VideoItem): Boolean
    fun isLoading(videoItem: VideoItem): Boolean
}

const val VIEW_TYPE_LOADING: Int = 0
const val VIEW_TYPE_NORMAL: Int = 1

class VideosListAdapter(private val videoItemInterface: VideoItemInterface) :
    RecyclerView.Adapter<BaseViewHolder>() {
    val videos: MutableList<VideoItem> = mutableListOf()
    private var isLoaderVisible: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NORMAL -> VideoViewHolder(DataBindingUtil.inflate<VideoItemBinding>(inflater, R.layout.video_item, parent, false).root)
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
            val binding = DataBindingUtil.getBinding<VideoItemBinding>((holder as VideoViewHolder).videoItem)
            binding?.let { videoItemView ->
                videoItemView.title.text = videoItem.title
                videoItemView.channelTitle.text = videoItem.authorChannelTitle
                Picasso.get().load(videoItem.thumbnail).into(videoItemView.thumbnail)
                if (!videoItemInterface.exists(videoItem)) {
                    binding.download.setOnClickListener {
                        videoItemInterface.onItemClickDownload(videoItem)
                        setLoadingState(videoItemView)
                    }
                    if (videoItemInterface.isLoading(videoItem))
                        setLoadingState(videoItemView)
                    else
                        setReadyToDownloadState(videoItemView)
                } else videoItemView.download.visibility = View.GONE
            } ?: throw IllegalStateException("PlayListItem binding item cannot be null")
        }
    }

    private fun setReadyToDownloadState(binding: VideoItemBinding) {
        binding.download.visibility = View.VISIBLE
        binding.loading.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setLoadingState(binding: VideoItemBinding) {
        binding.download.visibility = View.GONE
        binding.loading.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    class VideoViewHolder(val videoItem: View) : BaseViewHolder(videoItem)
    class LoadingViewHolder(loadingView: View) : BaseViewHolder(loadingView)
}

abstract class BaseViewHolder(val view: View) : RecyclerView.ViewHolder(view)

abstract class PaginationListener(private val layoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {

    abstract fun isLastPage(): Boolean

    abstract fun isLoading(): Boolean

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (!isLoading() && !isLastPage()) {
            if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                loadMoreItems()
            }
        }
    }

    abstract fun loadMoreItems()
}