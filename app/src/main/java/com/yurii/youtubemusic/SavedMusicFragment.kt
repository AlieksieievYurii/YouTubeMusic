package com.yurii.youtubemusic

import android.view.View
import androidx.fragment.app.Fragment
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayoutMediator
import com.yurii.youtubemusic.databinding.FragmentSavedMusicBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.utilities.TabFragment
import com.yurii.youtubemusic.utilities.TabParameters
import com.yurii.youtubemusic.videoslist.CategoriesTabAdapter
import com.yurii.youtubemusic.viewmodels.savedmusic.SavedMusicViewModel


/**
 * A simple [Fragment] subclass.
 */
class SavedMusicFragment : TabFragment() {
    private val savedMusicViewModel by viewModels<SavedMusicViewModel> {
        Injector.provideSavedMusicViewModel(requireContext())
    }

    private lateinit var viewPagerAdapter: CategoriesTabAdapter

    override fun getTabParameters(): TabParameters {
        return TabParameters(
            layoutId = R.layout.fragment_saved_music,
            title = requireContext().getString(R.string.label_fragment_title_saved_music),
            optionMenuId = R.menu.navigation_menu
        )
    }

    private lateinit var binding: FragmentSavedMusicBinding

    override fun onInflatedView(viewDataBinding: ViewDataBinding) {
        binding = viewDataBinding as FragmentSavedMusicBinding

        savedMusicViewModel.categoryItems.observe(viewLifecycleOwner, Observer { categoryItems ->
            initCategoriesLayout(categoryItems)
        })
    }

    private fun initCategoriesLayout(categories: List<Category>) {
        viewPagerAdapter = CategoriesTabAdapter(this, categories)
        binding.viewpager.adapter = viewPagerAdapter
        TabLayoutMediator(binding.categories, binding.viewpager) { tab, position ->
            tab.text = categories[position].name
        }.attach()
        binding.categories.visibility = View.VISIBLE
    }

    companion object {
        fun createInstance(): SavedMusicFragment = SavedMusicFragment()
    }
}
