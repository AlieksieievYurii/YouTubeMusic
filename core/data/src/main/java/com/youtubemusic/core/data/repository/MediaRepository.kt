package com.youtubemusic.core.data.repository

import com.youtubemusic.core.data.toMediaItem
import com.youtubemusic.core.data.toMediaItems
import com.youtubemusic.core.database.dao.MediaItemDao
import com.youtubemusic.core.database.models.MediaItemEntity
import com.youtubemusic.core.model.Item
import com.youtubemusic.core.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(private val mediaItemDao: MediaItemDao) {

    private val  lock = Mutex()

    val mediaItemCores = mediaItemDao.getMediaItemsEntities().map { it.toMediaItemCores() }

    val downloadingMediaItemEntities = mediaItemCores.map { mediaItemCores -> mediaItemCores.filter { it.downloadingJobUUID != null } }

    suspend fun getMediaItem(mediaItemId: String): MediaItem? = mediaItemDao.getMediaItem(mediaItemId)?.toMediaItem()

    suspend fun getMediaItemCore(itemId: String): MediaItemCore? = mediaItemDao.getDownloadingMediaItem(itemId)?.toMediaItemCore()

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

    suspend fun addDownloadingMediaItem(mediaItem: MediaItem, downloadingJobId: UUID, thumbnailUrl: String) {
        mediaItemDao.insert(
            MediaItemEntity(
                mediaItemId = mediaItem.id,
                title = mediaItem.title,
                author = mediaItem.author,
                durationInMillis = mediaItem.durationInMillis,
                thumbnail = mediaItem.thumbnail,
                mediaFile = mediaItem.mediaFile,
                position = MediaItemDao.UNSPECIFIED_POSITION,
                downloadingJobId = downloadingJobId,
                thumbnailUrl = thumbnailUrl
            )
        )
    }

    suspend fun changePosition(mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        mediaItemDao.updatePosition(mediaItem.id, from, to)
    }

    suspend fun setMediaItemAsDownloaded(itemId: String)  = lock.withLock {
        val incrementedPosition = mediaItemDao.getAvailablePosition() ?: 0
        mediaItemDao.setMediaItemAsDownloaded(itemId, incrementedPosition)
    }

    suspend fun updateDownloadingJobId(item: Item, jobId: UUID) {
        mediaItemDao.updateDownloadingJobId(item.id, jobId)
    }
}