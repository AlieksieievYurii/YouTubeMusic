package com.yurii.youtubemusic.screens.saved.mediaitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.services.media.MediaLibraryManager
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import com.yurii.youtubemusic.utilities.PlaylistRepository
import com.yurii.youtubemusic.utilities.move
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaItemsViewModel @AssistedInject constructor(
    @Assisted private val category: Category,
    private val mediaLibraryManager: MediaLibraryManager,
    private val mediaServiceConnection: MediaServiceConnection,
    private val playlistRepository: PlaylistRepository,
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
                    is MediaLibraryManager.Event.CategoryAssignment -> handleCustomCategoryAssignment(
                        event.mediaItem,
                        event.customCategories
                    )
                    else -> {
                        // Ignore some events
                    }
                }
            }
        }
    }

    fun onMove(mediaItem: MediaItem, from: Int, to: Int) {
        viewModelScope.launch {
            val currentList = getMediaItemsFromCache()
            currentList.move(from, to)
            _mediaItemsStatus.value = MediaItemsStatus.Loaded(currentList)
            mediaLibraryManager.changeMediaItemPosition(category, mediaItem, from, to)
        }
    }

    fun onClickMediaItem(mediaItem: MediaItem) {
        when (val playbackState = playbackState.value) {
            PlaybackState.None -> mediaServiceConnection.play(mediaItem, category)
            is PlaybackState.Playing -> if (playbackState.mediaItem == mediaItem) {
                if (playbackState.isPaused)
                    mediaServiceConnection.resume()
                else
                    mediaServiceConnection.pause()
            } else
                mediaServiceConnection.play(mediaItem, category)
        }
    }

    fun deleteMediaItem(mediaItem: MediaItem) {
        viewModelScope.launch { mediaLibraryManager.deleteItem(mediaItem) }
    }

    suspend fun getAssignedPlaylists(mediaItem: MediaItem) = playlistRepository.getAssignedPlaylistsFor(mediaItem)

    suspend fun getPlaylists() = playlistRepository.getPlaylists().first()

    fun assignPlaylists(mediaItem: MediaItem, playlists: List<MediaItemPlaylist>) {
        viewModelScope.launch {
            playlistRepository.assignMediaItemToPlaylists(mediaItem, playlists)
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

    private suspend fun getMediaItemsFromCache(): MutableList<MediaItem> =
        ((_mediaItemsStatus.value as? MediaItemsStatus.Loaded)?.mediaItems
            ?: mediaServiceConnection.getMediaItemsFor(category)).toMutableList()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val assistedFactory: MediaItemsViewModelAssistedFactory,
        private val category: Category
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = assistedFactory.create(category) as T
    }
}

@AssistedFactory
interface MediaItemsViewModelAssistedFactory {
    fun create(category: Category): MediaItemsViewModel
}