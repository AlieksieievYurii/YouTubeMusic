package com.yurii.youtubemusic.videoslist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.ItemMusicBinding
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.BaseViewHolder

class MediaListAdapter(context: Context) : RecyclerView.Adapter<MediaListAdapter.MusicViewHolder>() {
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
        holder.setMusicItem(musics[position])
    }

    class MusicViewHolder(private val itemMusicBinding: ItemMusicBinding) : BaseViewHolder(itemMusicBinding.root) {
        fun setMusicItem(mediaItem: MediaMetaData) {
            itemMusicBinding.apply {
                this.musicItem = mediaItem
            }.executePendingBindings()
        }
    }
}