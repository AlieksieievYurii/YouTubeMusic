package com.yurii.youtubemusic.screens.saved


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.utilities.MediaServiceConnection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class SavedMusicViewModel(private val mediaServiceConnection: MediaServiceConnection) : ViewModel() {
    private val _musicCategories: MutableSharedFlow<List<Category>> = MutableSharedFlow()
    val musicCategories = _musicCategories.asSharedFlow()

    init {
        reloadMusicCategories()
    }

    fun reloadMusicCategories() {
        viewModelScope.launch {
            _musicCategories.emit(mediaServiceConnection.getCategories())
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mediaServiceConnection: MediaServiceConnection) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SavedMusicViewModel::class.java))
                return SavedMusicViewModel(mediaServiceConnection) as T
            throw IllegalStateException("Given the model class is not assignable from SavedMusicViewModel class")
        }
    }
}