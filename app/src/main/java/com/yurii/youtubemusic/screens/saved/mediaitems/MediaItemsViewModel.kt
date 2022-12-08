package com.yurii.youtubemusic.screens.saved.mediaitems

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.*
import com.yurii.youtubemusic.screens.saved.service.MusicServiceConnection
import com.yurii.youtubemusic.screens.saved.service.PLAYBACK_STATE_MEDIA_ITEM
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.EXTRA_KEY_CATEGORIES
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.screens.main.MainActivityViewModel
import com.yurii.youtubemusic.utilities.MediaMetadataProvider
import com.yurii.youtubemusic.utilities.MediaStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.IllegalStateException

class MediaItemsViewModel(
    val category: Category,
    private val mediaStorage: MediaStorage,
    private val mediaMetadataProvider: MediaMetadataProvider,
    private val activityViewModel: MainActivityViewModel,
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    data class PlayingMediaItem(val mediaMetaData: MediaMetaData, val isPaused: Boolean)

    sealed class MediaItemsStatus {
        object Loading : MediaItemsStatus()
        object NoMediaItems : MediaItemsStatus()
        data class Loaded(val mediaItems: List<MediaMetaData>) : MediaItemsStatus()
    }

    sealed class Event {
        data class ConfirmRemovingMediaItem(val mediaMetaData: MediaMetaData) : Event()
    }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event: SharedFlow<Event> = _event

    private val _mediaItems: MutableStateFlow<MediaItemsStatus> = MutableStateFlow(MediaItemsStatus.Loading)
    val mediaItems: StateFlow<MediaItemsStatus> = _mediaItems

    val playbackState = musicServiceConnection.playbackState

    private val _playingMediaItem = MutableStateFlow<PlayingMediaItem?>(null)
    val playingMediaItem: StateFlow<PlayingMediaItem?> = _playingMediaItem

    init {
        viewModelScope.launch {
            musicServiceConnection.playbackState.asFlow().collectLatest {
                val mediaMetaData = it.extras?.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)

                when (it.state) {
                    PlaybackStateCompat.STATE_PLAYING -> _playingMediaItem.value = PlayingMediaItem(mediaMetaData!!, isPaused = false)
                    PlaybackStateCompat.STATE_PAUSED -> _playingMediaItem.value = PlayingMediaItem(mediaMetaData!!, isPaused = true)
                    PlaybackStateCompat.STATE_STOPPED -> _playingMediaItem.value = null
                }
            }
        }
    }

    private val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            val mediaItems = children.map { MediaMetaData.createFrom(it) }
            _mediaItems.value = if (mediaItems.isEmpty()) MediaItemsStatus.NoMediaItems else MediaItemsStatus.Loaded(mediaItems)
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            //TODO Implement error handling
            Timber.e(parentId + options)
        }
    }

    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(category.id.toString(), mediaItemsSubscription)
    }

    init {
        viewModelScope.launch {
            activityViewModel.event.collectLatest {
                if (_mediaItems.value is MediaItemsStatus.Loaded) {
                    when (it) {
                        is MainActivityViewModel.Event.ItemHasBeenDeleted -> deleteMediaItem(it.item)
                        is MainActivityViewModel.Event.ItemHasBeenDownloaded -> addItem(it.videoItem)
                        is MainActivityViewModel.Event.ItemHasBeenModified -> updateMediaItem(it.item)
                        else -> {
                        }
                    }
                }
            }
        }
    }

    //TODO Check if it works correctly
    private fun updateMediaItem(mediaMetaData: MediaMetaData) {
        val mediaItems = (_mediaItems.value as MediaItemsStatus.Loaded).mediaItems
        if (category == Category.ALL) {
            _mediaItems.value = MediaItemsStatus.Loaded(mediaItems.map { if (it.id == mediaMetaData.id) mediaMetaData else it })
            return
        }

        if (category in mediaMetaData.categories) {
            if (!mediaItems.contains(mediaMetaData))
                _mediaItems.value = MediaItemsStatus.Loaded(mediaItems + mediaMetaData)
        } else {
            _mediaItems.value = MediaItemsStatus.Loaded(mediaItems.filter { it.id != mediaMetaData.id })
        }

    }

    private fun deleteMediaItem(item: Item) {
        val mediaItems = (_mediaItems.value as MediaItemsStatus.Loaded).mediaItems
        _mediaItems.value = MediaItemsStatus.Loaded(mediaItems.filter { it.id != item.id })
    }

    private fun addItem(item: Item) {
        val metadata = getMetaData(item.id)
        val mediaItems = (_mediaItems.value as MediaItemsStatus.Loaded).mediaItems
        _mediaItems.value = MediaItemsStatus.Loaded(mediaItems + metadata)
    }

    fun getMetaData(mediaId: String): MediaMetaData = mediaMetadataProvider.readMetadata(mediaId)

    fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat? {
        val currentMediaItem = playbackState.value?.extras?.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)
        return if (mediaItem.mediaId == currentMediaItem?.mediaId) playbackState.value else null
    }

    fun deleteMediaItem(mediaMetaData: MediaMetaData) {
        mediaStorage.deleteAllDataFor(mediaMetaData.mediaId)
    }

    fun onClickMediaItem(mediaMetaData: MediaMetaData) {
        getPlaybackState(mediaMetaData)?.run {
            if (state == PlaybackStateCompat.STATE_PLAYING)
                musicServiceConnection.transportControls.pause()
            else if (state == PlaybackStateCompat.STATE_PAUSED)
                musicServiceConnection.transportControls.play()
        } ?: playMusic(mediaMetaData)
    }

    private fun playMusic(mediaMetaData: MediaMetaData) {
        musicServiceConnection.transportControls.playFromMediaId(mediaMetaData.mediaId, Bundle().apply {
            putParcelable(EXTRA_KEY_CATEGORIES, category)
        })
    }

    private fun sendEvent(event: Event) = viewModelScope.launch { _event.emit(event) }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val category: Category,
        private val mediaStorage: MediaStorage,
        private val mediaMetadataProvider: MediaMetadataProvider,
        private val activityViewModel: MainActivityViewModel,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaItemsViewModel::class.java))
                return MediaItemsViewModel(category, mediaStorage, mediaMetadataProvider, activityViewModel, musicServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from MediaItemsViewModel class")
        }
    }
}