package com.youtubemusic.feature.youtube_downloader.playlist_videos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.youtubemusic.core.data.repository.*
import com.youtubemusic.core.downloader.youtube.DownloadAllFromPlaylistUseCase
import com.youtubemusic.core.downloader.youtube.DownloadManager
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import com.youtubemusic.core.model.YouTubePlaylistDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlaylistVideosViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val playlistRepository: PlaylistRepository,
    private val youTubeRepository: YouTubeRepository,
    private val googleAccount: GoogleAccount,
    state: SavedStateHandle,
    private val mediaLibraryDomain: MediaLibraryDomain,
    private val downloadAllFromPlaylistUseCase: DownloadAllFromPlaylistUseCase
) : ViewModel() {
    sealed class State {
        object Loading : State()
        data class Ready(val youTubePlaylistDetails: YouTubePlaylistDetails) : State()
        data class Error(val exception: Exception) : State()
    }

    enum class DownloadAllState {
        READY, PROCESSING, DISABLED, ERROR, DONE
    }

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

    private val youTubePlaylistId: String = state["playlist_id"] ?: throw IllegalStateException("playlist_id is required!")

    private val _viewState = MutableStateFlow<State>(State.Loading)
    val viewState = _viewState.asStateFlow()

    private val _downloadAllState = MutableStateFlow(DownloadAllState.READY)
    val downloadAllState = _downloadAllState.asStateFlow()

    val downloadingJobsNumber = downloadManager.getDownloadingJobs().map { it.size }

    init {
        loadVideoItems()
        loadPlaylistDetails()
    }

    fun reloadPlaylistInformation() {
        loadPlaylistDetails()
    }

    fun signOut() {
        googleAccount.signOut()
    }

    fun downloadAll() {
        _downloadAllState.value = DownloadAllState.PROCESSING
        viewModelScope.launch {
            try {
                downloadAllFromPlaylistUseCase.downloadAll(youTubePlaylistId, emptyList())
                _downloadAllState.value = DownloadAllState.DONE
            } catch (error: Exception) {
                _downloadAllState.value = DownloadAllState.ERROR
            }
        }
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

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _event.emit(event)
    }

    private fun loadPlaylistDetails() {
        viewModelScope.launch {
            try {
                val result = youTubeRepository.getPlaylistDetails(youTubePlaylistId)
                _downloadAllState.value = if (result.videosNumber == 0L) DownloadAllState.DISABLED else DownloadAllState.READY
                _viewState.value = State.Ready(result)
            } catch (error: Exception) {
                _viewState.value = State.Error(error)
            }
        }
    }

    private fun loadVideoItems() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Pager(config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { youTubeRepository.getYouTubePlaylistVideosPagingSource(youTubePlaylistId) })
                .flow.cachedIn(viewModelScope).collectLatest {
                    _videoItems.emit(it)
                }
        }
    }
}