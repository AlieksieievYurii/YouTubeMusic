package com.yurii.youtubemusic.utilities

import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem
import kotlinx.coroutines.flow.*

/**
 * Class represents very simplified interface to media service.
 * The class can controls music execution and is informed about adding/removing new media items.
 *
 * It provides [mediaItems] flow to get all the media items assigned to given [category].
 * Once the instance of the class is created, run suspended function [launch] to load the items and observe changes
 */
class MediaPlayer(val category: Category, private val mediaServiceConnection: MediaServiceConnection) {
    private val _mediaItems: MutableSharedFlow<List<MediaItem>> = MutableSharedFlow()
    val mediaItems = _mediaItems.asSharedFlow()

    val playbackState = mediaServiceConnection.playbackState

    suspend fun launch() {
        _mediaItems.emit(mediaServiceConnection.getMediaItemsFor(category))
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

}