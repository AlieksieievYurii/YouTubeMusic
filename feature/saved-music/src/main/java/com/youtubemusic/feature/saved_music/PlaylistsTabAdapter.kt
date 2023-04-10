package com.youtubemusic.feature.saved_music

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.feature.saved_music.mediaitems.MediaItemsFragment

internal class PlaylistsTabAdapter(fragment: Fragment, private val categories: List<MediaItemPlaylist>) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = categories.size
    override fun createFragment(position: Int): Fragment = MediaItemsFragment.create(categories[position])
}