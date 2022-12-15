package com.yurii.youtubemusic.screens.saved

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayoutMediator
import com.yurii.youtubemusic.screens.categories.CategoriesEditorActivity
import com.yurii.youtubemusic.screens.equalizer.EqualizerActivity
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentSavedMusicBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.utilities.TabFragment
import com.yurii.youtubemusic.utilities.requireApplication
import kotlinx.coroutines.flow.collectLatest


class SavedMusicFragment : TabFragment<FragmentSavedMusicBinding>(
    layoutId = R.layout.fragment_saved_music,
    titleStringId = R.string.label_fragment_title_saved_music,
    optionMenuId = R.menu.saved_musics_fragment_menu
) {
    private val viewModel: SavedMusicViewModel by viewModels { Injector.provideSavedMusicViewModel(requireApplication()) }

    override fun onClickOption(id: Int) {
        when (id) {
            R.id.item_add_edit_categories -> openCategoriesEditor()
            R.id.item_open_equalizer -> openEqualizerActivity()
        }
    }

    override fun onInflatedView(viewDataBinding: FragmentSavedMusicBinding) {
        lifecycleScope.launchWhenCreated {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.musicCategories.collectLatest {
                    when(it) {
                        is SavedMusicViewModel.State.Loaded -> initCategoriesLayout(it.allCategories)
                        SavedMusicViewModel.State.Loading -> {
                            // TODO Add some loading
                        }
                    }
                }
            }
        }
    }

    private fun openEqualizerActivity() {
        startActivity(Intent(requireContext(), EqualizerActivity::class.java))
    }

    private fun openCategoriesEditor() {
        val activity = CategoriesEditorActivity.create(requireContext())
        startActivityForResult(activity, CategoriesEditorActivity.REQUEST_CODE)
    }

    private fun initCategoriesLayout(categories: List<Category>) {
        binding.viewpager.adapter = CategoriesTabAdapter(this, categories)
        TabLayoutMediator(binding.categories, binding.viewpager) { tab, position ->
            tab.text = categories[position].name
        }.attach()
        binding.categories.visibility = View.VISIBLE
    }

    companion object {
        fun createInstance(): SavedMusicFragment = SavedMusicFragment()
    }
}
