package com.youtubemusic.feature.youtube_downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.youtubemusic.core.data.repository.*
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.core.model.YouTubePlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlaylistVideosViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val youTubePreferences: YouTubePreferences,
    private val playlistRepository: PlaylistRepository,
    private val youTubeRepository: YouTubeRepository,
    private val googleAccount: GoogleAccount,
    private val mediaLibraryDomain: MediaLibraryDomain,
) : ViewModel() {
    sealed class Event {
        data class ShowFailedVideoItem(val videoItem: VideoItem, val error: String?) : Event()
        data class OpenPlaylistSelector(val videoItem: VideoItem, val playlists: List<MediaItemPlaylist>) : Event()
    }

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems

    val videoItemStatus: Flow<DownloadManager.Status> = downloadManager.observeStatus()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private var searchJob: Job? = null

    init {
        youTubePreferences.getCurrentYouTubePlaylist()?.let {
            loadVideoItems(it)
        }
    }

    fun signOut() {
        googleAccount.signOut()
        youTubePreferences.setCurrentYouTubePlaylist(null)
    }

    fun playlist() = youTubePreferences.getCurrentYouTubePlaylist()

    fun isLoggedOn(): Boolean {
        return googleAccount.isAuthenticatedAndAuthorized.value
    }

    fun setPlaylist(playlist: YouTubePlaylist) {
        youTubePreferences.setCurrentYouTubePlaylist(playlist)
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
                sendEvent(Event.ShowFailedVideoItem(videoItem, it.errorMessage))
            }
        }
    }

    fun getItemStatus(videoItem: VideoItem) = downloadManager.getDownloadingJobState(videoItem.id)

    fun getYouTubePlaylistsPager(): Pager<String, YouTubePlaylist> {
        return Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { youTubeRepository.getYouTubePlaylistsPagingSource() })

    }

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _event.emit(event)
    }

    private fun loadVideoItems(playlist: YouTubePlaylist) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { youTubeRepository.getYouTubeVideosPagingSource(playlist) }).flow.cachedIn(viewModelScope)
                .collectLatest {
                    _videoItems.emit(it)
                }
        }
    }
}