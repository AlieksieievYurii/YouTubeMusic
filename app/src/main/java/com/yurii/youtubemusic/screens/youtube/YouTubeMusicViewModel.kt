package com.yurii.youtubemusic.screens.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.screens.youtube.playlists.Playlist
import com.yurii.youtubemusic.services.downloader.MusicDownloaderService
import com.yurii.youtubemusic.services.downloader.ServiceConnection
import com.yurii.youtubemusic.services.media.MediaLibraryManager
import com.yurii.youtubemusic.source.GoogleAccount
import com.yurii.youtubemusic.source.PlaylistRepository
import com.yurii.youtubemusic.source.YouTubePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

abstract class VideoItemStatus(open val videoItem: Item) {
    class Download(override val videoItem: Item) : VideoItemStatus(videoItem)
    class Downloaded(override val videoItem: VideoItem, val size: Long) : VideoItemStatus(videoItem)
    class Downloading(override val videoItem: VideoItem, val currentSize: Long, val size: Long) : VideoItemStatus(videoItem)
    class Failed(override val videoItem: VideoItem, val error: Exception?) : VideoItemStatus(videoItem)
}

@HiltViewModel
class YouTubeMusicViewModel @Inject constructor(
    private val mediaLibraryManager: MediaLibraryManager,
    private val downloaderServiceConnection: ServiceConnection,
    private val youTubePreferences: YouTubePreferences,
    private val playlistRepository: PlaylistRepository,
    val youTubeAPI: YouTubeAPI,
    private val googleAccount: GoogleAccount
) : ViewModel() {
    sealed class Event {
        data class ShowFailedVideoItem(val videoItem: VideoItem, val error: Exception?) : Event()
        data class OpenPlaylistSelector(val videoItem: VideoItem, val playlists: List<MediaItemPlaylist>) : Event()
    }

    private val _videoItems: MutableStateFlow<PagingData<VideoItem>> = MutableStateFlow(PagingData.empty())
    val videoItems: StateFlow<PagingData<VideoItem>> = _videoItems

    private val _currentPlaylistId: MutableStateFlow<Playlist?> = MutableStateFlow(youTubePreferences.getCurrentYouTubePlaylist())
    val currentPlaylistId: StateFlow<Playlist?> = _currentPlaylistId

    private val _videoItemStatus = MutableSharedFlow<VideoItemStatus>()
    val videoItemStatus: SharedFlow<VideoItemStatus> = _videoItemStatus

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private var searchJob: Job? = null

    init {
        mediaLibraryManager.mediaStorage.deleteDownloadingMocks()

        viewModelScope.launch {
            mediaLibraryManager.event.collect {
                if (it is MediaLibraryManager.Event.ItemDeleted)
                    _videoItemStatus.emit(VideoItemStatus.Download(it.item))
            }
        }

        _currentPlaylistId.value?.let { loadVideoItems(it) }

        viewModelScope.launch {
            downloaderServiceConnection.downloadingReport.collectLatest { report ->
                when (report) {
                    is MusicDownloaderService.DownloadingReport.Successful -> {
                        val musicFile = mediaLibraryManager.mediaStorage.getMediaFile(report.videoItem)
                        sendVideoItemStatus(VideoItemStatus.Downloaded(report.videoItem, musicFile.length()))
                    }
                    is MusicDownloaderService.DownloadingReport.Failed -> sendVideoItemStatus(
                        VideoItemStatus.Failed(report.videoItem, report.error)
                    )
                }
            }
        }

        viewModelScope.launch {
            downloaderServiceConnection.downloadingProgress.collectLatest { progress ->
                sendVideoItemStatus(VideoItemStatus.Downloading(progress.first, progress.second.currentSize, progress.second.totalSize))
            }
        }
        downloaderServiceConnection.connect()
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
        downloaderServiceConnection.download(item, playlists)
        sendVideoItemStatus(VideoItemStatus.Downloading(item, 0, 0))
    }

    fun openCategorySelectorFor(videoItem: VideoItem) {
        viewModelScope.launch {
            _event.emit(Event.OpenPlaylistSelector(videoItem, playlistRepository.getPlaylists().first()))
        }
    }

    fun tryToDownloadAgain(videoItem: VideoItem) {
        downloaderServiceConnection.retryToDownload(videoItem)
        sendVideoItemStatus(VideoItemStatus.Downloading(videoItem, 0, 0))
    }

    fun cancelDownloading(item: VideoItem) {
        downloaderServiceConnection.cancelDownloading(item)
        sendVideoItemStatus(VideoItemStatus.Download(item))
    }

    fun delete(videoItem: VideoItem) {
        viewModelScope.launch {
            mediaLibraryManager.deleteItem(videoItem)
        }
    }

    fun showFailedItemDetails(videoItem: VideoItem) {
        sendEvent(Event.ShowFailedVideoItem(videoItem, downloaderServiceConnection.getError(videoItem)))
    }

    fun getItemStatus(videoItem: VideoItem): VideoItemStatus {
        val musicFile = mediaLibraryManager.mediaStorage.getMediaFile(videoItem)

        if (musicFile.exists())
            return VideoItemStatus.Downloaded(videoItem, musicFile.length())

        if (downloaderServiceConnection.isItemDownloading(videoItem)) {
            val progress = downloaderServiceConnection.getProgress(videoItem) ?: Progress.create()
            return VideoItemStatus.Downloading(videoItem, progress.currentSize, progress.totalSize)
        }

        if (downloaderServiceConnection.isDownloadingFailed(videoItem))
            return VideoItemStatus.Failed(videoItem, downloaderServiceConnection.getError(videoItem))

        return VideoItemStatus.Download(videoItem)
    }

    private fun sendVideoItemStatus(videoItemStatus: VideoItemStatus) = viewModelScope.launch {
        _videoItemStatus.emit(videoItemStatus)
    }

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