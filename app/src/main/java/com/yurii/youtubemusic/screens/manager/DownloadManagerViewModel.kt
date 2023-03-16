package com.yurii.youtubemusic.screens.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.services.downloader.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.E

data class PlaylistSyncBind(val playlistName: String)
data class DownloadingVideoItemJob(val videoItemName: String, val videoItemId: String, val thumbnail: String)

@HiltViewModel
class DownloadManagerViewModel @Inject constructor(private val downloadManager: DownloadManager) : ViewModel() {
    sealed class Event {
        data class OpenFailedJobError(val videoId: String, val error: String?): Event()
    }
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    val downloadingJobs = downloadManager.getDownloadingJobs().map { downloadingJobs ->
        downloadingJobs.map { DownloadingVideoItemJob(it.mediaItem.title, it.mediaItem.id, it.thumbnailUrl) }
    }

    val downloadingStatus = downloadManager.observeStatus()

    fun getDownloadingJobStatus(videoId: String) = downloadManager.getDownloadingJobState(videoId)
    fun cancelDownloading(itemId: String) {
        viewModelScope.launch {
            downloadManager.cancel(itemId)
        }
    }

    fun openFailedJobError(itemId: String) {
        viewModelScope.launch {
            (downloadManager.getDownloadingJobState(itemId) as? DownloadManager.State.Failed)?.let {
                _events.emit(Event.OpenFailedJobError(itemId, it.errorMessage))
            }
        }
    }
}