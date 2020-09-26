package com.yurii.youtubemusic

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.databinding.FragmentSavedMusicBinding
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.utilities.TabFragment
import com.yurii.youtubemusic.utilities.TabParameters
import com.yurii.youtubemusic.videoslist.MediaListAdapter
import com.yurii.youtubemusic.viewmodels.savedmusic.SavedMusicViewModel


/**
 * A simple [Fragment] subclass.
 */
class SavedMusicFragment : TabFragment() {
    private val savedMusicViewModel by viewModels<SavedMusicViewModel> {
        Injector.provideSavedMusicViewModel(requireContext())
    }


    private lateinit var mediaItemsAdapter: MediaListAdapter

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
        mediaItemsAdapter = MediaListAdapter(requireContext())
        binding.musics.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = mediaItemsAdapter
        }

        savedMusicViewModel.mediaItems.observe(viewLifecycleOwner, Observer {mediaItems ->
            mediaItemsAdapter.setMediaItems(mediaItems)
        })

        savedMusicViewModel.categoryItems.observe(viewLifecycleOwner, Observer {categoryItems ->
            Toast.makeText(requireContext(), categoryItems.toString(), Toast.LENGTH_LONG).show()
        })
    }


    companion object {
        fun createInstance(): SavedMusicFragment = SavedMusicFragment()
    }
}
