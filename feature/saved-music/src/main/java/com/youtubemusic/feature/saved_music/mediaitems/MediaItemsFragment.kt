package com.youtubemusic.feature.saved_music.mediaitems

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.requireParcelable
import com.youtubemusic.core.common.ui.SelectPlaylistsDialog
import com.youtubemusic.core.common.ui.showDeletionDialog
import com.youtubemusic.core.model.MediaItem
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.player.PlaybackState
import com.youtubemusic.feature.saved_music.R
import com.youtubemusic.feature.saved_music.ShareContentModalSheet
import com.youtubemusic.feature.saved_music.databinding.FragmentMediaItemsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MediaItemsFragment : Fragment(R.layout.fragment_media_items) {
    private val playlist: MediaItemPlaylist by lazy { requireArguments().requireParcelable(EXTRA_PLAYLIST) }

    @Inject
    lateinit var assistedFactory: MediaItemsViewModelAssistedFactory
    internal val viewModel: MediaItemsViewModel by viewModels { MediaItemsViewModel.Factory(assistedFactory, playlist) }
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
                playlist = if (playbackState.playlist != playlist) playbackState.playlist else null
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
        showDeletionDialog(requireContext(), R.string.dialog_confirm_deletion_music_title, R.string.dialog_confirm_deletion_music_message) {
            viewModel.deleteMediaItem(mediaItem)
        }
    }

    internal fun expandMoreOptionsFor(viewItem: View, mediaItem: MediaItem) {
        val popupMenu = PopupMenu(requireContext(), viewItem)
        popupMenu.menuInflater.inflate(R.menu.media_item_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_delete_media_item -> confirmRemovingMediaItem(mediaItem)
                R.id.item_edit_categories -> openCategoriesEditor(mediaItem)
                R.id.item_share -> openSharingSheet(mediaItem)
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popupMenu.show()
    }

    private fun openSharingSheet(mediaItem: MediaItem) {
        ShareContentModalSheet.show(mediaItem, requireActivity().supportFragmentManager)
    }
    private fun openCategoriesEditor(mediaItem: MediaItem) {
        lifecycleScope.launch {
            SelectPlaylistsDialog(
                requireContext(),
                viewModel.getPlaylists(),
                viewModel.getAssignedPlaylists(mediaItem)
            ) {
                viewModel.assignPlaylists(mediaItem, it)
            }.show()
        }
    }

    companion object {
        private const val EXTRA_PLAYLIST: String = "com.yurii.youtubemusic.media.items.playlist"
        fun create(category: MediaItemPlaylist): MediaItemsFragment = MediaItemsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_PLAYLIST, category)
            }
        }
    }
}