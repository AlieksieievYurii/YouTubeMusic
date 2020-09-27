package com.yurii.youtubemusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.databinding.FragmentMediaItemsBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.videoslist.MediaListAdapter
import com.yurii.youtubemusic.viewmodels.mediaitems.MediaItemsViewModel

class MediaItemsFragment : Fragment() {
    private lateinit var viewModel: MediaItemsViewModel
    private lateinit var mediaItemsAdapter: MediaListAdapter

    private lateinit var binding: FragmentMediaItemsBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_media_items, container, false)
        val category: Category = requireArguments().getParcelable(EXTRA_CATEGORY)!!
        binding.test.text = category.name
        initRecyclerView()
        initViewModel(category)
        return binding.root
    }

    private fun initViewModel(category: Category) {
        viewModel = Injector.provideMediaItemsViewModel(requireContext(), category)
        viewModel.mediaItems.observe(viewLifecycleOwner, Observer {
            mediaItemsAdapter.setMediaItems(it)
        })
    }

    private fun initRecyclerView() {
        mediaItemsAdapter = MediaListAdapter(requireContext())
        binding.mediaItems.apply {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(requireContext())
            this.adapter = mediaItemsAdapter
        }
    }

    companion object {
        private const val EXTRA_CATEGORY: String = "com.yurii.youtubemusic.media.items.category"
        fun create(category: Category): MediaItemsFragment = MediaItemsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_CATEGORY, category)
            }
        }
    }
}