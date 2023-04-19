package com.youtubemusic.feature.youtube_downloader.playlist_videos

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.ToolBarAccessor
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistVideosFragment : Fragment(R.layout.fragment_playlist_videos) {
    sealed class ViewState {
        object VideosLoaded : ViewState()
        object Loading : ViewState()
        object EmptyList : ViewState()
        object Error : ViewState()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        (requireActivity() as ToolBarAccessor).getToolbar().setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_log_out -> viewModel.signOut()
                R.id.item_open_download_manager -> openDownloadManager()
            }
            true
        }

        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            launch { viewModel.videoItems.collectLatest { listAdapter.submitData(it) } }
            launch { startHandlingListLoadState() }
            launch { startHandlingEvents() }
            launch { viewModel.videoItemStatus.collectLatest { listAdapter.updateItem(it) } }
        }

        binding.apply {
            btnTryAgain.setOnClickListener {
                binding.refresh.isEnabled = true
                listAdapter.retry()
            }
            refresh.setOnRefreshListener { listAdapter.refresh() }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.youtube_playlist_videos, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private suspend fun startHandlingEvents() = viewModel.event.collectLatest { event ->
        when (event) {
            is PlaylistVideosViewModel.Event.ShowFailedVideoItem -> showFailedVideoItem(event.videoItem, event.error)
            is PlaylistVideosViewModel.Event.OpenPlaylistSelector -> showDialogToSelectPlaylists(
                event.videoItem,
                event.playlists
            )
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


    private suspend fun startHandlingListLoadState() = listAdapter.loadStateFlow.collectLatest {
        when (it.refresh) {
            is LoadState.Loading -> {
                if (!binding.refresh.isRefreshing) {
                    binding.viewState = ViewState.Loading
                    binding.refresh.isEnabled = false
                }
            }
            is LoadState.NotLoading -> {
                binding.refresh.isRefreshing = false
                binding.refresh.isEnabled = true
                binding.viewState = ViewState.VideosLoaded
            }
            is LoadState.Error -> {
                binding.refresh.isRefreshing = false
                binding.refresh.isEnabled = false
                val loadStateError = it.refresh as LoadState.Error
                if (loadStateError.error is EmptyListException)
                    binding.viewState = ViewState.EmptyList
                else {
                    binding.viewState = ViewState.Error
                    binding.error.text =
                        loadStateError.error.message ?: getString(com.youtubemusic.core.common.R.string.label_no_error_message)
                }
            }
        }
    }


    private fun openDownloadManager() {
        startActivity(Intent(requireContext(), DownloadManagerActivity::class.java))
    }
}
