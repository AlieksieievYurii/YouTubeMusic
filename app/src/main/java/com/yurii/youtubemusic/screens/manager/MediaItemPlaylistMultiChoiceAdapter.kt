package com.yurii.youtubemusic.screens.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.youtubemusic.core.model.MediaItemPlaylist

class MediaItemPlaylistMultiChoiceAdapter :
    ListAdapter<MediaItemPlaylist, MediaItemPlaylistMultiChoiceAdapter.PlaylistViewHolder>(Comparator) {
    private val selectedItems = mutableMapOf<MediaItemPlaylist, Boolean>()

    private object Comparator : DiffUtil.ItemCallback<MediaItemPlaylist>() {
        override fun areItemsTheSame(oldItem: MediaItemPlaylist, newItem: MediaItemPlaylist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItemPlaylist, newItem: MediaItemPlaylist): Boolean {
            return oldItem == newItem
        }

    }

    override fun submitList(list: List<MediaItemPlaylist>?) {
        selectedItems.clear()
        list?.forEach { selectedItems[it] = false }
        super.submitList(list)
    }

    fun getSelectedItems(): List<MediaItemPlaylist> = selectedItems.filter { it.value }.map { it.key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(val view: View) : ViewHolder(view) {
        private val item = view.findViewById<CheckedTextView>(android.R.id.text1)
        fun bind(playlist: MediaItemPlaylist) {
            item.isChecked = selectedItems[playlist] ?: false
            item.text = playlist.name
            item.setOnClickListener {
                val check = !item.isChecked
                selectedItems[playlist] = check
                item.isChecked = check
            }
        }
    }
}