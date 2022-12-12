package com.yurii.youtubemusic.services.media

import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.getMediaDescriptionCompat
import com.yurii.youtubemusic.utilities.findIndex

class QueueProviderException(message: String) : Exception(message)

class QueueProvider(private val mediaSession: MediaSessionCompat, private val mediaStorage: MediaStorage) {
    private val queue = ArrayList<MediaItem>()
    private var playingCategory: Category? = null
    private var currentPlayingMediaItemPosition = 0

    val isInitialized: Boolean
        get() = playingCategory != null && queue.isNotEmpty()

    fun getCurrentPlayingCategory(): Category {
        return playingCategory ?: throw QueueProviderException("Can not get category")
    }

    fun getCurrentQueueItem(): MediaItem? {
        if (queue.isNotEmpty())
            return queue[currentPlayingMediaItemPosition]
        return null
    }

    fun removeFromQueueIfExists(item: Item) {
        queue.find { it.id == item.id }?.run {
            queue.remove(this)
            currentPlayingMediaItemPosition = 0
        }
    }

    fun add(mediaItem: MediaItem) {
        queue.add(mediaItem)
    }

    suspend fun createQueueFor(category: Category) {
        if (playingCategory == category)
            return

        playingCategory = category
        queue.clear()
        queue.addAll(mediaStorage.getMediaItemsFor(category))

        mediaSession.setQueue(getQueueAsMediaSessionQueueItems())
        mediaSession.setQueueTitle("Queue from '$category' category")
    }

    fun setTargetMediaItem(mediaItemId: String) {
        queue.findIndex { it.id == mediaItemId }?.run {
            currentPlayingMediaItemPosition = this
        } ?: throw QueueProviderException("Can not find media item with id '$mediaItemId' in the queue")
    }

    fun next() {
        skipToNext()
    }

    fun skipToNext() {
        validateIfQueueIsPrepared()
        if (currentPlayingMediaItemPosition < queue.lastIndex)
            currentPlayingMediaItemPosition++
        else
            currentPlayingMediaItemPosition = 0
    }

    fun skipToPrevious() {
        validateIfQueueIsPrepared()
        if (currentPlayingMediaItemPosition > 0)
            currentPlayingMediaItemPosition--
        else
            currentPlayingMediaItemPosition = queue.lastIndex
    }

    private fun validateIfQueueIsPrepared() {
        if (playingCategory == null || queue.isEmpty())
            throw QueueProviderException("Queue is not prepared")
    }


    private fun getQueueAsMediaSessionQueueItems(): List<MediaSessionCompat.QueueItem> {
        var id = 0L
        return queue.map { MediaSessionCompat.QueueItem(it.getMediaDescriptionCompat(), id++) }
    }
}