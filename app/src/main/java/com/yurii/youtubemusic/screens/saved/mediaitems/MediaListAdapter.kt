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
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.getVisibleItems

class MediaListAdapter(private val callback: Callback) : ListAdapter<MediaMetaData, MediaListAdapter.MusicViewHolder>(Comparator) {
    interface Callback {
        fun onMediaItemClicked(mediaItem: MediaMetaData)
        fun onMediaItemMoreOptionsClicked(mediaItem: MediaMetaData, mediaItemView: View)
    }

    private object Comparator : DiffUtil.ItemCallback<MediaMetaData>() {
        override fun areItemsTheSame(oldItem: MediaMetaData, newItem: MediaMetaData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaMetaData, newItem: MediaMetaData) =
            oldItem == newItem
    }

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    private var playingMediaItem: MediaItemsViewModel.PlayingMediaItem? = null

    fun setPlayingMediaItem(playingMediaItem: MediaItemsViewModel.PlayingMediaItem?) {
        playingMediaItem?.let {
            if (it != this.playingMediaItem)
                setClearAllVisibleItems()

            this.playingMediaItem = it
            findVisibleViewHolder(it.mediaMetaData)?.setPlaying(it.isPaused)
        } ?: setNoPlayingItem()
    }

    private fun setNoPlayingItem() {
        playingMediaItem = null
        setClearAllVisibleItems()
    }

    private fun setClearAllVisibleItems() = recyclerView.getVisibleItems<MusicViewHolder>().forEach { it.setNonPlayingState() }

    private fun findVisibleViewHolder(mediaItem: MediaMetaData): MediaListAdapter.MusicViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1 && getItem(it)?.mediaId == mediaItem.mediaId) {
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
        fun bind(mediaItem: MediaMetaData) {
            itemMusicBinding.apply {
                musicItem = mediaItem
                container.setOnClickListener { callback.onMediaItemClicked(mediaItem) }
                moreOptions.setOnClickListener { callback.onMediaItemMoreOptionsClicked(mediaItem, moreOptions) }
            }

            if (playingMediaItem?.mediaMetaData == mediaItem)
                setPlaying(playingMediaItem!!.isPaused)
            else
                setNonPlayingState()
        }

        fun setPlaying(isPaused: Boolean) {
            itemMusicBinding.apply {
                val itemIcon = ContextCompat.getDrawable(root.context, if (isPaused) R.drawable.ic_pause_24px else R.drawable.ic_play_24dp)
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