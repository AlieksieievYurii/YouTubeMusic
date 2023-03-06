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

    val mediaItemEntities =  mediaItemDao.getMediaItemsEntities()
    suspend fun getMediaItem(mediaItemId: String): MediaItem? = mediaItemDao.getMediaItem(mediaItemId)?.toMediaItem()

    fun getOrderedMediaItems(): Flow<List<MediaItem>> = mediaItemDao.getAllSortedMediaItems().map { it.toMediaItems() }

    suspend fun delete(item: Item) = withContext(Dispatchers.IO) {
        mediaItemDao.deleteAndCorrectPositions(item.id)
    }

    suspend fun addMediaItem(mediaItem: MediaItem, downloadingJobId: UUID) = lock.withLock {
        val incrementedPosition = mediaItemDao.getAvailablePosition() ?: 0
        mediaItemDao.insert(mediaItem.toEntity(incrementedPosition, downloadingJobId))
    }

    suspend fun addDownloadingMediaItem(mediaItem: MediaItem, downloadingJobId: UUID) {
        mediaItemDao.insert(mediaItem.toEntity(UNSPECIFIED_POSITION, downloadingJobId))
    }

    suspend fun changePosition(mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        mediaItemDao.updatePosition(mediaItem.id, from, to)
    }

    suspend fun setMediaItemAsDownloaded(itemId: String) {
        mediaItemDao.setMediaItemAsDownloaded(itemId, mediaItemDao.getAvailablePosition() ?: 0)
    }

    suspend fun updateDownloadingJobId(item: Item, jobId: UUID) {
        mediaItemDao.updateDownloadingJobId(item.id, jobId)
    }

    companion object {
        private const val UNSPECIFIED_POSITION = -1
    }
}