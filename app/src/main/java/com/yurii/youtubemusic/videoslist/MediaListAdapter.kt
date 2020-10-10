package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemMusicBinding
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.BaseViewHolder

interface MediaListAdapterController {
    fun onChangePlaybackState(mediaItem: MediaMetaData, playbackStateCompat: PlaybackStateCompat)
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

    fun setMediaItems(list: List<MediaMetaData>) {
        musics.clear()
        musics.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_music, parent, false))
    }

    override fun getItemCount(): Int = musics.size

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.setMusicItem(musics[position], callback)
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
    }

    override fun onChangePlaybackState(mediaItem: MediaMetaData, playbackStateCompat: PlaybackStateCompat) {
        TODO("Not yet implemented")
    }
}