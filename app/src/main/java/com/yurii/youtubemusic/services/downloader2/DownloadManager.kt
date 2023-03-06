package com.yurii.youtubemusic.services.downloader2

import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.VideoItem
import kotlinx.coroutines.flow.Flow

interface DownloadManager {
    sealed class State {
        object Download : State()
        data class Downloaded(val size: Long) : State()
        data class Downloading( val currentSize: Long, val size: Long) : State()
        data class Failed(val errorMessage: String?) : State()
    }

    data class Status(val videoId: String, val status: State)

    suspend fun enqueue(videoItem: VideoItem, playlists: List<MediaItemPlaylist>)
    suspend fun cancel(videoItem: VideoItem)
    fun getStatus(videoItem: VideoItem): Status
    fun observeStatus(): Flow<Status>
}