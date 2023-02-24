package com.yurii.youtubemusic.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class
SavedMusicViewModel @Inject constructor(
    private val mediaServiceConnection: MediaServiceConnection
) : ViewModel() {
    sealed class State {
        object Loading : State()
        data class Loaded(val allCategories: List<MediaItemPlaylist>) : State()
    }

    private val _musicCategories: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val musicCategories = _musicCategories.asStateFlow()

    init {
        viewModelScope.launch {
            mediaServiceConnection.allPlaylists.collectLatest { playlists ->
                _musicCategories.value = State.Loaded(playlists.toMutableList().also {
                    it.add(0, MediaItemPlaylist.ALL)
                })
            }
        }
    }
}