package com.yurii.youtubemusic.screens.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.services.downloader.DownloadManager
import com.yurii.youtubemusic.source.YouTubePlaylistSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadingVideoItemJob(val videoItemName: String, val videoItemId: String, val thumbnail: String)

@HiltViewModel
class DownloadManagerViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository
) : ViewModel() {
    sealed class Event {
        data class OpenFailedJobError(val videoId: String, val error: String?) : Event()
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    val downloadingJobs = downloadManager.getDownloadingJobs().map { downloadingJobs ->
        downloadingJobs.map { DownloadingVideoItemJob(it.mediaItem.title, it.mediaItem.id, it.thumbnailUrl) }
    }

    val youTubePlaylistSyncs = youTubePlaylistSyncRepository.youTubePlaylistSyncs

    val downloadingStatus = downloadManager.observeStatus()

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