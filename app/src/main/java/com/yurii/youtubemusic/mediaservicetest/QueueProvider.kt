package com.yurii.youtubemusic.mediaservicetest

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat


class QueueProvider(private val mediaSession: MediaSessionCompat, private val musicItemsProvider: MusicItemsProvider) {
    private var currentQueueIndex: Long = 0

    private var currentQueue: List<MediaSessionCompat.QueueItem>? = null

    fun isQueueEmpty(): Boolean = currentQueue.isNullOrEmpty()

    fun canMoveNext(): Boolean {
        currentQueue?.run {
            if (currentQueueIndex < this.size - 1)
                return true
        }
        return false
    }

    fun canMoveToPrevious(): Boolean {
        currentQueue?.run {
            if (currentQueueIndex > 0)
                return true
        }
        return false
    }

    fun getCurrentPlayingMusic(): MediaMetadataCompat? {
        if (isCurrentIndexPlayable()) {
            val currentQueueItem = currentQueue?.get(currentQueueIndex.toInt())
            return currentQueueItem?.run {
                musicItemsProvider.getMetaDataById(description.mediaId!!)
            }
        }
        return null
    }

    fun setNextQueueItem() {
        if (!isQueueEmpty()) {
            currentQueueIndex++
            if (currentQueueIndex >= currentQueue!!.size)
                currentQueueIndex = 0
        }
    }

    private fun isCurrentIndexPlayable(): Boolean = !currentQueue.isNullOrEmpty() && currentQueueIndex >= 0 && currentQueueIndex < currentQueue!!.size

    fun setSequentQueue() {
        currentQueueIndex = 0
        var index = 0L
        val queue = arrayListOf<MediaSessionCompat.QueueItem>()


        musicItemsProvider.getAllMusicItems().forEach {
            queue.add(MediaSessionCompat.QueueItem(it.description, index++))
        }
        currentQueue = queue
        mediaSession.setQueueTitle("SequentQueue")
        mediaSession.setQueue(queue)
    }
}