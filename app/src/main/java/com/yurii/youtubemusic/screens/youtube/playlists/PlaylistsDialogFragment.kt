package com.yurii.youtubemusic.screens.youtube.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.DialogPlayListsBinding
import com.yurii.youtubemusic.screens.youtube.LoaderViewHolder
import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import com.yurii.youtubemusic.utilities.EmptyListException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistsDialogFragment private constructor() : DialogFragment() {
    private lateinit var binding: DialogPlayListsBinding
    private lateinit var playlistsAdapter: PlaylistsAdapter

    var currentPlayList: Playlist? = null
    lateinit var onSelectedPlaylist: (Playlist) -> Unit
    lateinit var youTubeAPI: YouTubeAPI

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistsAdapter = PlaylistsAdapter(currentPlayList) { selectedPlaylist ->
            onSelectedPlaylist.invoke(selectedPlaylist)
            dismiss()
        }

        binding.rvPlayLists.apply {
            adapter = playlistsAdapter.apply {
                val loader = LoaderViewHolder()
                withLoadStateHeaderAndFooter(loader, loader)
            }
            layoutManager = LinearLayoutManager(context)
        }

        binding.refresh.setOnClickListener {
            playlistsAdapter.retry()
        }

        lifecycleScope.launchWhenCreated {
            launch {
                Pager(
                    config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                    pagingSourceFactory = { PlaylistsPagingSource(youTubeAPI) }).flow.collectLatest {
                    playlistsAdapter.submitData(it)
                }
            }
            launch {
                playlistsAdapter.loadStateFlow.collectLatest {
                    when (it.refresh) {
                        is LoadState.Loading -> showLoadingLayout()
                        is LoadState.NotLoading -> showListItemsLayout()
                        is LoadState.Error -> {
                            val loadStateError = it.refresh as LoadState.Error
                            if (loadStateError.error is EmptyListException)
                                showEmptyListLayout()
                            else
                                showErrorLayout(loadStateError.error.message ?: "None")
                        }
                    }
                }
            }
        }
    }

    private fun showListItemsLayout() = binding.apply {
        hintListIsEmpty.isVisible = false
        layoutError.isVisible = false
        progressBar.isVisible = false
        rvPlayLists.isVisible = true
    }

    private fun showLoadingLayout() = binding.apply {
        rvPlayLists.isVisible = false
        hintListIsEmpty.isVisible = false
        layoutError.isVisible = false
        progressBar.isVisible = true
    }

    private fun showErrorLayout(errorMessage: String) = binding.apply {
        rvPlayLists.isVisible = false
        progressBar.isVisible = false
        hintListIsEmpty.isVisible = false
        layoutError.isVisible = true
        tvError.text = getString(R.string.label_error, errorMessage)
    }

    private fun showEmptyListLayout() = binding.apply {
        rvPlayLists.isVisible = false
        progressBar.isVisible = false
        layoutError.isVisible = false
        hintListIsEmpty.isVisible = true
    }

    companion object {
        fun show(fragmentManager: FragmentManager, youTubeAPI: YouTubeAPI, currentPlayList: Playlist?, onSelectedPlaylist: (Playlist) -> Unit) {
            PlaylistsDialogFragment().apply {
                this.currentPlayList = currentPlayList
                this.onSelectedPlaylist = onSelectedPlaylist
                this.youTubeAPI = youTubeAPI
            }.show(fragmentManager, "SelectionPlayListFragment")
        }
    }
}