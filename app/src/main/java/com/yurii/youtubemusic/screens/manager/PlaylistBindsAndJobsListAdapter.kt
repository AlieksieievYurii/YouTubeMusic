package com.yurii.youtubemusic.screens.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemHeadlineBinding
import com.yurii.youtubemusic.databinding.ItemJobBinding
import com.yurii.youtubemusic.databinding.ItemPlaylistSyncBindBinding
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.services.downloader.DownloadManager

sealed class AdapterData {
    data class PlaylistBind(val data: YouTubePlaylistSync) : AdapterData()
    data class Job(val data: DownloadingVideoItemJob) : AdapterData()
    data class Headline(val titleId: Int, val actionTextId: Int, val onAction: () -> Unit) : AdapterData()
}


class PlaylistBindsAndJobsListAdapter(private val callback: Callback) : ListAdapter<AdapterData, ViewHolder>(Comparator) {
    interface Callback {
        fun onAddSyncPlaylistBind()
        fun cancelAllDownloading()

        fun onClickPlaylistSync(view: View, playlistSync: YouTubePlaylistSync)
        fun openFailedJobError(itemId: String)

        fun cancelDownloading(itemId: String)
        fun getDownloadingJobState(id: String): DownloadManager.State
    }

    private val cashedPlaylistBinds = mutableListOf<AdapterData.PlaylistBind>()
    private val cashedDownloadingJobs = mutableListOf<AdapterData.Job>()

    private lateinit var recyclerView: RecyclerView

    private object Comparator : DiffUtil.ItemCallback<AdapterData>() {
        override fun areItemsTheSame(oldItem: AdapterData, newItem: AdapterData) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AdapterData, newItem: AdapterData) =
            oldItem == newItem
    }

    fun submitPlaylistBinds(list: List<YouTubePlaylistSync>) = synchronized(this) {
        setDataSources(cashedPlaylistBinds.apply {
            clear()
            cashedPlaylistBinds.addAll(list.map { AdapterData.PlaylistBind(it) })
        }, cashedDownloadingJobs)
    }

    fun submitDownloadingJobs(list: List<DownloadingVideoItemJob>) = synchronized(this) {
        setDataSources(cashedPlaylistBinds, cashedDownloadingJobs.apply {
            clear()
            addAll(list.map { AdapterData.Job(it) })
        })
    }

    fun updateDownloadingJobStatus(status: DownloadManager.Status) {
        findVisibleJobViewHolder(status.videoId)?.updateState(status.videoId, status.state)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdapterData.Job -> JOB_VIEW_TYPE
            is AdapterData.PlaylistBind -> PLAYLIST_BIND_VIEW_TYPE
            is AdapterData.Headline -> HEADLINE_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        PLAYLIST_BIND_VIEW_TYPE -> {
            val view = ItemPlaylistSyncBindBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            PlaylistBindViewHolder(view)
        }
        JOB_VIEW_TYPE -> {
            val view = ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            JobViewHolder(view)
        }
        HEADLINE_VIEW_TYPE -> {
            val view = ItemHeadlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeadlineViewHolder(view)
        }
        else -> throw IllegalStateException()
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (val data = getItem(position)) {
        is AdapterData.Job -> (holder as JobViewHolder).bind(data.data)
        is AdapterData.PlaylistBind -> (holder as PlaylistBindViewHolder).bind(data.data)
        is AdapterData.Headline -> (holder as HeadlineViewHolder).bind(data)
    }

    private fun setDataSources(playlistBinds: List<AdapterData.PlaylistBind>, downloadingJobs: List<AdapterData.Job>) {
        val result = mutableListOf<AdapterData>()
        if (playlistBinds.isNotEmpty()) {
            result.add(AdapterData.Headline(R.string.label_playlist_sync_binds, R.string.label_add, callback::onAddSyncPlaylistBind))
            result.addAll(playlistBinds)
        }

        if (downloadingJobs.isNotEmpty()) {
            result.add(AdapterData.Headline(R.string.label_downloading, R.string.label_cancel_all, callback::cancelAllDownloading))
            result.addAll(downloadingJobs)
        }

        submitList(result)
    }

    private fun findVisibleJobViewHolder(videoId: String): JobViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1) {
                val item = try {
                    getItem(it) as? AdapterData.Job
                } catch (_: IndexOutOfBoundsException) {
                    null
                }

                if (item != null && item.data.videoItemId == videoId)
                   return recyclerView.findViewHolderForLayoutPosition(it) as JobViewHolder
            }
        }
        return null
    }

    private inner class PlaylistBindViewHolder(private val binding: ItemPlaylistSyncBindBinding) : ViewHolder(binding.root) {
        fun bind(playlistBind: YouTubePlaylistSync) {
            binding.playlistName.text = playlistBind.youTubePlaylistName
            binding.appPlaylists.text = playlistBind.mediaItemPlaylists.joinToString(",") { it.name }
            binding.content.setOnClickListener { callback.onClickPlaylistSync(binding.root, playlistBind) }
        }
    }

    private inner class JobViewHolder(private val binding: ItemJobBinding) : ViewHolder(binding.root) {
        fun bind(downloadingJob: DownloadingVideoItemJob) {
            binding.apply {
                thumbnail.load(downloadingJob.thumbnail)
                videoItemName.text = downloadingJob.videoItemName
                videoItemId.text = downloadingJob.videoItemId
            }

            updateState(downloadingJob.videoItemId, callback.getDownloadingJobState(downloadingJob.videoItemId))
        }

        fun updateState(itemId: String, state: DownloadManager.State) {
            when (state) {
                DownloadManager.State.Download -> {
                    //Nothing because at that moment item will disappear from the list
                }
                is DownloadManager.State.Downloaded -> {
                    //Nothing because at that moment item will disappear from the list
                }
                is DownloadManager.State.Downloading -> binding.apply {
                    progress.isVisible = true
                    progress.progress = state.progress
                    sizeProgress.isVisible = true
                    sizeProgress.text = root.resources.getString(R.string.label_size_progress, state.currentSizeInMb, state.sizeInMb)
                    action.setImageResource(R.drawable.ic_baseline_cancel_36)
                    action.setOnClickListener { callback.cancelDownloading(itemId) }
                }
                is DownloadManager.State.Failed -> binding.apply {
                    progress.isVisible = false
                    sizeProgress.isVisible = false
                    action.setImageResource(R.drawable.ic_baseline_error_36)
                    action.setOnClickListener { callback.openFailedJobError(itemId) }
                }
            }
        }
    }

    private class HeadlineViewHolder(private val binding: ItemHeadlineBinding) : ViewHolder(binding.root) {
        private val resources = binding.root.resources
        fun bind(headline: AdapterData.Headline) {
            binding.title.text = resources.getText(headline.titleId)
            binding.action.setOnClickListener { headline.onAction() }
            binding.action.text = resources.getText(headline.actionTextId)
        }
    }

    companion object {
        private const val JOB_VIEW_TYPE = 1
        private const val PLAYLIST_BIND_VIEW_TYPE = 2
        private const val HEADLINE_VIEW_TYPE = 3
    }
}