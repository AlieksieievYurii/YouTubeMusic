package com.yurii.youtubemusic.services.media

import android.content.Context
import com.yurii.youtubemusic.models.Category

import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.utilities.move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

/**
 * The class is responsible for doing modification operations on media items and categories.
 * It implements event-based approach. In simple words when you do some changes,
 * an event is triggered and all its subscribers are informed.
 */
class MediaLibraryManager private constructor(val mediaStorage: MediaStorage) {
    sealed class Event {
        data class ItemDeleted(val item: Item) : Event()
        data class MediaItemIsAdded(val mediaItem: MediaItem, val assignedCategoriesIds: List<Int>) : Event()
        data class CategoryRemoved(val category: Category) : Event()
        data class CategoryCreated(val category: Category) : Event()
        data class CategoryUpdated(val category: Category) : Event()
        data class CategoryAssignment(val mediaItem: MediaItem, val customCategories: List<Category>) : Event()
        data class MediaItemPositionChanged(val category: Category, val mediaItem: MediaItem, val from: Int, val to: Int) : Event()
    }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event: SharedFlow<Event> = _event

    /**
     * Eliminates(means that all the information of item will be remove) the give media item and sends broadcast event [Event.ItemDeleted]
     */
    suspend fun deleteItem(item: Item) {
        mediaStorage.eliminateMediaItem(item.id)
        _event.emit(Event.ItemDeleted(item))
    }

    /**
     * Changes the order of the given [item] in [category]. Also sends broadcast event [Event.MediaItemPositionChanged]
     */
    suspend fun changeMediaItemPosition(category: Category, item: MediaItem, from: Int, to: Int) {
        val categoryContainer = mediaStorage.getCategoryContainer(category)
        if (item.id != categoryContainer.mediaItemsIds[from])
            throw IllegalStateException("Can not move media item")

        val newList = categoryContainer.mediaItemsIds.toMutableList()
        newList.move(from, to)
        val newCategoryContainer = categoryContainer.copy(mediaItemsIds = newList)
        mediaStorage.saveCategoryContainer(newCategoryContainer)

        _event.emit(Event.MediaItemPositionChanged(category, item, from, to))
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
        mediaStorage.assignItemToDefaultCategory(mediaItem)
        assignMediaItemToCustomCategories(mediaItem, customCategories)
        _event.emit(Event.MediaItemIsAdded(mediaItem, customCategories.map { it.id }))
    }

    /**
     * Creates meta file for the given [category]. Also sends the event [Event.CategoryCreated]
     */
    suspend fun createCategory(category: Category) {
        mediaStorage.addCategory(category)
        _event.emit(Event.CategoryCreated(category))
    }

    /**
     * Removes [category]. Also sends the event [Event.CategoryRemoved]
     */
    suspend fun removeCategory(category: Category) {
        mediaStorage.removeCategory(category)
        _event.emit(Event.CategoryRemoved(category))
    }

    /**
     * Updates [category]. Also sends the event [Event.CategoryUpdated]
     */
    suspend fun updateCategory(category: Category) {
        val newCategoryContainer = mediaStorage.getCategoryContainer(category).copy(category = category)
        mediaStorage.saveCategoryContainer(newCategoryContainer)
        _event.emit(Event.CategoryUpdated(category))
    }

    /**
     * Synchronises the given [customCategories] with actual of [mediaItem]. For example, it [mediaItem] is assigned to a category that is not listed
     * in [customCategories], then the [mediaItem] will be unassigned from the category. And in the opposite way, if the media item is not assigned
     * to one of the categories from [customCategories], then it will be assigned.
     *
     * Also sends broadcast event [Event.CategoryAssignment]
     */
    suspend fun assignCategories(mediaItem: MediaItem, customCategories: List<Category>) = withContext(Dispatchers.IO) {
        mediaStorage.getAssignedCustomCategoriesFor(mediaItem).forEach { alreadyAssignedCategory ->
            if (!customCategories.contains(alreadyAssignedCategory))
                mediaStorage.demoteCategory(mediaItem, alreadyAssignedCategory)
        }

        customCategories.forEach { category -> mediaStorage.assignItemToCategory(category, mediaItem) }
        _event.emit(Event.CategoryAssignment(mediaItem, customCategories))
    }

    /**
     * In comparison to [assignCategories], it does not synchronise, but just assignees [additionalCustomCategories] to [mediaItem]
     * if each category exists, and the function does not send any event
     */
    private suspend fun assignMediaItemToCustomCategories(mediaItem: MediaItem, additionalCustomCategories: List<Category>) {
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