package com.yurii.youtubemusic.services.downloader

import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.VideoItem
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class DownloadingJob(val mediaItem: MediaItem, val thumbnailUrl: String, val downloadingJobId: UUID)

interface DownloadManager {
    sealed class State {
        object Download : State()
        data class Downloaded(val size: Long) : State()
        data class Downloading( val currentSize: Long, val size: Long) : State()
        data class Failed(val errorMessage: String?) : State()
    }

    data class Status(val videoId: String, val status: State)

    fun getDownloadingJobs(): Flow<List<DownloadingJob>>

    suspend fun enqueue(videoItem: VideoItem, playlists: List<MediaItemPlaylist>)

    suspend fun retry(videoItem: VideoItem)
    suspend fun cancel(videoItem: VideoItem)
    fun getStatus(videoItem: VideoItem): Status
    fun observeStatus(): Flow<Status>
}