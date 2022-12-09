package com.yurii.youtubemusic.screens.saved.mediaitems

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils.loadLayoutAnimation
import android.viewbinding.library.fragment.viewBinding
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentMediaItemsBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.ui.SelectCategoriesDialog
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.screens.main.MainActivityViewModel
import com.yurii.youtubemusic.utilities.MediaLibraryManager
import com.yurii.youtubemusic.utilities.requireParcelable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class MediaItemsFragment : Fragment(R.layout.fragment_media_items) {
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val viewModel: MediaItemsViewModel2 by viewModels {
        val category: Category = requireArguments().requireParcelable(EXTRA_CATEGORY)
        Injector.provideMediaItemsViewModel2(requireContext(), category)
    }
    private val binding: FragmentMediaItemsBinding by viewBinding()

    private val mediaListAdapter: MediaListAdapter by lazy {
        MediaListAdapter(object : MediaListAdapter.Callback {
            override fun onMediaItemClicked(mediaItem: MediaItem) {
                //viewModel.onClickMediaItem(mediaItem)
                viewModel.onClickMediaItem(mediaItem)
            }

            override fun onMediaItemMoreOptionsClicked(mediaItem: MediaItem, mediaItemView: View) =
                expandMoreOptionsFor(mediaItemView, mediaItem)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenCreated {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                launch { startObservingEvents() }
//                launch { startObservingMediaItems() }
//                launch { startObservingPlayingItem() }
                viewModel.mediaItemsStatus.collectLatest { mediaItemsStatus ->
                    binding.apply {
                        loadingBar.isVisible = mediaItemsStatus == MediaItemsViewModel2.MediaItemsStatus.Loading
                        mediaItems.isVisible = mediaItemsStatus is MediaItemsViewModel2.MediaItemsStatus.Loaded
                        noMediaItems.isVisible = mediaItemsStatus == MediaItemsViewModel2.MediaItemsStatus.NoMediaItems

                        if (mediaItemsStatus is MediaItemsViewModel2.MediaItemsStatus.Loaded)
                            mediaListAdapter.submitList(mediaItemsStatus.mediaItems)
                    }
                }
            }
        }

        binding.mediaItems.apply {
            layoutAnimation = loadLayoutAnimation(requireContext(), R.anim.bottom_lifting_animation)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mediaListAdapter
        }
    }
//
//    private suspend fun startObservingPlayingItem() = viewModel.playingMediaItem.collectLatest { playingMediaItem ->
//        mediaListAdapter.setPlayingMediaItem(playingMediaItem)
//    }
//
//    private suspend fun startObservingMediaItems() = viewModel.mediaItems.collectLatest {
//        when (it) {
//            is MediaItemsViewModel.MediaItemsStatus.Loading -> binding.apply {
//                loadingBar.isVisible = true
//                mediaItems.isVisible = false
//            }
//            is MediaItemsViewModel.MediaItemsStatus.Loaded -> binding.apply {
//                loadingBar.isVisible = false
//                mediaItems.isVisible = true
//                mediaListAdapter.submitList(it.mediaItems)
//            }
//            MediaItemsViewModel.MediaItemsStatus.NoMediaItems -> binding.apply {
//                noMediaItems.isVisible = true
//                mediaItems.isVisible = false
//            }
//        }
//    }
//
//    private suspend fun startObservingEvents() {
//        viewModel.event.collectLatest {
//            when (it) {
//                is MediaItemsViewModel.Event.ConfirmRemovingMediaItem -> confirmRemovingMediaItem(it.mediaMetaData)
//            }
//        }
//    }

    private fun confirmRemovingMediaItem(mediaItem: MediaItem) {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_music_title,
            messageId = R.string.dialog_confirm_deletion_music_message,
            onConfirm = {
                //viewModel.deleteMediaItem(mediaItem)
            }
        ).show(requireActivity().supportFragmentManager, "RequestToDeleteMediaItem")

    }

    private fun expandMoreOptionsFor(viewItem: View, mediaItem: MediaItem) {
        val popupMenu = PopupMenu(requireContext(), viewItem)
        popupMenu.menuInflater.inflate(R.menu.media_item_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_delete_media_item -> confirmRemovingMediaItem(mediaItem)
                R.id.item_edit_categories -> openCategoriesEditor(mediaItem)
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popupMenu.show()
    }

    private fun openCategoriesEditor(mediaItem: MediaItem) {
        SelectCategoriesDialog.selectCategories(requireContext(), mediaItem.categories) {
            mediaItem.categories.clear()
            mediaItem.categories.addAll(it)
            //mainActivityViewModel.notifyMediaItemHasBeenModified(mediaItem)
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