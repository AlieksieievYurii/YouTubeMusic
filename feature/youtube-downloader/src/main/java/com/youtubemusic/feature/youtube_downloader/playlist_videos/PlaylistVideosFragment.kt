package com.youtubemusic.feature.youtube_downloader.playlist_videos

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.core.common.attachNumberBadge
import com.youtubemusic.core.common.ui.ErrorDialog
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.core.common.ui.SelectPlaylistsDialog
import com.youtubemusic.core.common.ui.showDeletionDialog
import com.youtubemusic.core.data.EmptyListException
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.feature.download_manager.DownloadManagerActivity
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.utils.VideoItemsListAdapter
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentPlaylistVideosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistVideosFragment : Fragment(R.layout.fragment_playlist_videos), MenuProvider {
    sealed class ViewState {
        object Loading : ViewState()
        object Ready : ViewState()
        data class Error(val error: String) : ViewState()
    }

    private val binding: FragmentPlaylistVideosBinding by viewBinding()
    internal val viewModel: PlaylistVideosViewModel by viewModels()

    private val listAdapter: VideoItemsListAdapter by lazy {
        VideoItemsListAdapter(object : VideoItemsListAdapter.Callback {
            override fun getDownloadingJobState(videoItem: VideoItem): DownloadManager.State = viewModel.getItemStatus(videoItem)
            override fun onDownload(videoItem: VideoItem) = viewModel.download(videoItem)
            override fun onDownloadAndAssignedCategories(videoItem: VideoItem) = viewModel.openCategorySelectorFor(videoItem)
            override fun onCancelDownloading(videoItem: VideoItem) = viewModel.cancelDownloading(videoItem)
            override fun onDelete(videoItem: VideoItem) = showConfirmationDialogToDeleteVideoItem(videoItem)
            override fun onShowErrorDetail(videoItem: VideoItem) = viewModel.showFailedItemDetails(videoItem)

        })
    }

    private val headerAdapter: PlaylistDetailsHeaderAdapter by lazy {
        PlaylistDetailsHeaderAdapter(object : PlaylistDetailsHeaderAdapter.Callback {
            override fun onDownloadAll() {
                viewModel.downloadAll()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)

        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ConcatAdapter(headerAdapter, listAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            })
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            launch { viewModel.videoItemStatus.collectLatest { listAdapter.updateItem(it) } }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            launch { viewModel.videoItems.collectLatest { listAdapter.submitData(it) } }
            launch { startHandlingListLoadState() }
            launch { startHandlingEvents() }
            launch { handleViewState() }
            launch { viewModel.downloadAllState.collectLatest { headerAdapter.downloadAllButtonState = it } }
        }

        binding.apply {
            btnTryAgain.setOnClickListener {
                viewModel.reloadPlaylistInformation()
                listAdapter.retry()
            }
            refresh.setOnRefreshListener {
                listAdapter.refresh()
                viewModel.reloadPlaylistInformation()
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        (requireActivity() as ToolBarAccessor).getToolbar()
            .attachNumberBadge(R.id.item_open_download_manager, viewLifecycleOwner, viewModel.downloadingJobsNumber)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.youtube_playlist_videos, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_log_out -> viewModel.signOut()
            R.id.item_open_download_manager -> openDownloadManager()
            else -> return false
        }
        return true
    }

    private suspend fun startHandlingEvents() = viewModel.event.collectLatest { event ->
        when (event) {
            is PlaylistVideosViewModel.Event.ShowFailedVideoItem -> showFailedVideoItem(event.videoItem, event.error)
            is PlaylistVideosViewModel.Event.OpenPlaylistSelector -> showDialogToSelectPlaylists(event.videoItem, event.playlists)
        }
    }

    private fun showDialogToSelectPlaylists(videoItem: VideoItem, playlists: List<MediaItemPlaylist>) {
        SelectPlaylistsDialog(requireContext(), playlists, emptyList()) { categories ->
            viewModel.download(videoItem, categories)
        }.show()
    }

    internal fun showConfirmationDialogToDeleteVideoItem(videoItem: VideoItem) {
        showDeletionDialog(requireContext(), R.string.dialog_confirm_deletion_music_title, R.string.dialog_confirm_deletion_music_message) {
            viewModel.delete(videoItem)
        }
    }

    private fun showFailedVideoItem(videoItem: VideoItem, error: String?) {
        ErrorDialog.create(error ?: getString(com.youtubemusic.core.common.R.string.label_no_error_message)).addListeners(
            onTryAgain = { viewModel.tryToDownloadAgain(videoItem) },
            onCancel = { viewModel.cancelDownloading(videoItem) })
            .show(requireActivity().supportFragmentManager, "ErrorDialog")
    }

    private suspend fun handleViewState() {
        viewModel.viewState.combine(listAdapter.loadStateFlow) { playlistInfoState, videosPagerState ->
            val noErrorMessage = getString(com.youtubemusic.core.common.R.string.label_no_error_message)

            val isPlaylistDetailsReady = playlistInfoState is PlaylistVideosViewModel.State.Ready
            val isVideosLoaded = videosPagerState.refresh is LoadState.NotLoading
            val isVideosLoadedButEmpty = (videosPagerState.refresh as? LoadState.Error)?.error is EmptyListException
            if (isPlaylistDetailsReady)
                headerAdapter.data = (playlistInfoState as PlaylistVideosViewModel.State.Ready).youTubePlaylistDetails

            when {
                binding.refresh.isRefreshing -> ViewState.Ready
                isPlaylistDetailsReady && (isVideosLoaded || isVideosLoadedButEmpty) -> ViewState.Ready
                playlistInfoState is PlaylistVideosViewModel.State.Error -> ViewState.Error(
                    playlistInfoState.exception.message ?: noErrorMessage
                )

                videosPagerState.refresh is LoadState.Error -> ViewState.Error(
                    (videosPagerState.refresh as LoadState.Error).error.message ?: noErrorMessage
                )

                else -> ViewState.Loading
            }
        }.distinctUntilChanged().collectLatest {
            binding.viewState = it
        }
    }

    private suspend fun startHandlingListLoadState() = listAdapter.loadStateFlow.collectLatest {
        binding.apply {
            if ((it.refresh as? LoadState.Error)?.error is EmptyListException) {
                labelEmptyPlaylist.isVisible = true
                videos.overScrollMode = View.OVER_SCROLL_NEVER
            } else {
                labelEmptyPlaylist.isVisible = false
                videos.overScrollMode = View.OVER_SCROLL_ALWAYS
            }
        }

        when (it.refresh) {
            is LoadState.Loading -> if (!binding.refresh.isRefreshing) binding.refresh.isEnabled = false
            is LoadState.NotLoading -> {
                binding.refresh.isRefreshing = false
                binding.refresh.isEnabled = true
            }

            is LoadState.Error -> {
                binding.refresh.isRefreshing = false
                binding.refresh.isEnabled = (it.refresh as LoadState.Error).error is EmptyListException
            }
        }
    }

    private fun openDownloadManager() {
        startActivity(Intent(requireContext(), DownloadManagerActivity::class.java))
    }
}
