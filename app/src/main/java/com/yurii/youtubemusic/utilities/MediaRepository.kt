package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.db.*
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.toMediaItems
import com.yurii.youtubemusic.services.media.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject


class MediaRepository @Inject constructor(private val mediaItemDao: MediaItemDao, private val mediaStorage: MediaStorage) {

    private val lock = Mutex()

    fun getOrderedMediaItems(): Flow<List<MediaItem>> = mediaItemDao.getAllSortedMediaItems().map { it.toMediaItems() }

    suspend fun delete(mediaItem: MediaItem) = withContext(Dispatchers.IO) {
        mediaItemDao.deleteById(mediaItem.id)
        mediaStorage.deleteMediaFiles(mediaItem)
    }

    suspend fun addMediaItem(mediaItem: MediaItem) = lock.withLock {
        val incrementedPosition = mediaItemDao.getAvailablePosition() ?: 0
        mediaItemDao.insert(mediaItem.toEntity(incrementedPosition))
    }

    suspend fun changePosition(mediaItem: MediaItem, from: Int, to: Int) = lock.withLock {
        mediaItemDao.updatePosition(mediaItem.id, from, to)
    }
}