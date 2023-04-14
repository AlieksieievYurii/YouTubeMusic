package com.youtubemusic.feature.saved_music

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayoutMediator
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.feature.equalizer.EqualizerActivity
import com.youtubemusic.feature.playlist_editor.PlaylistEditorActivity
import com.youtubemusic.feature.download_manager.DownloadManagerActivity
import com.youtubemusic.feature.saved_music.databinding.FragmentSavedMusicBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class SavedMusicFragment : Fragment(R.layout.fragment_saved_music) {
    private val binding: FragmentSavedMusicBinding by viewBinding()
    private val viewModel: SavedMusicViewModel by viewModels()



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity() as ToolBarAccessor).getToolbar().setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_add_edit_categories -> openCategoriesEditor()
                R.id.item_open_equalizer -> openEqualizerActivity()
                R.id.item_open_download_manager -> openDownloadManager()
            }
            true
        }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.saved_musics_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
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
}
