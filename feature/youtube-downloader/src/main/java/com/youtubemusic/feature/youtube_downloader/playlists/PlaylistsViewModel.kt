package com.youtubemusic.feature.youtube_downloader.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.youtubemusic.core.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(private val youTubeRepository: YouTubeRepository) : ViewModel() {
    val playlistsPageSource = Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { youTubeRepository.getYouTubePlaylistsPagingSource() }).flow.cachedIn(viewModelScope)
}