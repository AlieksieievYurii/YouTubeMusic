package com.youtubemusic.feature.youtube_downloader.playlist_videos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.common.getAttrColor
import com.youtubemusic.core.model.YouTubePlaylistDetails
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.HeaderPlaylistDetailsBinding

internal class PlaylistDetailsHeaderAdapter(private val callback: Callback) : RecyclerView.Adapter<PlaylistDetailsHeaderAdapter.Content>() {
    interface Callback {
        fun onDownloadAll()
    }

    private lateinit var recyclerView: RecyclerView

    var data: YouTubePlaylistDetails? = null
        set(value) {
            if (value != field)
                notifyItemChanged(0)
            field = value
        }

    var downloadAllButtonState = PlaylistVideosViewModel.DownloadAllState.READY
        set(value) {
            field = value
            (recyclerView.findViewHolderForAdapterPosition(0) as? Content)?.setDownloadButtonState(value)
        }

    private val onDownloadAllClickListener = View.OnClickListener { callback.onDownloadAll() }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Content {
        val inflater = LayoutInflater.from(parent.context)
        return Content(DataBindingUtil.inflate(inflater, R.layout.header_playlist_details, parent, false))
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: Content, position: Int) {
        data?.let { holder.onBind(it) }
    }

    inner class Content(private val binding: HeaderPlaylistDetailsBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context
        fun onBind(playlistDetails: YouTubePlaylistDetails) {
            binding.apply {
                playlist = playlistDetails
                setDownloadButtonState(downloadAllButtonState)
            }
        }

        fun setDownloadButtonState(downloadAllState: PlaylistVideosViewModel.DownloadAllState) {
            binding.downloadAll.apply {
                buttonLoading.isVisible = downloadAllButtonState == PlaylistVideosViewModel.DownloadAllState.PROCESSING
                icon.isVisible = downloadAllButtonState == PlaylistVideosViewModel.DownloadAllState.DONE
                when (downloadAllState) {
                    PlaylistVideosViewModel.DownloadAllState.READY -> {
                        button.setOnClickListener(onDownloadAllClickListener)
                        title.text = context.getString(R.string.label_download_all)
                        button.setCardBackgroundColor(context.getAttrColor(android.R.attr.colorPrimary))
                    }

                    PlaylistVideosViewModel.DownloadAllState.PROCESSING -> {
                        button.setOnClickListener(null)
                        title.text = context.getString(com.youtubemusic.core.common.R.string.label_loading)
                        button.setCardBackgroundColor(context.getAttrColor(android.R.attr.colorPrimary))
                    }

                    PlaylistVideosViewModel.DownloadAllState.DISABLED -> {
                        button.setOnClickListener(null)
                        title.text = context.getString(R.string.label_download_all)
                        val disabledColor = ResourcesCompat.getColor(context.resources, android.R.color.darker_gray, null)
                        button.setCardBackgroundColor(disabledColor)
                    }

                    PlaylistVideosViewModel.DownloadAllState.ERROR -> {
                        val errorColor = ResourcesCompat.getColor(context.resources, android.R.color.holo_red_light, null)
                        title.text = context.getString(com.youtubemusic.core.common.R.string.label_failed)
                        button.setCardBackgroundColor(errorColor)
                    }

                    PlaylistVideosViewModel.DownloadAllState.DONE -> {
                        button.setOnClickListener(null)
                        val doneColor = ResourcesCompat.getColor(context.resources, android.R.color.holo_green_light, null)
                        title.text = context.getString(com.youtubemusic.core.common.R.string.label_done)
                        button.setCardBackgroundColor(doneColor)
                    }
                }
            }
        }
    }

}