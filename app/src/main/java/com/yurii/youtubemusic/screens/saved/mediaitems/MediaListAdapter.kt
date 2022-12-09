package com.yurii.youtubemusic.screens.saved.mediaitems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemMusicBinding
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.utilities.getVisibleItems

class MediaListAdapter(private val callback: Callback) : ListAdapter<MediaItem, MediaListAdapter.MusicViewHolder>(Comparator) {
    interface Callback {
        fun onMediaItemClicked(mediaItem: MediaItem)
        fun onMediaItemMoreOptionsClicked(mediaItem: MediaItem, mediaItemView: View)
    }

    private object Comparator : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem
    }

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    /*
        playingMediaItem represents which mediaItem is playing or paused.
        isPlaying represents actual state: playing or paused
    */
    private var playingMediaItem: MediaItem? = null
    private var isPlaying = false

    fun resetState() {
        playingMediaItem = null
        isPlaying = false
        recyclerView.getVisibleItems<MusicViewHolder>().forEach { it.setNonPlayingState() }
    }


    fun setPlayingStateMediaItem(mediaItem: MediaItem, isPlaying: Boolean) {
        if (mediaItem != playingMediaItem)
            resetState()
        playingMediaItem = mediaItem
        this.isPlaying = isPlaying
        findVisibleViewHolder(mediaItem)?.setPlayingState(isPlaying)
    }


    private fun findVisibleViewHolder(mediaItem: MediaItem): MediaListAdapter.MusicViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1 && getItem(it)?.id == mediaItem.id) {
                return recyclerView.findViewHolderForLayoutPosition(it) as MediaListAdapter.MusicViewHolder
            }
        }
        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class MusicViewHolder(private val itemMusicBinding: ItemMusicBinding) : RecyclerView.ViewHolder(itemMusicBinding.root) {
        fun bind(mediaItem: MediaItem) {
            itemMusicBinding.apply {
                musicItem = mediaItem
                container.setOnClickListener { callback.onMediaItemClicked(mediaItem) }
                moreOptions.setOnClickListener { callback.onMediaItemMoreOptionsClicked(mediaItem, moreOptions) }
            }

            if (playingMediaItem == mediaItem)
                setPlayingState(isPlaying)
            else
                setNonPlayingState()
        }

        fun setPlayingState(isPlaying: Boolean) {
            itemMusicBinding.apply {
                val itemIcon = ContextCompat.getDrawable(root.context, if (isPlaying) R.drawable.ic_pause_24px else R.drawable.ic_play_24dp)
                container.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.lightGray))
                thumbnailState.isVisible = true
                thumbnailState.setImageDrawable(itemIcon)
            }
        }

        fun setNonPlayingState() {
            itemMusicBinding.apply {
                hintPlayingCategory.isVisible = false
                container.setCardBackgroundColor(ContextCompat.getColor(itemMusicBinding.container.context, R.color.white))
                thumbnailState.isVisible = false
            }
        }
    }
}