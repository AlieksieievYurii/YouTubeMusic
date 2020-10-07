package com.yurii.youtubemusic.mediaservice

import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Queue
import com.yurii.youtubemusic.models.toMediaCompatQueueItem

class QueueProvider(private val mediaSession: MediaSessionCompat, private val musicsProvider: MusicsProvider) {
    lateinit var queue: Queue

    fun createQueue(mediaId: String, category: Category) {
        val mediaItems = musicsProvider.getMediaItemsByCategory(category)

        queue = Queue.createQueue(mediaItems).also {
            it.setStartItem(mediaId)
        }

        mediaSession.setQueue(queue.toMediaCompatQueueItem())
        mediaSession.setQueueTitle("Queue from '$category' category")
    }
}