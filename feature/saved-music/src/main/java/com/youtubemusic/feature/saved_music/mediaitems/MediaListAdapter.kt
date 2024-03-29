package com.youtubemusic.feature.saved_music.mediaitems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.youtubemusic.core.common.getVisibleItems
import com.youtubemusic.core.common.setUniqueAnimatedDrawable
import com.youtubemusic.core.model.MediaItem
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.feature.saved_music.databinding.ItemMusicBinding
import java.util.*

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
    private var playlist: MediaItemPlaylist? = null

    private var modifiableList = mutableListOf<MediaItem>()

    fun resetState() {
        playingMediaItem = null
        isPlaying = false
        recyclerView.getVisibleItems<MusicViewHolder>().forEach { it.setNonPlayingState() }
    }

    fun moveItem(from: Int, to: Int) {
        Collections.swap(modifiableList, to, from)
        notifyItemMoved(from, to)
    }

    override fun submitList(list: List<MediaItem>?) {
        modifiableList = list.orEmpty().toMutableList()
        super.submitList(modifiableList)
    }

    /**
     * Finds the viewHolder associated to [mediaItem] and sets the state.
     * @param: [category] - playing category. When [category] is null, It means that the current item is playing from original category.
     * It is used to show that media item is playing from another category
     */
    fun setPlayingStateMediaItem(mediaItem: MediaItem, isPlaying: Boolean, playlist: MediaItemPlaylist?) {
        if (mediaItem != playingMediaItem)
            resetState()
        playingMediaItem = mediaItem
        this.isPlaying = isPlaying
        this.playlist = playlist
        findVisibleViewHolder(mediaItem)?.setPlayingState(isPlaying)
    }


    private fun findVisibleViewHolder(mediaItem: MediaItem): MusicViewHolder? {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        (layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()).forEach {
            if (it != -1 && getItem(it)?.id == mediaItem.id) {
                return recyclerView.findViewHolderForLayoutPosition(it) as MusicViewHolder
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
                container.setCardBackgroundColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                thumbnailState.isVisible = true
                if (isPlaying)
                    thumbnailState.setUniqueAnimatedDrawable(com.youtubemusic.core.common.R.drawable.anim_from_pause_to_play_48dp)
                else
                    thumbnailState.setUniqueAnimatedDrawable(com.youtubemusic.core.common.R.drawable.anim_from_play_to_pause_48dp)

                if (playlist != null) {
                    hintPlayingCategory.text =
                        root.context.getString(com.youtubemusic.core.common.R.string.label_playing_from, playlist?.name)
                    hintPlayingCategory.isVisible = true
                } else
                    hintPlayingCategory.isVisible = false
            }
        }

        fun setNonPlayingState() {
            itemMusicBinding.apply {
                hintPlayingCategory.isVisible = false
                container.setCardBackgroundColor(ContextCompat.getColor(itemMusicBinding.container.context, android.R.color.white))
                thumbnailState.isVisible = false
            }
        }
    }
}