package com.youtubemusic.feature.youtube_downloader.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.youtubemusic.core.common.SingleLiveEvent
import com.youtubemusic.core.data.repository.GoogleAccount
import com.youtubemusic.core.data.repository.MediaLibraryDomain
import com.youtubemusic.core.data.repository.PlaylistRepository
import com.youtubemusic.core.data.repository.YouTubeRepository
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.core.data.SearchFilterData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeVideosSearchViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val mediaLibraryDomain: MediaLibraryDomain,
    private val youTubeRepository: YouTubeRepository,
    private val playlistRepository: PlaylistRepository,
    private val googleAccount: GoogleAccount,
) : ViewModel() {
    sealed class Event {
        object NavigateToLoginScreen : Event()
        data class OpenPlaylistSelector(val videoItem: VideoItem, val playlists: List<MediaItemPlaylist>) : Event()
    }

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems
    private var searchJob: Job? = null

    val event = SingleLiveEvent<Event>()

    val numberOfDownloadingJobs = downloadManager.getDownloadingJobs().map { it.size }

    fun getItemStatus(videoItem: VideoItem) = downloadManager.getDownloadingJobState(videoItem.id)

    var searchFilter: SearchFilterData = SearchFilterData.DEFAULT

    fun download(item: VideoItem, playlists: List<MediaItemPlaylist> = emptyList()) {
        viewModelScope.launch {
            downloadManager.enqueue(item, playlists)
        }
    }

    fun openCategorySelectorFor(videoItem: VideoItem) {
        viewModelScope.launch {
            event.value = Event.OpenPlaylistSelector(videoItem, playlistRepository.getPlaylists().first())
        }
    }

    fun tryToDownloadAgain(videoItem: VideoItem) {
        viewModelScope.launch {
            downloadManager.retry(videoItem.id)
        }
    }

    fun cancelDownloading(item: VideoItem) {
        viewModelScope.launch {
            downloadManager.cancel(item.id)
        }
    }

    fun delete(videoItem: VideoItem) {
        viewModelScope.launch {
            mediaLibraryDomain.deleteMediaItem(videoItem)
        }
    }


    fun load() {
        if (googleAccount.isAuthenticatedAndAuthorized.value)
            search("")
        else
            event.value = Event.NavigateToLoginScreen
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { youTubeRepository.getYouTubeVideosPagingSource(query, searchFilter) }).flow.cachedIn(viewModelScope)
                .collectLatest {
                    _videoItems.emit(it)
                }
        }
    }

    fun logOut() {
        googleAccount.signOut()
        event.value = Event.NavigateToLoginScreen
    }
}