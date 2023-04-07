package com.youtubemusic.feature.playlist_editor

import androidx.lifecycle.*
import com.youtubemusic.core.data.repository.PlaylistRepository
import com.youtubemusic.core.model.MediaItemPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistEditorViewModel @Inject constructor(private val playlistRepository: PlaylistRepository) : ViewModel() {

    val playlistsFlow = playlistRepository.getPlaylists()

    fun renameCategory(playlist: MediaItemPlaylist, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlist, newName)
        }
    }

    fun removePlaylist(playlist: MediaItemPlaylist) {
        viewModelScope.launch {
            playlistRepository.removePlaylist(playlist)
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            playlistRepository.addPlaylist(name)
        }
    }

}

