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
        data class Downloading(val progress: Int, val currentSize: Long, val size: Long) : State() {
            val currentSizeInMb: Float = currentSize / 1000_000F
            val sizeInMb: Float = size / 1000_000F
        }

        data class Failed(val errorMessage: String?) : State()
    }

    data class Status(val videoId: String, val state: State)

    fun getDownloadingJobs(): Flow<List<DownloadingJob>>

    suspend fun enqueue(videoItem: VideoItem, playlists: List<MediaItemPlaylist>)

    suspend fun retry(videoId: String)
    suspend fun cancel(videoId: String)
    fun getDownloadingJobState(videoId: String): State
    fun observeStatus(): Flow<Status>
}