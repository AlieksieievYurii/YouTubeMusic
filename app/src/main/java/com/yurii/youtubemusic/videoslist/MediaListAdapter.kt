package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemMusicBinding
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.BaseViewHolder

interface MediaListAdapterController {
    fun onChangePlaybackState(mediaMetaData: MediaMetaData, playbackStateCompat: PlaybackStateCompat)
}

class MediaListAdapter(context: Context, private val callback: CallBack) : RecyclerView.Adapter<MediaListAdapter.MusicViewHolder>(),
    MediaListAdapterController {
    interface CallBack {
        fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat
        fun onOptionsClick(mediaItem: MediaMetaData)
        fun onItemClick(mediaItem: MediaMetaData)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val musics = mutableListOf<MediaMetaData>()
    private lateinit var recyclerView: RecyclerView

    fun setMediaItems(list: List<MediaMetaData>) {
        musics.clear()
        musics.addAll(list)
        notifyDataSetChanged()
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_music, parent, false))
    }

    override fun getItemCount(): Int = musics.size

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.setMusicItem(musics[position], callback)
    }

    private fun findVideoItemView(mediaItem: MediaMetaData, onFound: ((MusicViewHolder) -> Unit)) {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION || musics.isEmpty())
                continue

            if (musics[position].mediaId == mediaItem.mediaId) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as MusicViewHolder
                onFound.invoke(viewHolder)
                return
            }
        }
    }

    private fun resetItemsState() {
        getVisibleItems().forEach {
            it.setNoneState()
        }
    }

    private fun getVisibleItems(): List<MusicViewHolder> = ArrayList<MusicViewHolder>().apply {
        for (index: Int in 0 until recyclerView.childCount) {
            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))

            if (position == RecyclerView.NO_POSITION || musics.isEmpty())
                continue

            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as MusicViewHolder
            add(viewHolder)
        }
    }

    override fun onChangePlaybackState(mediaMetaData: MediaMetaData, playbackStateCompat: PlaybackStateCompat) {
        resetItemsState()
        findVideoItemView(mediaMetaData) {
            when (playbackStateCompat.state) {
                PlaybackStateCompat.STATE_PLAYING -> it.setPlayingState()
                PlaybackStateCompat.STATE_PAUSED -> it.setPausedState()
            }
        }
    }

    class MusicViewHolder(private val itemMusicBinding: ItemMusicBinding) : BaseViewHolder(itemMusicBinding.root) {
        fun setMusicItem(mediaItem: MediaMetaData, callBack: CallBack) {
            itemMusicBinding.apply {
                this.musicItem = mediaItem
            }.executePendingBindings()

            itemMusicBinding.root.setOnClickListener {
                callBack.onItemClick(mediaItem)
            }
        }

        fun setPlayingState() {
            val context = itemMusicBinding.root.context
            itemMusicBinding.thumbnailState.apply {
                isVisible = true
                setImageDrawable(context.getDrawable(R.drawable.ic_play_24dp))
            }
        }

        fun setPausedState() {
            val context = itemMusicBinding.root.context
            itemMusicBinding.thumbnailState.apply {
                isVisible = true
                setImageDrawable(context.getDrawable(R.drawable.ic_pause_24px))
            }
        }

        fun setNoneState() {
            itemMusicBinding.thumbnailState.apply {
                isVisible = false
                setImageDrawable(null)
            }
        }
    }
}