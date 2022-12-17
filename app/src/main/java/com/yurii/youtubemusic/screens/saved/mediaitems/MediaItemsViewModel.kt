package com.yurii.youtubemusic.screens.saved.mediaitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.isDefault
import com.yurii.youtubemusic.services.media.MediaLibraryManager
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class MediaItemsViewModel(
    private val category: Category,
    private val mediaLibraryManager: MediaLibraryManager,
    private val mediaServiceConnection: MediaServiceConnection
) : ViewModel() {
    sealed class MediaItemsStatus {
        object Loading : MediaItemsStatus()
        object NoMediaItems : MediaItemsStatus()
        data class Loaded(val mediaItems: List<MediaItem>) : MediaItemsStatus()
    }

    private val _mediaItemsStatus: MutableStateFlow<MediaItemsStatus> = MutableStateFlow(MediaItemsStatus.Loading)
    val mediaItemsStatus = _mediaItemsStatus.asStateFlow()

    val playbackState = mediaServiceConnection.playbackState

    init {
        viewModelScope.launch {
            val mediaItems = mediaServiceConnection.getMediaItemsFor(category)
            _mediaItemsStatus.value = if (mediaItems.isNotEmpty()) MediaItemsStatus.Loaded(mediaItems) else MediaItemsStatus.NoMediaItems
        }

        viewModelScope.launch {
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
    }

    fun onClickMediaItem(mediaItem: MediaItem) {
        when (val playbackState = playbackState.value) {
            PlaybackState.None -> mediaServiceConnection.play(mediaItem, category)
            is PlaybackState.Paused -> if (playbackState.mediaItem == mediaItem)
                mediaServiceConnection.resume()
            else
                mediaServiceConnection.play(mediaItem, category)
            is PlaybackState.Playing -> if (playbackState.mediaItem == mediaItem)
                mediaServiceConnection.pause()
            else
                mediaServiceConnection.play(mediaItem, category)
        }
    }

    fun deleteMediaItem(mediaItem: MediaItem) {
        viewModelScope.launch { mediaLibraryManager.deleteItem(mediaItem) }
    }

    suspend fun getAssignedCustomCategoriesFor(mediaItem: MediaItem) =
        mediaLibraryManager.mediaStorage.getAssignedCustomCategoriesFor(mediaItem)

    suspend fun getAllCustomCategories() = mediaLibraryManager.mediaStorage.getCustomCategories()

    fun assignCustomCategoriesFor(mediaItem: MediaItem, categories: List<Category>) {
        viewModelScope.launch {
            mediaLibraryManager.assignCategories(mediaItem, categories)
        }
    }

    private suspend fun handleItemIsRemoved(item: Item) {
        val newList = getMediaItemsFromCache().filterNot { it.id == item.id }
        _mediaItemsStatus.value = MediaItemsStatus.Loaded(newList)
    }

    private suspend fun handleMediaItemIsAdded(mediaItem: MediaItem, customAssignedCategoriesIds: List<Int>) {
        if (category.isDefault || category.id in customAssignedCategoriesIds) {
            val newList = getMediaItemsFromCache().apply { add(mediaItem) }
            _mediaItemsStatus.value = MediaItemsStatus.Loaded(newList)
        }
    }

    private suspend fun handleCustomCategoryAssignment(mediaItem: MediaItem, customCategories: List<Category>) {
        if (category.isDefault)
            return

        val mediaItems = getMediaItemsFromCache()
        if (customCategories.contains(category) && !mediaItems.contains(mediaItem)) {
            mediaItems.add(mediaItem)
            _mediaItemsStatus.value = MediaItemsStatus.Loaded(mediaItems)
        } else if (!customCategories.contains(category) && mediaItems.contains(mediaItem)) {
            mediaItems.remove(mediaItem)
            _mediaItemsStatus.value = MediaItemsStatus.Loaded(mediaItems)
        }
    }

    private suspend fun getMediaItemsFromCache(): MutableList<MediaItem> = ((_mediaItemsStatus.value as? MediaItemsStatus.Loaded)?.mediaItems
        ?: mediaServiceConnection.getMediaItemsFor(category)).toMutableList()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val category: Category,
        private val mediaLibraryManager: MediaLibraryManager,
        private val mediaServiceConnection: MediaServiceConnection
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaItemsViewModel::class.java))
                return MediaItemsViewModel(category, mediaLibraryManager, mediaServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from MediaItemsViewModel class")
        }
    }
}