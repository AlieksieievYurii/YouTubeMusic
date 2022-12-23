package com.yurii.youtubemusic.services.media

import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.getMediaDescriptionCompat
import com.yurii.youtubemusic.utilities.findIndex
import com.yurii.youtubemusic.utilities.move
import java.lang.IllegalStateException

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

    /**
     * Changes the position of the given [mediaItem] in the queue
     */
    fun changePosition(mediaItem: MediaItem, from: Int, to: Int) {
        if (queue[from] != mediaItem)
            throw IllegalStateException("Can't change the position of $mediaItem in the queue")

        val currentPlayingMediaItem = getCurrentQueueItem()
        queue.move(from, to)
        currentPlayingMediaItemPosition = queue.indexOf(currentPlayingMediaItem)
    }

    /**
     * Replaces the current playing category with [category] if their id are the same
     */
    fun updateCategory(category: Category) {
        if (isInitialized && getCurrentPlayingCategory().id == category.id)
            playingCategory = category
    }

    /**
     * Clears the queue
     */
    fun release() {
        queue.clear()
        playingCategory = null
        currentPlayingMediaItemPosition = 0
    }

    /**
     * Returns true if the given [mediaItem] exists in the current queue.
     * Otherwise false, also when the queue is not initialized
     */
    fun contains(mediaItem: MediaItem): Boolean {
        if (!isInitialized)
            return false

        return queue.find { it.id == mediaItem.id } != null
    }

    /**
     * Removes an [item] from the queue if the queue has that. Otherwise ignores
     */
    fun removeFromQueueIfExists(item: Item) {
        queue.find { it.id == item.id }?.run {
            val currentMediaItem = getCurrentQueueItem()
            queue.remove(this)
            currentPlayingMediaItemPosition = queue.indexOf(currentMediaItem)
        }
    }

    /**
     * Appends [mediaItem] to the queue(if is initialized) if the queue does not contain this [mediaItem].
     * The function will be performed even if the [mediaItem] is not assigned to the current playing category
     */
    fun addToQueueIfDoesNotContain(mediaItem: MediaItem) {
        if (!isInitialized || contains(mediaItem))
            return

        queue.add(mediaItem)
    }

    suspend fun add(mediaItem: MediaItem) {
        playingCategory?.run {
            val mediaItems = mediaStorage.getCategoryContainer(this).mediaItemsIds
            if (mediaItems.contains(mediaItem.id))
                queue.add(mediaItem)
        }
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