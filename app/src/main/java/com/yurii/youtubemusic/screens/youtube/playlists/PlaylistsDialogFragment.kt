package com.yurii.youtubemusic.screens.youtube.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.services.youtube.model.Playlist
import com.yurii.youtubemusic.R
import com.yurii.youtubemusic.databinding.DialogPlayListsBinding
import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import kotlinx.coroutines.flow.collectLatest

class PlaylistsDialogFragment private constructor() : DialogFragment() {
    private lateinit var binding: DialogPlayListsBinding
    private lateinit var playlistsAdapter: PlaylistsAdapter

    var currentPlayList: Playlist? = null
    lateinit var onSelectedPlaylist: (Playlist) -> Unit
    lateinit var youTubeAPI: YouTubeAPI

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_play_lists, null, false)
        playlistsAdapter = PlaylistsAdapter(currentPlayList, onSelectedPlaylist)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPlayLists.apply {
            adapter = playlistsAdapter
            layoutManager = LinearLayoutManager(context)
        }

        lifecycleScope.launchWhenCreated {
            Pager(config = PagingConfig(pageSize = 10), pagingSourceFactory = { PlaylistPagingSource(youTubeAPI) }).flow.collectLatest {
                playlistsAdapter.submitData(it)
            }
        }
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