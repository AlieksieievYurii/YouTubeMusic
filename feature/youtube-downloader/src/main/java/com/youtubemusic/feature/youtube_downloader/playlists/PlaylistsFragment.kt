package com.youtubemusic.feature.youtube_downloader.playlists

import android.os.Bundle
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentYoutubePlaylistsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_youtube_playlists) {
    sealed class ViewState {
        object Loading : ViewState()
        object Loaded : ViewState()
        data class Error(val error: String) : ViewState()
    }

    private val binding: FragmentYoutubePlaylistsBinding by viewBinding()
    private val viewModel: PlaylistsViewModel by viewModels()
    private val listAdapter by lazy {
        YouTubePlaylistsAdapter {
            findNavController().navigate(PlaylistsFragmentDirections.actionPlaylistsFragmentToFragmentPlaylistVideos(it.id))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            launch { viewModel.playlistsPageSource.collectLatest { listAdapter.submitData(it) } }
            launch { startHandlingListLoadState() }
        }
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