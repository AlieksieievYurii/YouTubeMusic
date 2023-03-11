package com.yurii.youtubemusic.screens.saved

import android.content.Intent
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayoutMediator
import com.yurii.youtubemusic.screens.categories.PlaylistEditorActivity
import com.yurii.youtubemusic.screens.equalizer.EqualizerActivity
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentSavedMusicBinding
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.screens.manager.DownloadManagerActivity
import com.yurii.youtubemusic.utilities.TabFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class SavedMusicFragment : TabFragment<FragmentSavedMusicBinding>(
    layoutId = R.layout.fragment_saved_music,
    titleStringId = R.string.label_fragment_title_saved_music,
    optionMenuId = R.menu.saved_musics_fragment_menu
) {
    private val viewModel: SavedMusicViewModel by viewModels()

    override fun onClickOption(id: Int) {
        when (id) {
            R.id.item_add_edit_categories -> openCategoriesEditor()
            R.id.item_open_equalizer -> openEqualizerActivity()
            R.id.item_open_download_manager -> openDownloadManager()
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
        startActivity(Intent(requireContext(), PlaylistEditorActivity::class.java))
    }

    private fun openDownloadManager() {
        startActivity(Intent(requireContext(), DownloadManagerActivity::class.java))
    }

    private fun initCategoriesLayout(categories: List<MediaItemPlaylist>) {
        binding.viewpager.adapter = PlaylistsTabAdapter(this, categories)
        TabLayoutMediator(binding.categories, binding.viewpager) { tab, position ->
            tab.text = categories[position].name
        }.attach()
        binding.categories.visibility = View.VISIBLE
    }

    companion object {
        fun createInstance(): SavedMusicFragment = SavedMusicFragment()
    }
}
