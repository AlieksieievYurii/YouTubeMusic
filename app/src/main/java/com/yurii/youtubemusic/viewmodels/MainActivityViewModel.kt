package com.yurii.youtubemusic.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

open class Event {
    private var hasBeenHandled = false

    fun handle(c: (() -> Unit)) {
        if (!hasBeenHandled) {
            c.invoke()
            hasBeenHandled = true
        }
    }
}

open class DataEvent<out T>(val content: T) {
    private var hasBeenHandled = false

    fun handleContent(c: ((content: T) -> Unit)) {
        if (!hasBeenHandled) {
            c.invoke(content)
            hasBeenHandled = true
        }
    }
}

class MainActivityViewModel : ViewModel() {
    private val _onMediaItemIsDeleted: MutableLiveData<DataEvent<String>> = MutableLiveData()
    val onMediaItemIsDeleted: LiveData<DataEvent<String>> = _onMediaItemIsDeleted

    private val _logInEvent: MutableLiveData<DataEvent<GoogleSignInAccount>> = MutableLiveData()
    val logInEvent: LiveData<DataEvent<GoogleSignInAccount>> = _logInEvent

    private val _logOutEvent: MutableLiveData<Event> = MutableLiveData()
    val logOutEvent: LiveData<Event> = _logOutEvent

    fun signIn(account: GoogleSignInAccount) = _logInEvent.postValue(DataEvent(account))

    fun logOut() = _logOutEvent.postValue(Event())

    fun notifyMediaItemHasBeenDeleted(id: String) = _onMediaItemIsDeleted.postValue(DataEvent(id))
}