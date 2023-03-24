package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.db.*
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.toMediaItem
import com.yurii.youtubemusic.models.toMediaItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject


class MediaRepository @Inject constructor(private val mediaItemDao: MediaItemDao) {

    private val lock = Mutex()

    val mediaItemEntities = mediaItemDao.getMediaItemsEntities()

    val downloadingMediaItemEntities = mediaItemEntities.map { mediaItems -> mediaItems.filter { it.downloadingJobId != null } }

    suspend fun getMediaItem(mediaItemId: String): MediaItem? = mediaItemDao.getMediaItem(mediaItemId)?.toMediaItem()

    suspend fun getDownloadingMediaItemEntity(itemId: String): MediaItemEntity? = mediaItemDao.getDownloadingMediaItem(itemId)

    /**
     * Checks if the item with given id exists independently if it is downloading or already downloaded
     */
    suspend fun exists(itemId: String): Boolean {
        return getMediaItem(itemId) != null
    }

    fun getOrderedMediaItems(): Flow<List<MediaItem>> = mediaItemDao.getAllSortedMediaItems().map { it.toMediaItems() }

    suspend fun delete(item: Item) = withContext(Dispatchers.IO) {
        mediaItemDao.deleteAndCorrectPositions(item.id)
    }

    suspend fun addDownloadingMediaItem(mediaItem: MediaItem, downloadingJobId: UUID, thumbnailUrl: String) = lock.withLock {
        val incrementedPosition = mediaItemDao.getAvailablePosition() ?: 0
        mediaItemDao.insert(
            MediaItemEntity(
                mediaItemId = mediaItem.id,
                title = mediaItem.title,
                author = mediaItem.author,
                durationInMillis = mediaItem.durationInMillis,
                thumbnail = mediaItem.thumbnail,
                mediaFile = mediaItem.mediaFile,
                position = incrementedPosition,
                downloadingJobId = downloadingJobId,
                thumbnailUrl = thumbnailUrl
            )
        )
    }

    suspend fun changePosition(mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        mediaItemDao.updatePosition(mediaItem.id, from, to)
    }

    suspend fun setMediaItemAsDownloaded(itemId: String) {
        mediaItemDao.setMediaItemAsDownloaded(itemId)
    }

    suspend fun updateDownloadingJobId(item: Item, jobId: UUID) {
        mediaItemDao.updateDownloadingJobId(item.id, jobId)
    }
}