package com.yurii.youtubemusic.services.media

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.Category

import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * The class is responsible for doing modification operations. It implements event-based approach. In simple words
 * when you do some changes, an event is triggered and all its subscribers are informed.
 */
class MediaLibraryManager private constructor(val mediaStorage: MediaStorage) {
    sealed class Event {
        data class ItemDeleted(val item: Item) : Event()
        data class MediaItemIsAdded(val mediaItem: MediaItem, val assignedCategoriesIds: List<Int>) : Event()
        data class CategoryRemoved(val category: Category) : Event()
        data class CategoryCreated(val category: Category) : Event()
        data class CategoryRenamed(val category: Category) : Event()
        //TODO Add Delete and Update events and implement them here
    }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event: SharedFlow<Event> = _event

    suspend fun deleteItem(item: Item) {
        mediaStorage.eliminateMediaItem(item.id)
        _event.emit(Event.ItemDeleted(item))
    }

    /**
     * Creates Metadata file for give(downloaded) [videoItem].
     * Also appends [videoItem] to the given [customCategories]
     */

    //TODO Make some lock to avoid asynchronous registering items
    suspend fun registerMediaItem(videoItem: VideoItem, customCategories: List<Category>) {
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
        mediaStorage.assignItemToCategory(Category.ALL, mediaItem)
        addMediaItemToAdditionalCategories(mediaItem, customCategories)
        _event.emit(Event.MediaItemIsAdded(mediaItem, customCategories.map { it.id }))
    }

    suspend fun createCategory(category: Category) {
        mediaStorage.addCategory(category)
        _event.emit(Event.CategoryCreated(category))
    }

    suspend fun removeCategory(category: Category) {
        mediaStorage.removeCategory(category)
        _event.emit(Event.CategoryRemoved(category))
    }

    suspend fun renameCategory(category: Category, newName: String) {
        val newCategory = category.copy(name = newName)
        val newCategoryContainer = mediaStorage.getCategoryContainer(category).copy(category = newCategory)
        mediaStorage.saveCategoryContainer(newCategoryContainer)
        _event.emit(Event.CategoryRenamed(category))
    }

    private suspend fun addMediaItemToAdditionalCategories(mediaItem: MediaItem, additionalCustomCategories: List<Category>) {
        val availableCategories = mediaStorage.getCustomCategories()

        additionalCustomCategories.forEach { targetCategory ->
            val category = availableCategories.find { it.id == targetCategory.id }
            category?.let { mediaStorage.assignItemToCategory(it, mediaItem) }
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