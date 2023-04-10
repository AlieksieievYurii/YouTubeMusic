package com.youtubemusic.core.data.repository

import com.youtubemusic.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryDomain @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val mediaRepository: MediaRepository,
    private val mediaFileRepository: MediaFileRepository
) {

    private val lock = Mutex()

    fun getMediaItems(playlist: MediaItemPlaylist): Flow<List<MediaItem>> {
        return if (playlist.isDefault())
            mediaRepository.getOrderedMediaItems()
        else
            playlistRepository.getMediaItemsFor(playlist)
    }

    suspend fun changePosition(playlist: MediaItemPlaylist, mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        if (playlist.isDefault())
            mediaRepository.changePosition(mediaItem, from, to)
        else
            playlistRepository.changePositionInPlaylist(playlist, mediaItem, from, to)
    }

    suspend fun deleteMediaItem(item: Item) = lock.withLock {
        playlistRepository.removeItemFromPlaylists(item)
        mediaRepository.delete(item)
        mediaFileRepository.deleteMediaFiles(item)
    }

    suspend fun registerDownloadingMediaItem(videoItem: VideoItem, playlists: List<MediaItemPlaylist>, downloadingJobId: UUID) =
        lock.withLock {
            if (mediaRepository.getMediaItem(videoItem.id) == null) {
                val createdMediaItem = registerMediaItem(videoItem, downloadingJobId)
                playlistRepository.assignDownloadingMediaItemToPlaylists(createdMediaItem, playlists)
            } else {
                mediaRepository.updateDownloadingJobId(videoItem, downloadingJobId)
            }
        }

    suspend fun setMediaItemAsDownloaded(itemId: String) = withContext(Dispatchers.IO) {
        mediaRepository.setMediaItemAsDownloaded(itemId)
        playlistRepository.setMediaItemAsDownloaded(itemId)
    }

    private suspend fun registerMediaItem(videoItem: VideoItem, downloadingJobId: UUID): MediaItem {
        val mediaFile = mediaFileRepository.getMediaFile(videoItem)
        val thumbnailFile = mediaFileRepository.getThumbnail(videoItem.id)

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