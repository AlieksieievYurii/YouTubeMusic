package com.yurii.youtubemusic.screens.saved.mediaitems

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils.loadLayoutAnimation
import android.viewbinding.library.fragment.viewBinding
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.FragmentMediaItemsBinding
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.ui.ConfirmDeletionDialog
import com.yurii.youtubemusic.utilities.Injector
import com.yurii.youtubemusic.services.media.PlaybackState
import com.yurii.youtubemusic.ui.SelectCategoriesDialog2
import com.yurii.youtubemusic.utilities.requireApplication
import com.yurii.youtubemusic.utilities.requireParcelable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MediaItemsFragment : Fragment(R.layout.fragment_media_items) {
    private val category: Category by lazy { requireArguments().requireParcelable(EXTRA_CATEGORY) }
    private val viewModel: MediaItemsViewModel by viewModels { Injector.provideMediaItemsViewModel(requireApplication(), category) }
    private val binding: FragmentMediaItemsBinding by viewBinding()

    private val mediaListAdapter: MediaListAdapter by lazy {
        MediaListAdapter(object : MediaListAdapter.Callback {
            override fun onMediaItemClicked(mediaItem: MediaItem) {
                viewModel.onClickMediaItem(mediaItem)
            }

            override fun onMediaItemMoreOptionsClicked(mediaItem: MediaItem, mediaItemView: View) =
                expandMoreOptionsFor(mediaItemView, mediaItem)
        })
    }

    private val itemTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(MoveListItemHelper { a, b, c -> viewModel.onMove(a, b, c) })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenCreated {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { startObservingMediaItems() }
                launch { startObservingPlayingItem() }
            }
        }

        binding.mediaItems.apply {
            layoutAnimation = loadLayoutAnimation(requireContext(), R.anim.bottom_lifting_animation)
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mediaListAdapter
        }

        itemTouchHelper.attachToRecyclerView(binding.mediaItems)
    }

    private suspend fun startObservingPlayingItem() = viewModel.playbackState.collectLatest { playbackState ->
        when (playbackState) {
            PlaybackState.None -> mediaListAdapter.resetState()
            is PlaybackState.Playing -> mediaListAdapter.setPlayingStateMediaItem(
                playbackState.mediaItem,
                isPlaying = !playbackState.isPaused,
                category = if (playbackState.category != category) playbackState.category else null
            )
        }
    }

    private suspend fun startObservingMediaItems() = viewModel.mediaItemsStatus.collectLatest { mediaItemsStatus ->
        binding.apply {
            loadingBar.isVisible = mediaItemsStatus == MediaItemsViewModel.MediaItemsStatus.Loading
            mediaItems.isVisible = mediaItemsStatus is MediaItemsViewModel.MediaItemsStatus.Loaded
            noMediaItems.isVisible = mediaItemsStatus == MediaItemsViewModel.MediaItemsStatus.NoMediaItems

            if (mediaItemsStatus is MediaItemsViewModel.MediaItemsStatus.Loaded) {
                mediaListAdapter.submitList(mediaItemsStatus.mediaItems)
            }
        }
    }


    private fun confirmRemovingMediaItem(mediaItem: MediaItem) {
        ConfirmDeletionDialog.create(
            titleId = R.string.dialog_confirm_deletion_music_title,
            messageId = R.string.dialog_confirm_deletion_music_message,
            onConfirm = { viewModel.deleteMediaItem(mediaItem) }
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
        lifecycleScope.launch {
            SelectCategoriesDialog2(requireContext(), viewModel.getAllCustomCategories(), viewModel.getAssignedCustomCategoriesFor(mediaItem)) {
                viewModel.assignCustomCategoriesFor(mediaItem, it)
            }.show()
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