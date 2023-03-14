package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.media.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class MediaCreator @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaStorage: MediaStorage
) {

    private val lock = Mutex()

    suspend fun registerDownloadingMediaItem(videoItem: VideoItem, playlists: List<MediaItemPlaylist>, downloadingJobId: UUID) =
        lock.withLock {
            if (mediaRepository.getMediaItem(videoItem.id) == null) {
                val createdMediaItem = registerMediaItem(videoItem, downloadingJobId)
                playlistRepository.assignMediaItemToPlaylists(createdMediaItem, playlists)
            } else {
                mediaRepository.updateDownloadingJobId(videoItem, downloadingJobId)
            }
        }

    suspend fun setMediaItemAsDownloaded(itemId: String) = withContext(Dispatchers.IO) {
        mediaRepository.setMediaItemAsDownloaded(itemId)
    }

    private suspend fun registerMediaItem(videoItem: VideoItem, downloadingJobId: UUID): MediaItem {
        val mediaFile = mediaStorage.getMediaFile(videoItem)
        val thumbnailFile = mediaStorage.getThumbnail(videoItem.id)

        val mediaItem = MediaItem(
            id = videoItem.id,
            title = videoItem.title,
            author = videoItem.author,
            durationInMillis = videoItem.durationInMillis,
            thumbnail = thumbnailFile,
            mediaFile = mediaFile,
            description = ""
        )

        mediaRepository.addDownloadingMediaItem(mediaItem, downloadingJobId, videoItem.normalThumbnail)

        return mediaItem
    }
}