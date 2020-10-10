package com.yurii.youtubemusic

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.mediaservice.PLAYBACK_STATE_MEDIA_ITEM
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.videoslist.MediaListAdapter
import com.yurii.youtubemusic.videoslist.MediaListAdapterController
import com.yurii.youtubemusic.viewmodels.mediaitems.MediaItemsViewModel

class MediaItemsFragment : Fragment() {
    private lateinit var viewModel: MediaItemsViewModel
    private lateinit var mediaItemsAdapterController: MediaListAdapterController

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
            mediaItemsAdapterController.setMediaItems(it)
        })

        viewModel.playbackState.observe(viewLifecycleOwner, Observer {
            if (it.state == PlaybackStateCompat.STATE_PLAYING || it.state == PlaybackStateCompat.STATE_PAUSED) {
                val mediaMetaData = it.extras!!.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)!!
                mediaItemsAdapterController.onChangePlaybackState(mediaMetaData, it)
            }
        })
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        val mediaItemsAdapter = MediaListAdapter(requireContext(), MediaListAdapterCallBack())
        mediaItemsAdapterController = mediaItemsAdapter
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(requireContext())
            this.adapter = mediaItemsAdapter
        }
    }

    private fun deleteMediaItem(mediaItem: MediaMetaData) {

    }

    private fun openCategoriesEditor(mediaItem: MediaMetaData) {

    }

    private inner class MediaListAdapterCallBack : MediaListAdapter.CallBack {
        override fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat? {
            val currentMediaItem = viewModel.playbackState.value!!.extras?.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)
            return if (mediaItem == currentMediaItem) viewModel.playbackState.value!! else null
        }

        override fun onOptionsClick(mediaItem: MediaMetaData, view: View) {
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.media_item_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_delete_media_item -> deleteMediaItem(mediaItem)
                    R.id.item_edit_categories -> openCategoriesEditor(mediaItem)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            popupMenu.show()
        }

        override fun onItemClick(mediaItem: MediaMetaData) {
            viewModel.playMusic(mediaItem)
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