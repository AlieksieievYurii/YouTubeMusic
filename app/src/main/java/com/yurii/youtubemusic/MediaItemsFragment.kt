package com.yurii.youtubemusic

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yurii.youtubemusic.databinding.FragmentMediaItemsBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.SelectCategoriesDialog
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.videoslist.MediaListAdapter
import com.yurii.youtubemusic.videoslist.MediaListAdapterController
import com.yurii.youtubemusic.viewmodels.MainActivityViewModel
import com.yurii.youtubemusic.viewmodels.mediaitems.MediaItemsViewModel

class MediaItemsFragment : Fragment() {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var viewModel: MediaItemsViewModel
    private lateinit var mediaItemsAdapterController: MediaListAdapterController
    private lateinit var binding: FragmentMediaItemsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_media_items, container, false)
        initViewModel(requireArguments().getParcelable(EXTRA_CATEGORY)!!)
        initRecyclerView(binding.mediaItems)

        mainActivityViewModel.onMediaItemIsDeleted.observe(viewLifecycleOwner, Observer {
            mediaItemsAdapterController.removeItemWithId(it)
            checkWhetherMediaItemsAreEmpty()
        })
        mainActivityViewModel.onVideoItemHasBeenDownloaded.observe(viewLifecycleOwner, Observer {
            val metadata = viewModel.getMetaData(it.videoId)
            if (viewModel.category == Category.ALL || viewModel.category in metadata.categories) {
                mediaItemsAdapterController.addNewMediaItem(metadata)
                checkWhetherMediaItemsAreEmpty()
            }
        })

        mainActivityViewModel.onUpdateMediaItem.observe(viewLifecycleOwner, Observer {
            val newMediaItem = it

            if (viewModel.category == Category.ALL) {
                mediaItemsAdapterController.updateMediaItem(newMediaItem)
                return@Observer
            }

            if (viewModel.category in newMediaItem.categories)
                addOrUpdateMediaItem(newMediaItem)
            else
                mediaItemsAdapterController.removeItemWithId(newMediaItem.mediaId)
            checkWhetherMediaItemsAreEmpty()
        })

        return binding.root
    }

    private fun initViewModel(category: Category) {
        viewModel = Injector.provideMediaItemsViewModel(requireContext(), category)
        viewModel.mediaItems.observe(viewLifecycleOwner, Observer {
            binding.loadingBar.isVisible = false
            mediaItemsAdapterController.setMediaItems(it)
            checkWhetherMediaItemsAreEmpty()
        })

        viewModel.playbackState.observe(viewLifecycleOwner, Observer {
            if (it.state == PlaybackStateCompat.STATE_PLAYING || it.state == PlaybackStateCompat.STATE_PAUSED || it.state == PlaybackStateCompat.STATE_STOPPED) {
                mediaItemsAdapterController.onChangePlaybackState(it)
            }
        })
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        val mediaItemsAdapter = MediaListAdapter(requireContext(), viewModel.category, MediaListAdapterCallBack())
        mediaItemsAdapterController = mediaItemsAdapter
        recyclerView.apply {
            layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.bottom_lifting_animation)
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(requireContext())
            this.adapter = mediaItemsAdapter
        }
    }

    private fun checkWhetherMediaItemsAreEmpty() {
        if (mediaItemsAdapterController.isEmptyList()) {
            binding.noMediaItems.isVisible = true
            binding.mediaItems.isVisible = false
        } else {
            binding.noMediaItems.isVisible = false
            binding.mediaItems.isVisible = true
        }
    }

    private fun addOrUpdateMediaItem(mediaItem: MediaMetaData) {
        if (mediaItemsAdapterController.contains(mediaItem.mediaId))
            mediaItemsAdapterController.updateMediaItem(mediaItem)
        else
            mediaItemsAdapterController.addNewMediaItem(mediaItem)
    }

    private fun deleteMediaItem(mediaItem: MediaMetaData) {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_music_title,
            messageId = R.string.dialog_confirm_deletion_music_message,
            onConfirm = {
                viewModel.deleteMediaItem(mediaItem)
                mediaItemsAdapterController.removeItemWithId(mediaItem.mediaId)
                mainActivityViewModel.notifyMediaItemHasBeenDeleted(mediaItem.mediaId)
            }
        ).show(requireActivity().supportFragmentManager, "RequestToDeleteMediaItem")

    }

    private fun openCategoriesEditor(mediaItem: MediaMetaData) {
        SelectCategoriesDialog.selectCategories(requireContext(), mediaItem.categories) {
            mediaItem.categories.clear()
            mediaItem.categories.addAll(it)
            mainActivityViewModel.notifyMediaItemHasBeenModified(mediaItem)
        }
    }

    private inner class MediaListAdapterCallBack : MediaListAdapter.CallBack {
        override fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat? = viewModel.getPlaybackState(mediaItem)

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
            viewModel.onClickMediaItem(mediaItem)
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