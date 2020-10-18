package com.yurii.youtubemusic.mediaservice

import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.models.getMediaDescriptionCompat

import java.lang.Exception

class QueueException(message: String) : Exception(message)

class QueueProvider(private val mediaSession: MediaSessionCompat, private val musicsProvider: MusicsProvider) {
    private val queueItems = ArrayList<MediaMetaData>()
    private var playingCategory: Category? = null
    private var currentPosition = 0


    fun createQueue(mediaId: String, category: Category) {
        playingCategory = category
        val mediaItems = musicsProvider.getMediaItemsByCategory(category)

        queueItems.clear()
        queueItems.addAll(mediaItems)
        setStartItem(mediaId)

        mediaSession.setQueue(getQueueAsMediaSessionQueueItems())
        mediaSession.setQueueTitle("Queue from '$category' category")
    }

    fun setFirstPosition() {
        currentPosition = 0
    }

    fun setLastPosition() {
        currentPosition = queueItems.lastIndex
    }

    fun addToQueue(mediaMetaData: MediaMetaData) = queueItems.add(mediaMetaData)

    private fun setStartItem(mediaId: String) {
        queueItems.forEachIndexed { index, queueMediaItem ->
            if (mediaId == queueMediaItem.mediaId) {
                currentPosition = index
                return
            }
        }

        throw QueueException("Cannot find item: '$mediaId' in the queue")
    }


    fun queueExists(): Boolean = queueItems.isNotEmpty()

    fun getPlayingCategory(): Category = playingCategory ?: throw IllegalStateException("Queue is not initialized")

    fun addMediaItemToQueue(mediaId: String) {
        val metaData = musicsProvider.getMetaDataItem(mediaId)
        if (playingCategory == Category.ALL || playingCategory in metaData.categories)
            queueItems.add(metaData)
    }

    fun deleteMediaItemFromQueue(mediaId: String) {
        val currentMediaItem = queueItems[currentPosition]
        queueItems.find { it.mediaId == mediaId }?.run { queueItems.remove(this) }
        currentPosition = queueItems.indexOf(currentMediaItem)
    }

    fun contains(mediaId: String): Boolean = queueItems.find { it.mediaId == mediaId } != null

    fun getCurrentQueueItem() = queueItems[currentPosition]

    fun canMoveToNext() = currentPosition < queueItems.lastIndex

    fun canMoveToPrevious() = currentPosition > 0

    fun next(): MediaMetaData = queueItems.getOrNull(++currentPosition) ?: throw QueueException("Cannot move to next item")

    fun previous(): MediaMetaData = queueItems.getOrNull(--currentPosition) ?: throw QueueException("Cannot move to previous item")

    private fun getQueueAsMediaSessionQueueItems(): List<MediaSessionCompat.QueueItem> {
        var id = 0L
        return queueItems.map { MediaSessionCompat.QueueItem(it.getMediaDescriptionCompat(), id++) }
    }
}