package com.youtubemusic.feature.saved_music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubemusic.core.data.repository.PlaylistRepository
import com.youtubemusic.core.model.MediaItemPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SavedMusicViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    sealed class State {
        object Loading : State()
        data class Loaded(val allCategories: List<MediaItemPlaylist>) : State()
    }

    private val _musicCategories: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val musicCategories = _musicCategories.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.getPlaylists().collectLatest { playlists ->
                _musicCategories.value = State.Loaded(playlists.toMutableList().also {
                    it.add(0, MediaItemPlaylist.ALL)
                })
            }
        }
    }
}