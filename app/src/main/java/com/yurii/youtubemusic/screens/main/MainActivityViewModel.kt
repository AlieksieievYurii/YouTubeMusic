package com.yurii.youtubemusic.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.services.media.MediaServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val mediaServiceConnection: MediaServiceConnection
) : ViewModel() {
    sealed class Event {
        object LogOutEvent : Event()
        data class LogInEvent(val account: GoogleSignInAccount) : Event()
        data class MediaServiceError(val exception: Exception) : Event()
    }

    init {
        viewModelScope.launch {
            mediaServiceConnection.errors.collectLatest { sendEvent(Event.MediaServiceError(it)) }
        }
    }

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event

    fun signIn(account: GoogleSignInAccount) = sendEvent(Event.LogInEvent(account))

    fun logOut() = sendEvent(Event.LogOutEvent)

    private fun sendEvent(event: Event) = viewModelScope.launch { _event.emit(event) }
}