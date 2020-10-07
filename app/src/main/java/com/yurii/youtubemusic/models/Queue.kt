package com.yurii.youtubemusic.models

import android.support.v4.media.session.MediaSessionCompat
import java.lang.Exception

class QueueException(message: String) : Exception(message)

class Queue private constructor(fromItems: List<MediaMetaData>) {
    private var currentPosition = 0
    private val queueItems = ArrayList<MediaMetaData>().apply { addAll(fromItems) }

    fun getQueue(): List<MediaMetaData> = queueItems

    fun setFirstPosition() {
        currentPosition = 0
    }

    fun setLastPosition() {
        currentPosition = queueItems.lastIndex
    }

    fun setStartItem(mediaId: String) {
        queueItems.forEachIndexed { index, queueMediaItem ->
            if (mediaId == queueMediaItem.mediaId) {
                currentPosition = index
                return
            }
        }

        throw QueueException("Cannot find item: '$mediaId' in the queue")
    }

    fun getCurrentQueueItem() = queueItems[currentPosition]

    fun isQueueEmpty() = queueItems.isEmpty()

    fun canMoveToNext() = currentPosition < queueItems.lastIndex

    fun canMoveToPrevious() = currentPosition > 0

    fun next(): MediaMetaData = queueItems.getOrNull(++currentPosition) ?: throw QueueException("Cannot move to next item")

    fun previous(): MediaMetaData = queueItems.getOrNull(--currentPosition) ?: throw QueueException("Cannot move to previous item")

    companion object {
        fun createQueue(fromItems: List<MediaMetaData>): Queue {
            return Queue(fromItems)
        }
    }
}

fun Queue.toMediaCompatQueueItem(): List<MediaSessionCompat.QueueItem> {
    var id = 0L
    return this.getQueue().map { MediaSessionCompat.QueueItem(it.getMediaDescriptionCompat(), id++) }
}