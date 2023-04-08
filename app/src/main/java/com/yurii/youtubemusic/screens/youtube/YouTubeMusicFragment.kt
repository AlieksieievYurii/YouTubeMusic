package com.yurii.youtubemusic.screens.youtube


import android.content.Intent
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.TabFragment
import com.youtubemusic.core.common.ui.ErrorDialog
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.core.data.EmptyListException
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.core.model.YouTubePlaylist
import com.yurii.youtubemusic.databinding.FragmentYoutubeMusicBinding
import com.yurii.youtubemusic.R

import com.youtubemusic.feature.download_manager.DownloadManagerActivity
import com.yurii.youtubemusic.screens.youtube.playlists.PlaylistsDialogFragment
import com.youtubemusic.feature.download_manager.SelectPlaylistsDialog
import com.yurii.youtubemusic.ui.showDeletionDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class YouTubeMusicFragment : TabFragment<FragmentYoutubeMusicBinding>(
    layoutId = R.layout.fragment_youtube_music,
    titleStringId = R.string.label_fragment_title_youtube_musics,
    optionMenuId = R.menu.youtube_music_fragment_menu
) {
    sealed class ViewState {
        object NoSelectedPlaylist : ViewState()
        object VideosLoaded : ViewState()
        object Loading : ViewState()
        object EmptyList : ViewState()
        object Error : ViewState()
    }

    private val viewModel: YouTubeMusicViewModel by viewModels()

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

    override fun onClickOption(id: Int) {
        when (id) {
            R.id.item_log_out -> viewModel.signOut()
            R.id.item_open_download_manager -> openDownloadManager()
        }
    }

    override fun onInflatedView(viewDataBinding: FragmentYoutubeMusicBinding) {
        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            launch { startHandlingCurrentPlaylist() }
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
            btnSelectPlayList.setOnClickListener { openDialogToSelectPlaylist(viewModel.currentYouTubePlaylistId.value) }
            btnSelectPlayListFirst.setOnClickListener { openDialogToSelectPlaylist(null) }
            refresh.setOnRefreshListener { listAdapter.refresh() }
        }
    }

    private suspend fun startHandlingEvents() = viewModel.event.collectLatest { event ->
        when (event) {
            is YouTubeMusicViewModel.Event.ShowFailedVideoItem -> showFailedVideoItem(event.videoItem, event.error)
            is YouTubeMusicViewModel.Event.OpenPlaylistSelector -> showDialogToSelectPlaylists(
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

    private fun showConfirmationDialogToDeleteVideoItem(videoItem: VideoItem) {
        showDeletionDialog(requireContext(), R.string.dialog_confirm_deletion_music_title, R.string.dialog_confirm_deletion_music_message) {
            viewModel.delete(videoItem)
        }
    }

    private fun showFailedVideoItem(videoItem: VideoItem, error: String?) {
        ErrorDialog.create(error ?: getString(R.string.label_no_error_message)).addListeners(
            onTryAgain = { viewModel.tryToDownloadAgain(videoItem) },
            onCancel = { viewModel.cancelDownloading(videoItem) })
            .show(requireActivity().supportFragmentManager, "ErrorDialog")
    }

    private suspend fun startHandlingCurrentPlaylist() = viewModel.currentYouTubePlaylistId.collectLatest {
        binding.apply {
            if (it != null) {
                viewState = ViewState.Loading
                tvPlayListName.text = it.name
            } else {
                viewState = ViewState.NoSelectedPlaylist
            }
        }
    }

    private suspend fun startHandlingListLoadState() = listAdapter.loadStateFlow.collectLatest {
        when (it.refresh) {
            is LoadState.Loading -> if (!binding.refresh.isRefreshing) binding.viewState = ViewState.Loading
            is LoadState.NotLoading -> {
                binding.refresh.isRefreshing = false
                if (viewModel.currentYouTubePlaylistId.value != null)
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
                    binding.error.text = loadStateError.error.message ?: "None"
                }
            }
        }
    }

    private fun openDialogToSelectPlaylist(currentPlaylist: YouTubePlaylist?) = PlaylistsDialogFragment.show(
        requireActivity().supportFragmentManager,
        viewModel.getYouTubePlaylistsPager(), currentPlaylist, viewModel::setPlaylist
    )

    private fun openDownloadManager() {
        startActivity(Intent(requireContext(), DownloadManagerActivity::class.java))
    }

    companion object {
        fun createInstance() = YouTubeMusicFragment()
    }
}
