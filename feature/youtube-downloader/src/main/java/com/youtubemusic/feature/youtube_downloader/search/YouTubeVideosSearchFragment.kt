package com.youtubemusic.feature.youtube_downloader.search

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.youtubemusic.core.common.ToolBarAccessor
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.core.common.ui.SelectPlaylistsDialog
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.feature.download_manager.DownloadManagerActivity
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.utils.VideoItemsListAdapter
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentYoutubeVideosSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class YouTubeVideosSearchFragment : Fragment(R.layout.fragment_youtube_videos_search) {
    sealed class ViewState {
        object Loading : ViewState()
        object Loaded : ViewState()
        data class Error(val error: String) : ViewState()
    }

    private val binding: FragmentYoutubeVideosSearchBinding by viewBinding()
    internal val viewModel: YouTubeVideosSearchViewModel by viewModels()
    private val downloadManagerBudge by lazy { BadgeDrawable.create(requireContext()) }

    private val listAdapter: VideoItemsListAdapter by lazy {
        VideoItemsListAdapter(object : VideoItemsListAdapter.Callback {
            override fun getDownloadingJobState(videoItem: VideoItem): DownloadManager.State = viewModel.getItemStatus(videoItem)
            override fun onDownload(videoItem: VideoItem) = viewModel.download(videoItem)
            override fun onDownloadAndAssignedCategories(videoItem: VideoItem) = viewModel.openCategorySelectorFor(videoItem)
            override fun onCancelDownloading(videoItem: VideoItem) = viewModel.cancelDownloading(videoItem)
            override fun onDelete(videoItem: VideoItem) = viewModel.delete(videoItem)
            override fun onShowErrorDetail(videoItem: VideoItem) {}

        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        binding.videos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            }
        }

        binding.btnTryAgain.setOnClickListener { listAdapter.retry() }
        binding.refresh.setOnRefreshListener { listAdapter.refresh() }

        lifecycleScope.launchWhenStarted {
            launch { startHandlingListLoadState() }
            launch { observeNumberOfDownloadingJobs() }
            launch { viewModel.videoItems.collectLatest { listAdapter.submitData(it) } }
        }

        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                YouTubeVideosSearchViewModel.Event.NavigateToLoginScreen ->
                    findNavController().navigate(R.id.action_fragment_youtube_videos_search_to_authenticationFragment)
                is YouTubeVideosSearchViewModel.Event.OpenPlaylistSelector -> showDialogToSelectPlaylists(it.videoItem, it.playlists)
            }
        }
    }

    private suspend fun observeNumberOfDownloadingJobs() {
        viewModel.numberOfDownloadingJobs.collect {
            if (it != 0) {
                downloadManagerBudge.isVisible = true
                downloadManagerBudge.number = it
            } else
                downloadManagerBudge.isVisible = false
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private fun showDialogToSelectPlaylists(videoItem: VideoItem, playlists: List<MediaItemPlaylist>) {
        SelectPlaylistsDialog(requireContext(), playlists, emptyList()) { categories ->
            viewModel.download(videoItem, categories)
        }.show()
    }


    @Deprecated("Deprecated in Java")
    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.youtube_search_fragment_menu, menu)
        (requireActivity() as ToolBarAccessor).getToolbar().setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.item_log_out -> {
                    viewModel.logOut()
                    true
                }
                R.id.item_open_download_manager -> {
                    startActivity(Intent(requireContext(), DownloadManagerActivity::class.java))
                    true
                }
                R.id.item_open_playlists -> {
                    findNavController().navigate(R.id.action_fragment_youtube_videos_search_to_playlistsFragment)
                    true
                }
                else -> false
            }
        }
        menu.findItem(R.id.item_search).setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                viewModel.search("")
                return true
            }

        })
        (menu.findItem(R.id.item_search).actionView as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    viewModel.search(query)
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                }

            })
        }
        BadgeUtils.attachBadgeDrawable(
            downloadManagerBudge,
            (requireActivity() as ToolBarAccessor).getToolbar(),
            R.id.item_open_download_manager
        )
    }

    private suspend fun startHandlingListLoadState() = listAdapter.loadStateFlow.collectLatest {
        binding.apply {
            when (it.refresh) {
                is LoadState.Loading -> {
                    if (!refresh.isRefreshing) {
                        viewState = ViewState.Loading
                        refresh.isEnabled = false
                    }
                }
                is LoadState.NotLoading -> {
                    refresh.isRefreshing = false
                    refresh.isEnabled = true
                    viewState = ViewState.Loaded
                }
                is LoadState.Error -> {
                    refresh.isRefreshing = false
                    refresh.isEnabled = false
                    val loadStateError = it.refresh as LoadState.Error
                    viewState = ViewState.Error(
                        loadStateError.error.message ?: getString(com.youtubemusic.core.common.R.string.label_no_error_message)
                    )
                }
            }
        }
    }
}