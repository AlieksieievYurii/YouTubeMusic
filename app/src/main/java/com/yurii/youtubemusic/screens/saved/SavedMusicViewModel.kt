package com.yurii.youtubemusic.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.services.media.MediaLibraryManager
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedMusicViewModel @Inject constructor(
    private val mediaServiceConnection: MediaServiceConnection,
    private val mediaLibraryManager: MediaLibraryManager
) : ViewModel() {
    sealed class State {
        object Loading : State()
        data class Loaded(val allCategories: List<Category>) : State()
    }

    private val _musicCategories: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val musicCategories = _musicCategories.asStateFlow()

    init {
        viewModelScope.launch {
            mediaLibraryManager.event.collectLatest {
                if (it is MediaLibraryManager.Event.CategoryRemoved ||
                    it is MediaLibraryManager.Event.CategoryCreated ||
                    it is MediaLibraryManager.Event.CategoryUpdated
                ) {
                    reloadMusicCategories()
                }
            }
        }
        reloadMusicCategories()
    }

    private fun reloadMusicCategories() {
        viewModelScope.launch {
            val allCategories = mediaServiceConnection.getCategories()
            _musicCategories.value = State.Loaded(allCategories)
        }
    }
}