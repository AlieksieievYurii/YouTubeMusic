package com.yurii.youtubemusic.screens.saved.mediaitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.services.media.MediaPlayer
import com.yurii.youtubemusic.services.media.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class MediaItemsViewModel(private val mediaPlayer: MediaPlayer) : ViewModel() {
    sealed class MediaItemsStatus {
        object Loading : MediaItemsStatus()
        object NoMediaItems : MediaItemsStatus()
        data class Loaded(val mediaItems: List<MediaItem>) : MediaItemsStatus()
    }

    private val _mediaItemsStatus: MutableStateFlow<MediaItemsStatus> = MutableStateFlow(MediaItemsStatus.Loading)
    val mediaItemsStatus = _mediaItemsStatus.asStateFlow()

    val playbackState = mediaPlayer.playbackState

    init {
        viewModelScope.launch {
            mediaPlayer.mediaItems.collect { mediaItems ->
                _mediaItemsStatus.value = if (mediaItems.isNotEmpty()) MediaItemsStatus.Loaded(mediaItems) else MediaItemsStatus.NoMediaItems
            }
        }
        viewModelScope.launch {
            mediaPlayer.launch()
        }
    }

    fun onClickMediaItem(mediaItem: MediaItem) {
        when (val playbackState = mediaPlayer.playbackState.value) {
            PlaybackState.None -> mediaPlayer.play(mediaItem)
            is PlaybackState.Paused -> if (playbackState.mediaItem == mediaItem) mediaPlayer.resume() else mediaPlayer.play(mediaItem)
            is PlaybackState.Playing -> if (playbackState.mediaItem == mediaItem) mediaPlayer.pause() else mediaPlayer.play(mediaItem)
        }
    }

    fun deleteMediaItem(mediaItem: MediaItem) {
        viewModelScope.launch { mediaPlayer.removeMediaItem(mediaItem) }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mediaPlayer: MediaPlayer
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaItemsViewModel::class.java))
                return MediaItemsViewModel(mediaPlayer) as T
            throw IllegalStateException("Given the model class is not assignable from MediaItemsViewModel class")
        }
    }
}