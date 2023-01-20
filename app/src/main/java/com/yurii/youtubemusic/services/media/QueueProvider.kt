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

/**
 * Provides encapsulated logic for queue of media items.
 * Before any operations the queue must be initialized via [createQueueFor]
 */
class QueueProvider(private val mediaSession: MediaSessionCompat, private val mediaStorage: MediaStorage) {
    private val queue = ArrayList<MediaItem>()
    private var playingCategory: Category? = null
    private var currentPlayingMediaItemPosition = 0

    /**
     * Represents looping of current playing media item
     */
    var isLooped = false

    /**
     * Returns true is the queue is initialized that means [createQueueFor] was called
     * and there are media items assigned to the category
     */
    val isInitialized: Boolean
        get() = playingCategory != null && queue.isNotEmpty()

    /**
     * Returns current playing category. You should call this only is the queue is initialized
     */
    val currentPlayingCategory: Category
        get() {
            assertQueueInitialization()
            return playingCategory!!
        }

    /**
     * Returns target item in the queue. You should call this only is the queue is initialized, otherwise exception will be thrown
     */
    val currentQueueItem: MediaItem
        get() {
            assertQueueInitialization()
            return queue[currentPlayingMediaItemPosition]
        }

    /**
     * Initializes new queue for given [category]. In simple words, takes all the media items assigned to [category] and build the
     * queue in the specified sequence. If the queue is already initialized with the same category, then the process is ignored.
     * By default, the first item is the list of [category] media items - is set as target queue item
     */
    suspend fun createQueueFor(category: Category) {
        if (playingCategory == category)
            return

        playingCategory = category
        queue.clear()
        queue.addAll(mediaStorage.getMediaItemsFor(category))

        mediaSession.setQueue(getQueueAsMediaSessionQueueItems())
        mediaSession.setQueueTitle("Queue from '$category' category")
    }

    /**
     * Changes the position of the given [mediaItem] in the queue
     */
    fun changePosition(mediaItem: MediaItem, from: Int, to: Int) {
        assertQueueInitialization()

        if (queue[from] != mediaItem)
            throw IllegalStateException("Can't change the position of $mediaItem in the queue")

        val currentPlayingMediaItem = currentQueueItem
        queue.move(from, to)
        currentPlayingMediaItemPosition = queue.indexOf(currentPlayingMediaItem)
    }

    /**
     * Replaces the current playing category with [category] if their id are the same
     */
    fun updateCategory(category: Category) {
        if (isInitialized && currentPlayingCategory.id == category.id) {
            playingCategory = category
            mediaSession.setQueueTitle("Queue from '$category' category")
        }
    }

    /**
     * Clears the queue
     */
    fun release() {
        queue.clear()
        mediaSession.setQueue(null)
        mediaSession.setQueueTitle(null)
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
            val currentMediaItem = currentQueueItem
            queue.remove(this)
            if (queue.isEmpty())
                release()
            else if (currentMediaItem.id != item.id)
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

    /**
     * Appends given [mediaItem] to the queue. Also, it checks if the given media item belongs the [currentPlayingCategory].
     * If the media item does not belong then nothing is happened.
     * This method only must be called if the queue [isInitialized]
     */
    suspend fun add(mediaItem: MediaItem) {
        val mediaItems = mediaStorage.getCategoryContainer(currentPlayingCategory).mediaItemsIds
        if (mediaItems.contains(mediaItem.id))
            queue.add(mediaItem)
    }

    /**
     * Sets the target media item if it is exists in the current queue.
     * This method only must be called if the queue [isInitialized]
     */
    fun setTargetMediaItem(mediaItemId: String) {
        assertQueueInitialization()
        queue.findIndex { it.id == mediaItemId }?.run {
            currentPlayingMediaItemPosition = this
        } ?: throw QueueProviderException("Can not find media item with id '$mediaItemId' in the queue")
    }

    fun next() {
        if (!isLooped)
            skipToNext()
    }

    fun skipToNext() {
        assertQueueInitialization()
        if (currentPlayingMediaItemPosition < queue.lastIndex)
            currentPlayingMediaItemPosition++
        else
            currentPlayingMediaItemPosition = 0
    }

    fun skipToPrevious() {
        assertQueueInitialization()
        if (currentPlayingMediaItemPosition > 0)
            currentPlayingMediaItemPosition--
        else
            currentPlayingMediaItemPosition = queue.lastIndex
    }

    private fun assertQueueInitialization() {
        if (!isInitialized)
            throw QueueProviderException("Queue is not initalized. Call createQueueFor firstly")
    }


    private fun getQueueAsMediaSessionQueueItems(): List<MediaSessionCompat.QueueItem> {
        var id = 0L
        return queue.map { MediaSessionCompat.QueueItem(it.getMediaDescriptionCompat(), id++) }
    }
}