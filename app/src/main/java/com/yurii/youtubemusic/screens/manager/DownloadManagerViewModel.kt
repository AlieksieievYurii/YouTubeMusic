package com.yurii.youtubemusic.screens.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.services.downloader.DownloadManager
import com.yurii.youtubemusic.source.PlaylistRepository
import com.yurii.youtubemusic.source.SyncManager
import com.yurii.youtubemusic.source.YouTubePlaylistSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadingVideoItemJob(val videoItemName: String, val videoItemId: String, val thumbnail: String)

@HiltViewModel
class DownloadManagerViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository,
    private val playlistRepository: PlaylistRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    sealed class Event {
        data class OpenFailedJobError(val videoId: String, val error: String?) : Event()
        data class OpenPlaylistsEditor(
            val youTubePlaylistId: String,
            val alreadySelectedPlaylists: List<MediaItemPlaylist>,
            val allPlaylists: List<MediaItemPlaylist>
        ) : Event()
    }

    private val _synchronizerState = MutableStateFlow<Boolean?>(null)
    val synchronizerState = _synchronizerState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    val downloadingJobs = downloadManager.getDownloadingJobs().map { downloadingJobs ->
        downloadingJobs.map { DownloadingVideoItemJob(it.mediaItem.title, it.mediaItem.id, it.thumbnailUrl) }
    }

    val youTubePlaylistSyncs = youTubePlaylistSyncRepository.youTubePlaylistSyncs

    val downloadingStatus = downloadManager.observeStatus()

    init {
        viewModelScope.launch {
            _synchronizerState.value = syncManager.isOn()
        }
    }

    fun enableAutomationYouTubeSynchronization(enabled: Boolean) {
        if (enabled)
            syncManager.turnOn()
        else
            syncManager.turnOff()
    }

    fun editAssignedPlaylists(youTubePlaylistSync: YouTubePlaylistSync) {
        viewModelScope.launch {
            _events.emit(
                Event.OpenPlaylistsEditor(
                    youTubePlaylistSync.youTubePlaylistId,
                    youTubePlaylistSync.mediaItemPlaylists,
                    playlistRepository.getPlaylists().first()
                )
            )
        }
    }

    fun reassignPlaylistsForSync(youTubePlaylistId: String, playlists: List<MediaItemPlaylist>) {
        viewModelScope.launch {
            youTubePlaylistSyncRepository.reassignPlaylists(youTubePlaylistId, playlists)
        }
    }

    fun getDownloadingJobStatus(videoId: String) = downloadManager.getDownloadingJobState(videoId)
    fun cancelDownloading(itemId: String) {
        viewModelScope.launch {
            downloadManager.cancel(itemId)
        }
    }

    fun retryDownloading(videoId: String) {
        viewModelScope.launch {
            downloadManager.retry(videoId)
        }
    }

    fun cancelAllDownloadingJobs() {
        viewModelScope.launch {
            downloadManager.getDownloadingJobs().first().forEach {
                downloadManager.cancel(it.mediaItem.id)
            }
        }
    }

    fun openFailedJobError(itemId: String) {
        viewModelScope.launch {
            (downloadManager.getDownloadingJobState(itemId) as? DownloadManager.State.Failed)?.let {
                _events.emit(Event.OpenFailedJobError(itemId, it.errorMessage))
            }
        }
    }

    fun deletePlaylistSynchronization(youTubePlaylistId: String) {
        viewModelScope.launch {
            youTubePlaylistSyncRepository.removeYouTubePlaylistSynchronization(youTubePlaylistId)
        }
    }
}