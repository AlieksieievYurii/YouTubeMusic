package com.yurii.youtubemusic.services.media

import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.isDefault
import kotlinx.coroutines.flow.*

/**
 * Class represents very simplified interface to media service.
 * The class can controls music execution and is informed about adding/removing new media items.
 *
 * It provides [mediaItems] flow to get all the media items assigned to given [category].
 * Once the instance of the class is created, run suspended function [launch] to load the items and observe changes
 */
class MediaPlayer(
    val category: Category,
    private val mediaServiceConnection: MediaServiceConnection,
    private val mediaLibraryManager: MediaLibraryManager
) {
    private val _mediaItems: MutableSharedFlow<List<MediaItem>> = MutableSharedFlow(replay = 1) // replay = 1 to keep the cache
    val mediaItems = _mediaItems.asSharedFlow()

    val playbackState = mediaServiceConnection.playbackState

    suspend fun launch() {
        _mediaItems.emit(mediaServiceConnection.getMediaItemsFor(category))
        mediaLibraryManager.event.collectLatest { event ->
            when (event) {
                is MediaLibraryManager.Event.ItemDeleted -> handleItemIsRemoved(event.item)
                is MediaLibraryManager.Event.MediaItemIsAdded -> handleMediaItemIsAdded(event.mediaItem, event.assignedCategoriesIds)
                is MediaLibraryManager.Event.CategoryAssignment -> handleCustomCategoryAssignment(event.mediaItem, event.customCategories)
                else -> {
                    // Ignore some events
                }
            }
        }
    }

    fun play(mediaItem: MediaItem) {
        mediaServiceConnection.play(mediaItem, category)
    }

    fun pause() {
        mediaServiceConnection.pause()
    }

    fun resume() {
        mediaServiceConnection.resume()
    }

    suspend fun removeMediaItem(mediaItem: MediaItem) {
        mediaLibraryManager.deleteItem(mediaItem)
    }

    private suspend fun handleItemIsRemoved(item: Item) {
        val newList = getMediaItemsFromCache().filter { it.id != item.id }
        _mediaItems.emit(newList)
    }

    private suspend fun handleMediaItemIsAdded(mediaItem: MediaItem, customAssignedCategoriesIds: List<Int>) {
        if (category.isDefault || category.id in customAssignedCategoriesIds) {
            val newList = getMediaItemsFromCache().apply {
                add(mediaItem)
            }
            _mediaItems.emit(newList)
        }
    }

    private suspend fun handleCustomCategoryAssignment(mediaItem: MediaItem, customCategories: List<Category>) {
        if (category.isDefault)
            return

        val mediaItems = getMediaItemsFromCache()
        if (customCategories.contains(category) && !mediaItems.contains(mediaItem)) {
            mediaItems.add(mediaItem)
            _mediaItems.emit(mediaItems)
        } else if (!customCategories.contains(category) && mediaItems.contains(mediaItem)) {
            mediaItems.remove(mediaItem)
            _mediaItems.emit(mediaItems)
        }
    }

    private fun getMediaItemsFromCache() = _mediaItems.replayCache[0].toMutableList() //replayCache[0] because replay = 1
}