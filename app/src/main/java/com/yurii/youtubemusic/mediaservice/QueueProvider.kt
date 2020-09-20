package com.yurii.youtubemusic.mediaservice

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import java.lang.IllegalStateException

class QueueProvider(private val mediaSession: MediaSessionCompat, private val musicsProvider: MusicsProvider) {
    private var queue: MutableList<MediaSessionCompat.QueueItem>? = null
    private var currentQueueIndex = 0

    fun isQueueEmpty() = queue.isNullOrEmpty()

    fun setOnFirstPosition() {
        currentQueueIndex = 0
    }

    fun setOnLastPosition() {
        currentQueueIndex = queue?.lastIndex ?: throw IllegalStateException("Cannot call setOnLastPosition on null queue")
    }

    fun createQueue(mediaId: String, category: String) {
        var id = 0L
        val queue = musicsProvider.getMusicsByCategory(category).map { MediaSessionCompat.QueueItem(it.description, id++) }
        this.queue = queue.toMutableList()
        currentQueueIndex = getIndexOfMediaIdInQueue(mediaId)

        mediaSession.setQueue(queue)
        mediaSession.setQueueTitle("Queue from '$category' category")
    }

    private fun getIndexOfMediaIdInQueue(mediaId: String): Int {
        return queue?.run {
            val queueItem = find { it.description.mediaId == mediaId }
            return indexOf(queueItem)
        } ?: throw IllegalStateException("Cannot call getIndexOfMediaIdInQueue on null queue")
    }

    fun getCurrentQueueItemMetaData(): MediaMetadataCompat {
        val currentQueueItem = getCurrentQueueItem()
        return musicsProvider.getMetaData(currentQueueItem)
    }

    private fun getCurrentQueueItem(): MediaSessionCompat.QueueItem {
        return queue?.get(currentQueueIndex) ?: throw IllegalStateException("Cannot get current queue item because queue is empty")
    }

    fun moveToNextQueueItem(): MediaSessionCompat.QueueItem {
        currentQueueIndex++
        return queue?.get(currentQueueIndex) ?: throw IllegalStateException("Cannot can moveToNextQueueItem on empty queue")
    }

    fun moveToPreviousQueueItem(): MediaSessionCompat.QueueItem {
        currentQueueIndex--
        return queue?.get(currentQueueIndex) ?: throw IllegalStateException("Cannot can moveToPreviousQueueItem on empty queue")
    }

    fun canMoveToNext(): Boolean = !queue.isNullOrEmpty() && currentQueueIndex < queue!!.lastIndex


    fun canMoveToPrevious(): Boolean = !queue.isNullOrEmpty() && currentQueueIndex > 0
}