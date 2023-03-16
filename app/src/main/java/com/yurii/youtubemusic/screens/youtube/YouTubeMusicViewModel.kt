package com.yurii.youtubemusic.screens.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.yurii.youtubemusic.services.downloader.DownloadManager
import com.yurii.youtubemusic.source.GoogleAccount
import com.yurii.youtubemusic.source.MediaLibraryDomain
import com.yurii.youtubemusic.source.PlaylistRepository
import com.yurii.youtubemusic.source.YouTubePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeMusicViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val youTubePreferences: YouTubePreferences,
    private val playlistRepository: PlaylistRepository,
    val youTubeAPI: YouTubeAPI,
    private val googleAccount: GoogleAccount,
    private val mediaLibraryDomain: MediaLibraryDomain,

) : ViewModel() {
    sealed class Event {
        data class ShowFailedVideoItem(val videoItem: VideoItem, val error: String) : Event()
        data class OpenPlaylistSelector(val videoItem: VideoItem, val playlists: List<MediaItemPlaylist>) : Event()
    }

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems

    private val _currentPlaylistId: MutableStateFlow<Playlist?> = MutableStateFlow(youTubePreferences.getCurrentYouTubePlaylist())
    val currentPlaylistId: StateFlow<Playlist?> = _currentPlaylistId

    val videoItemStatus: Flow<DownloadManager.Status> = downloadManager.observeStatus()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private var searchJob: Job? = null

    init {
        _currentPlaylistId.value?.let { loadVideoItems(it) }
    }

    fun signOut() {
        googleAccount.signOut()
        youTubePreferences.setCurrentYouTubePlaylist(null)
    }

    fun setPlaylist(playlist: Playlist) {
        youTubePreferences.setCurrentYouTubePlaylist(playlist)
        _currentPlaylistId.value = playlist
        loadVideoItems(playlist)
    }

    fun download(item: VideoItem, playlists: List<MediaItemPlaylist> = emptyList()) {
        viewModelScope.launch {
            downloadManager.enqueue(item, playlists)
        }
    }

    fun openCategorySelectorFor(videoItem: VideoItem) {
        viewModelScope.launch {
            _event.emit(Event.OpenPlaylistSelector(videoItem, playlistRepository.getPlaylists().first()))
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

    fun showFailedItemDetails(videoItem: VideoItem) {
        viewModelScope.launch {
            (downloadManager.getDownloadingJobState(videoItem.id) as? DownloadManager.State.Failed)?.let {
                sendEvent(Event.ShowFailedVideoItem(videoItem, it.errorMessage ?: "No error message"))
            }
        }
    }

    fun getItemStatus(videoItem: VideoItem) = downloadManager.getDownloadingJobState(videoItem.id)

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _event.emit(event)
    }

    private fun loadVideoItems(playlist: Playlist) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { VideosPagingSource(youTubeAPI, playlist.id) }).flow.cachedIn(viewModelScope)
                .collectLatest {
                    _videoItems.emit(it)
                }
        }
    }
}