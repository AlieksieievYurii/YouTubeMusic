package com.yurii.youtubemusic.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.screens.youtube.models.Item
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    sealed class Event {
        object LogOutEvent : Event()
        data class LogInEvent(val account: GoogleSignInAccount) : Event()
        data class ItemHasBeenDownloaded(val videoItem: VideoItem) : Event()
        data class ItemHasBeenDeleted(val item: Item) : Event()
        data class ItemHasBeenModified(val item: MediaMetaData) : Event()

    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    fun signIn(account: GoogleSignInAccount) = sendEvent(Event.LogInEvent(account))

    fun logOut() = sendEvent(Event.LogOutEvent)

    fun notifyMediaItemHasBeenDeleted(item: Item) = sendEvent(Event.ItemHasBeenDeleted(item))

    fun notifyMediaItemHasBeenModified(mediaMetaData: MediaMetaData) = sendEvent(Event.ItemHasBeenModified(mediaMetaData))

    fun notifyVideoItemHasBeenDownloaded(videoItem: VideoItem) = sendEvent(Event.ItemHasBeenDownloaded(videoItem))

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _event.emit(event)
    }

    class MainActivityViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainActivityViewModel() as T
            }
            throw IllegalArgumentException("Unable to construct viewModel")
        }
    }
}