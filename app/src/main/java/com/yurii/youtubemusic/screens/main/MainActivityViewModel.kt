package com.yurii.youtubemusic.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtubemusic.core.data.repository.GoogleAccount
import com.youtubemusic.core.downloader.youtube.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val mediaServiceConnection: com.youtubemusic.core.player.MediaServiceConnection,
    downloadManager: DownloadManager,
    googleAccount: GoogleAccount
) : ViewModel() {
    sealed class Event {
        data class MediaServiceError(val exception: Exception) : Event()
    }

    val isAuthenticatedAndAuthorized = googleAccount.isAuthenticatedAndAuthorized
    val numberOfDownloadingJobs = downloadManager.getDownloadingJobs().map { it.size }

    init {
        viewModelScope.launch {
            mediaServiceConnection.errors.collectLatest { sendEvent(Event.MediaServiceError(it)) }
        }
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    private fun sendEvent(event: Event) = viewModelScope.launch { _event.emit(event) }
}