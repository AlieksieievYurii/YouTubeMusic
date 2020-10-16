package com.yurii.youtubemusic.mediaservice

import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Queue
import com.yurii.youtubemusic.models.toMediaCompatQueueItem

class QueueProvider(private val mediaSession: MediaSessionCompat, private val musicsProvider: MusicsProvider) {
    private var queue: Queue? = null
    private var playingCategory: Category? = null

    fun createQueue(mediaId: String, category: Category) {
        playingCategory = category
        val mediaItems = musicsProvider.getMediaItemsByCategory(category)

        queue = Queue.createQueue(mediaItems).also {
            it.setStartItem(mediaId)
        }

        mediaSession.setQueue(queue!!.toMediaCompatQueueItem())
        mediaSession.setQueueTitle("Queue from '$category' category")
    }

    fun getQueue(): Queue = queue ?: throw IllegalStateException("Queue is not initialized")

    fun queueExists(): Boolean = queue != null

    fun addMediaItemToQueue(mediaId: String) {
        val metaData = musicsProvider.getMetaDataItem(mediaId)
        if (playingCategory == Category.ALL || playingCategory in metaData.categories)
            getQueue().addToQueue(metaData)
    }
}