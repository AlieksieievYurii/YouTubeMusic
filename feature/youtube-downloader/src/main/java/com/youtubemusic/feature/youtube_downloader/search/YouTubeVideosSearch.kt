package com.youtubemusic.feature.youtube_downloader.search

import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.FragmentYoutubeVideosSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class YouTubeVideosSearch : Fragment(R.layout.fragment_youtube_videos_search) {
    private val binding: FragmentYoutubeVideosSearchBinding by viewBinding()
    private val viewModel: YouTubeVideosSearchViewModel by viewModels()
}