package com.youtubemusic.core.downloader.youtube

import com.youtubemusic.core.data.repository.MediaRepository
import com.youtubemusic.core.data.repository.YouTubeRepository
import com.youtubemusic.core.model.MediaItemPlaylist
import javax.inject.Inject

class DownloadAllFromPlaylistUseCase @Inject constructor(
    private val downloadManager: DownloadManager,
    private val youTubeRepository: YouTubeRepository,
    private val mediaRepository: MediaRepository
) {
    suspend fun downloadAll(playlistId: String, appPlaylists: List<MediaItemPlaylist>) {
        youTubeRepository.getAllVideoItemsFromPlaylist(playlistId).forEach {
            if (!mediaRepository.exists(it.id))
                downloadManager.enqueue(it, appPlaylists)
        }
    }
}