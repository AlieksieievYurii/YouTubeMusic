package com.yurii.youtubemusic.screens.saved.mediaitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import com.yurii.youtubemusic.services.media.PlaybackState
import com.yurii.youtubemusic.source.MediaLibraryDomain
import com.yurii.youtubemusic.source.PlaylistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaItemsViewModel @AssistedInject constructor(
    @Assisted private val playlist: MediaItemPlaylist,
    private val mediaServiceConnection: MediaServiceConnection,
    private val playlistRepository: PlaylistRepository,
    private val mediaLibraryDomain: MediaLibraryDomain
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
            mediaLibraryDomain.getMediaItems(playlist).collect {
                _mediaItemsStatus.value = if (it.isEmpty()) MediaItemsStatus.NoMediaItems else MediaItemsStatus.Loaded(it)
            }
        }
    }

    fun onMove(mediaItem: MediaItem, from: Int, to: Int) {
        viewModelScope.launch {
            mediaLibraryDomain.changePosition(playlist, mediaItem, from, to)
        }
    }

    fun onClickMediaItem(mediaItem: MediaItem) {
        when (val playbackState = playbackState.value) {
            PlaybackState.None -> mediaServiceConnection.play(mediaItem, playlist)
            is PlaybackState.Playing -> if (playbackState.mediaItem == mediaItem) {
                if (playbackState.isPaused)
                    mediaServiceConnection.resume()
                else
                    mediaServiceConnection.pause()
            } else
                mediaServiceConnection.play(mediaItem, playlist)
        }
    }

    fun deleteMediaItem(mediaItem: MediaItem) {
        viewModelScope.launch {
            mediaLibraryDomain.deleteMediaItem(mediaItem)
        }
    }

    suspend fun getAssignedPlaylists(mediaItem: MediaItem) = playlistRepository.getAssignedPlaylistsFor(mediaItem)

    suspend fun getPlaylists() = playlistRepository.getPlaylists().first()

    fun assignPlaylists(mediaItem: MediaItem, playlists: List<MediaItemPlaylist>) {
        viewModelScope.launch {
            playlistRepository.assignMediaItemToPlaylists(mediaItem, playlists)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val assistedFactory: MediaItemsViewModelAssistedFactory,
        private val playlist: MediaItemPlaylist
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = assistedFactory.create(playlist) as T
    }
}

@AssistedFactory
interface MediaItemsViewModelAssistedFactory {
    fun create(playlist: MediaItemPlaylist): MediaItemsViewModel
}