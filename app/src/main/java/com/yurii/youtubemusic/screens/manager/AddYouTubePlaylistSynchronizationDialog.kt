package com.yurii.youtubemusic.screens.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.DialogAddYoutubePlaylistSyncBinding
import com.yurii.youtubemusic.screens.youtube.LoaderViewHolder
import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import com.yurii.youtubemusic.screens.youtube.playlists.PlaylistsAdapter
import com.yurii.youtubemusic.source.PlaylistRepository
import com.yurii.youtubemusic.source.YouTubePlaylistSyncRepository
import com.yurii.youtubemusic.utilities.EmptyListException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddYouTubePlaylistSynchronizationDialog : DialogFragment() {
    @Inject
    lateinit var youTubeAPI: YouTubeAPI

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository

    private val playlistsAdapter = PlaylistsAdapter {
        moveToAppPlaylistsSelection()
    }.apply {
        val loader = LoaderViewHolder()
        withLoadStateHeaderAndFooter(loader, loader)
    }

    private val mediaItemPlaylistMultiChoiceAdapter = MediaItemPlaylistMultiChoiceAdapter()
    private lateinit var binding: DialogAddYoutubePlaylistSyncBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_add_youtube_playlist_sync, null, false)
        binding.youTubePlaylists.apply {
            adapter = playlistsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.cancel.setOnClickListener { dismiss() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            launch {
                Pager(
                    config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                    pagingSourceFactory = { ExcludingAlreadySyncPlaylistPagingSource(youTubeAPI, youTubePlaylistSyncRepository) }).flow.collectLatest {
                    playlistsAdapter.submitData(it)
                }
            }
            launch {
                playlistsAdapter.loadStateFlow.collectLatest {
                    binding.youTubePlaylists.isVisible = it.refresh is LoadState.NotLoading
                    binding.progressBar.isVisible = it.refresh is LoadState.Loading
                    binding.hintListIsEmpty.isVisible = it.refresh is LoadState.Error

                    if (it.refresh is LoadState.Error) {
                        binding.hintListIsEmpty.text = when (val error = (it.refresh as LoadState.Error).error) {
                            is MissingItems -> getString(R.string.hint_missing_items)
                            is EmptyListException -> getString(R.string.label_no_playlist)
                            else -> getString(R.string.label_error, error.message ?: "None")
                        }
                    }
                }
            }

            launch { initAppPlaylists() }
        }

        binding.back.setOnClickListener { moveToYouTubePlaylistSelection() }
    }

    private fun moveToYouTubePlaylistSelection() {
        binding.apply {
            stepView.go(0, true)
            back.visibility = View.INVISIBLE
            nextOrAdd.text = binding.root.context.getString(R.string.label_next)
            hint.text = binding.root.context.getString(R.string.hint_select_you_tube_playlist)
            nextOrAdd.setOnClickListener { moveToAppPlaylistsSelection() }
            youTubePlaylists.adapter = playlistsAdapter
            youTubePlaylists.isVisible = true
        }
    }

    private fun moveToAppPlaylistsSelection() {
        binding.apply {
            stepView.go(1, true)
            youTubePlaylists.adapter = mediaItemPlaylistMultiChoiceAdapter
            binding.nextOrAdd.isEnabled = true
            back.visibility = View.VISIBLE
            hint.text = binding.root.context.getString(R.string.hint_select_app_playlists)
            nextOrAdd.text = binding.root.context.getString(R.string.label_add)
            nextOrAdd.setOnClickListener { performAddingYouTubePlaylistSync() }
        }
    }

    private suspend fun initAppPlaylists() {
        playlistRepository.getPlaylists().collect {
            mediaItemPlaylistMultiChoiceAdapter.submitList(it)
        }
    }


    private fun performAddingYouTubePlaylistSync() {
        lifecycleScope.launch {
            youTubePlaylistSyncRepository.addYouTubePlaylistSynchronization(
                playlistsAdapter.selectedPlaylist!!,
                mediaItemPlaylistMultiChoiceAdapter.getSelectedItems()
            )
        }
    }
}