package com.yurii.youtubemusic.screens.saved

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.youtubemusic.core.model.MediaItemPlaylist
import com.yurii.youtubemusic.screens.saved.mediaitems.MediaItemsFragment

class PlaylistsTabAdapter(fragment: Fragment, private val categories: List<MediaItemPlaylist>) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = categories.size
    override fun createFragment(position: Int): Fragment = MediaItemsFragment.create(categories[position])
}