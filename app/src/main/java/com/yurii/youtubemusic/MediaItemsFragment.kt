package com.yurii.youtubemusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.videoslist.MediaListAdapter
import com.yurii.youtubemusic.viewmodels.mediaitems.MediaItemsViewModel

class MediaItemsFragment : Fragment() {
    private lateinit var viewModel: MediaItemsViewModel
    private lateinit var mediaItemsAdapter: MediaListAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_media_items, container, false)
        val mediaItemsRecyclerView: RecyclerView = root.findViewById(R.id.media_items)
        val category: Category = requireArguments().getParcelable(EXTRA_CATEGORY)!!
        initRecyclerView(mediaItemsRecyclerView)
        initViewModel(category)
        return root
    }

    private fun initViewModel(category: Category) {
        viewModel = Injector.provideMediaItemsViewModel(requireContext(), category)
        viewModel.mediaItems.observe(viewLifecycleOwner, Observer {
            mediaItemsAdapter.setMediaItems(it)
        })
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        mediaItemsAdapter = MediaListAdapter(requireContext())
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
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