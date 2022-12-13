package com.yurii.youtubemusic.services.media

import com.yurii.youtubemusic.models.Category
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
    val mediaLibraryManager: MediaLibraryManager
) {
    private val _mediaItems: MutableSharedFlow<List<MediaItem>> = MutableSharedFlow(replay = 1) // replay = 1 to keep the cache
    val mediaItems = _mediaItems.asSharedFlow()

    val playbackState = mediaServiceConnection.playbackState

    suspend fun launch() {
        _mediaItems.emit(mediaServiceConnection.getMediaItemsFor(category))
        mediaLibraryManager.event.collectLatest { event ->
            when (event) {
                is MediaLibraryManager.Event.ItemDeleted -> {
                    val newList = _mediaItems.replayCache[0].filter { it.id != event.item.id } //replayCache[0] because replay = 1
                    _mediaItems.emit(newList)
                }
                is MediaLibraryManager.Event.MediaItemIsAdded -> if (category.isDefault || category.id in event.assignedCategoriesIds) {
                    val newList = _mediaItems.replayCache[0].toMutableList().apply {
                        add(event.mediaItem)
                    }
                    _mediaItems.emit(newList)
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

}