package com.yurii.youtubemusic.utilities

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yurii.youtubemusic.screens.saved.mediaitems.MediaItemsFragment
import com.yurii.youtubemusic.models.Category

class CategoriesTabAdapter(fragment: Fragment, private val categories: List<Category>) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = categories.size
    override fun createFragment(position: Int): Fragment = MediaItemsFragment.create(categories[position])
}