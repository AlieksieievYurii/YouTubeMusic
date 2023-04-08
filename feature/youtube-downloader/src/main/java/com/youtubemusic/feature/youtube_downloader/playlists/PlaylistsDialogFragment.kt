package com.youtubemusic.feature.youtube_downloader.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.recyclerview.widget.LinearLayoutManager
import com.youtubemusic.core.common.PlaylistsAdapter
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.core.data.EmptyListException
import com.youtubemusic.core.model.YouTubePlaylist
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.DialogPlayListsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistsDialogFragment private constructor() : DialogFragment() {
    private lateinit var binding: DialogPlayListsBinding
    private lateinit var playlistsAdapter: PlaylistsAdapter

    var currentPlayList: YouTubePlaylist? = null
    lateinit var onSelectedPlaylist: (YouTubePlaylist) -> Unit
    lateinit var youTubePlaylistsPager: Pager<String, YouTubePlaylist>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistsAdapter = PlaylistsAdapter { selectedPlaylist ->
            onSelectedPlaylist.invoke(selectedPlaylist)
            dismiss()
        }

        playlistsAdapter.selectedPlaylist = currentPlayList

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
                youTubePlaylistsPager.flow.collectLatest {
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
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
        fun show(
            fragmentManager: FragmentManager,
            youTubePlaylistsPager: Pager<String, YouTubePlaylist>,
            currentPlayList: YouTubePlaylist?,
            onSelectedPlaylist: (YouTubePlaylist) -> Unit
        ) {
            PlaylistsDialogFragment().apply {
                this.currentPlayList = currentPlayList
                this.onSelectedPlaylist = onSelectedPlaylist
                this.youTubePlaylistsPager = youTubePlaylistsPager
            }.show(fragmentManager, "SelectionPlayListFragment")
        }
    }
}