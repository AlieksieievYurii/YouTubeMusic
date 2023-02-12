package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.db.*
import com.yurii.youtubemusic.models.MediaItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject


class MediaRepository @Inject constructor(private val mediaItemDao: MediaItemDao) {

    private val lock = Mutex()

    suspend fun getOrderedMediaItems(): List<MediaItemEntity> {
        return mediaItemDao.getAllSortedMediaItems()
    }

    suspend fun addMediaItem(mediaItem: MediaItem) = lock.withLock {
        val incrementedPosition = mediaItemDao.getAvailablePosition() ?: 0
        mediaItemDao.insert(mediaItem.toEntity(incrementedPosition))
    }

    suspend fun changePosition(mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        mediaItemDao.updatePosition(mediaItem.id, from, to)
    }
}