package com.youtubemusic.feature.download_manager

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
import com.youtubemusic.core.common.PlaylistsAdapter
import com.youtubemusic.core.common.ui.LoaderViewHolder
import com.youtubemusic.core.data.AllYouTubePlaylistsSynchronized
import com.youtubemusic.core.data.EmptyListException
import com.youtubemusic.core.data.repository.PlaylistRepository
import com.youtubemusic.core.data.repository.YouTubePlaylistSyncRepository
import com.youtubemusic.core.data.repository.YouTubeRepository
import com.youtubemusic.feature.download_manager.databinding.DialogAddYoutubePlaylistSyncBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddYouTubePlaylistSynchronizationDialog : DialogFragment() {
    @Inject
    lateinit var youTubeRepository: YouTubeRepository

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository

    private val playlistsAdapter = PlaylistsAdapter { moveToAppPlaylistsSelection() }.apply {
        val loader = LoaderViewHolder()
        withLoadStateHeaderAndFooter(loader, loader)
    }

    private val mediaItemPlaylistMultiChoiceAdapter = MediaItemPlaylistMultiChoiceAdapter()
    private lateinit var binding: DialogAddYoutubePlaylistSyncBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_add_youtube_playlist_sync, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            launch { observeYouTubePlaylists() }
            launch { observeYouTubePlaylistsState() }
            launch { observeAppPlaylists() }
        }
        binding.apply {
            youTubePlaylists.adapter = playlistsAdapter
            youTubePlaylists.layoutManager = LinearLayoutManager(context)
            back.setOnClickListener { moveToYouTubePlaylistSelection() }
            cancel.setOnClickListener { dismiss() }
        }
    }

    private suspend fun observeYouTubePlaylists() {
        Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { youTubeRepository.getExcludingAlreadySyncPlaylistPagingSource() }).flow.collectLatest {
            playlistsAdapter.submitData(it)
        }
    }

    private suspend fun observeYouTubePlaylistsState() {
        playlistsAdapter.loadStateFlow.collectLatest {
            binding.youTubePlaylists.isVisible = it.refresh is LoadState.NotLoading
            binding.progressBar.isVisible = it.refresh is LoadState.Loading
            binding.hintListIsEmpty.isVisible = it.refresh is LoadState.Error

            if (it.refresh is LoadState.Error) {
                binding.hintListIsEmpty.text = when (val error = (it.refresh as LoadState.Error).error) {
                    is AllYouTubePlaylistsSynchronized -> getString(R.string.hint_all_playlists_synchronized)
                    is EmptyListException -> getString(R.string.label_no_playlist)
                    else -> getString(R.string.label_error, error.message ?: "None")
                }
            }
        }
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
            if (playlistsAdapter.itemCount == 0) {
                youTubePlaylists.isVisible = false
                hintListIsEmpty.isVisible = true
                hintListIsEmpty.text = getString(R.string.label_no_playlist)
            } else {
                youTubePlaylists.isVisible = true
                hintListIsEmpty.isVisible = false
            }
        }
    }

    private fun moveToAppPlaylistsSelection() {
        binding.apply {
            val context = root.context
            stepView.go(1, true)
            youTubePlaylists.adapter = mediaItemPlaylistMultiChoiceAdapter
            binding.nextOrAdd.isEnabled = true
            back.visibility = View.VISIBLE
            hint.text = context.getString(R.string.hint_select_app_playlists)
            nextOrAdd.text = context.getString(R.string.label_add)
            nextOrAdd.setOnClickListener { performAddingYouTubePlaylistSync() }

            if (mediaItemPlaylistMultiChoiceAdapter.itemCount == 0) {
                youTubePlaylists.isVisible = false
                hintListIsEmpty.isVisible = true
                hintListIsEmpty.text = context.getString(R.string.label_no_playlist)
            }
        }
    }

    private suspend fun observeAppPlaylists() {
        playlistRepository.getPlaylists().collect {
            mediaItemPlaylistMultiChoiceAdapter.submitList(it)
        }
    }


    private fun performAddingYouTubePlaylistSync() {
        lifecycleScope.launch {
            val playlist = playlistsAdapter.selectedPlaylist!!
            youTubePlaylistSyncRepository.addYouTubePlaylistSynchronization(
                playlist, mediaItemPlaylistMultiChoiceAdapter.getSelectedItems()
            )
            dismiss()
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager) {
            AddYouTubePlaylistSynchronizationDialog().show(fragmentManager, "AddYouTubePlaylistSynchronization")
        }
    }
}