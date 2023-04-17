package com.yurii.youtubemusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubemusic.core.player.MediaServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val mediaServiceConnection: MediaServiceConnection,
) : ViewModel() {
    sealed class Event {
        data class MediaServiceError(val exception: Exception) : Event()
    }

    init {
        viewModelScope.launch {
            mediaServiceConnection.errors.collectLatest { sendEvent(Event.MediaServiceError(it)) }
        }
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private fun sendEvent(event: Event) = viewModelScope.launch { _event.emit(event) }
}