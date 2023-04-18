package com.youtubemusic.feature.youtube_downloader.playlists

import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentPlaylistsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {
    private val binding: FragmentPlaylistsBinding by viewBinding()
    private val viewModel: PlaylistsViewModel by viewModels()
}