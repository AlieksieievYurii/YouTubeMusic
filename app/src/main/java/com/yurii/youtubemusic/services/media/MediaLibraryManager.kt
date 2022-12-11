package com.yurii.youtubemusic.utilities

import android.content.Context
import com.yurii.youtubemusic.models.Category

import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.services.media.MediaStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The class is responsible for doing modification operations. It implements event-based approach. In simple words
 * when you do some changes, an event is triggered and all its subscribers are informed.
 */
class MediaLibraryManager private constructor(val mediaStorage: MediaStorage) {
    sealed class Event {
        data class ItemDeleted(val item: Item) : Event()
        data class MediaItemIsAdded(val mediaItem: MediaItem, val assignedCategoriesIds: List<Int>) : Event()
        //TODO Add Delete and Update events and implement them here
    }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event: SharedFlow<Event> = _event

    suspend fun deleteItem(item: Item) {
        mediaStorage.deleteAllDataFor(item)
        _event.emit(Event.ItemDeleted(item))
    }

    /**
     * Creates Metadata file for give(downloaded) [videoItem].
     * Also appends [videoItem] to the given [additionalCustomCategories]
     */

    //TODO Make some lock to avoid asynchronous registering items
    suspend fun registerMediaItem(videoItem: VideoItem, additionalCustomCategories: List<Category>) {
        val mediaFile = mediaStorage.getMediaFile(videoItem)
        val thumbnailFile = mediaStorage.getThumbnail(videoItem)

        if (!mediaFile.exists())
            throw IllegalStateException("Media file does not exist")

        if (!thumbnailFile.exists())
            throw IllegalStateException("Thumbnail file does not exist")

        val mediaItem = MediaItem(
            id = videoItem.id,
            title = videoItem.title,
            author = videoItem.author,
            durationInMillis = videoItem.durationInMillis,
            description = videoItem.description,
            thumbnail = thumbnailFile,
            mediaFile = mediaFile
        )

        mediaStorage.createMediaMetadata(mediaItem)
        addMediaItemToDefaultCategory(videoItem)
        addMediaItemToAdditionalCategories(mediaItem, additionalCustomCategories)
        _event.emit(Event.MediaItemIsAdded(mediaItem, additionalCustomCategories.map { it.id }))
    }

    private fun addMediaItemToDefaultCategory(videoItem: VideoItem) {
        val defaultCategory = mediaStorage.getDefaultCategory()
        defaultCategory.appendItem(videoItem)
        mediaStorage.saveCategory(defaultCategory)
    }


    private fun addMediaItemToAdditionalCategories(mediaItem: MediaItem, additionalCustomCategories: List<Category>) {
        val availableCategories = mediaStorage.getCustomCategories()

        additionalCustomCategories.forEach { targetCategory ->
            val category = availableCategories.find { it.id == targetCategory.id }
            category?.let {
                it.appendItem(mediaItem)
                mediaStorage.saveCategory(category)
            }
        }

    }

    companion object {
        private var instance: MediaLibraryManager? = null

        fun getInstance(context: Context): MediaLibraryManager {
            if (instance == null)
                instance = MediaLibraryManager(MediaStorage(context))

            return instance!!
        }
    }
}