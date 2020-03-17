package com.yurii.youtubemusic.dialogplaylists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.youtube.model.Playlist
import com.squareup.picasso.Picasso
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.PlaylistItemBinding
import java.lang.IllegalStateException

class PlayListsAdapter(private val playLists: List<Playlist>) :
    RecyclerView.Adapter<PlayListsAdapter.PlayListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val playListItem = DataBindingUtil.inflate<PlaylistItemBinding>(
            inflater,
            R.layout.playlist_item,
            parent,
            false
        )

        return PlayListViewHolder(playListItem.root)
    }

    override fun getItemCount(): Int = playLists.size

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        val binding = DataBindingUtil.getBinding<PlaylistItemBinding>(holder.playListItem)
        binding?.let {
            val youTubePlayList = playLists[position]

            it.title.text = youTubePlayList.snippet.title

            it.videosCount.text = it.root.context.resources.getString(
                R.string.label_videos_count,
                youTubePlayList.contentDetails.itemCount
            )

            Picasso.get().load(youTubePlayList.snippet.thumbnails.standard.url).into(it.image)
        } ?: throw IllegalStateException("PlayList binding item cannot be null")
    }

    class PlayListViewHolder(val playListItem: View) : RecyclerView.ViewHolder(playListItem)
}