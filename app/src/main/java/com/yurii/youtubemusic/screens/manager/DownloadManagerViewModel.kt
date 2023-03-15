package com.yurii.youtubemusic.screens.manager

import androidx.lifecycle.ViewModel
import com.yurii.youtubemusic.services.downloader.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class PlaylistSyncBind(val playlistName: String)
data class DownloadingVideoItemJob(val videoItemName: String, val videoItemId: String, val thumbnail: String)

@HiltViewModel
class DownloadManagerViewModel @Inject constructor(private val downloadManager: DownloadManager) : ViewModel() {
    val downloadingJobs = downloadManager.getDownloadingJobs().map { downloadingJobs ->
        downloadingJobs.map { DownloadingVideoItemJob(it.mediaItem.title, it.mediaItem.id, it.thumbnailUrl) }
    }

    val downloadingStatus = downloadManager.observeStatus()
}