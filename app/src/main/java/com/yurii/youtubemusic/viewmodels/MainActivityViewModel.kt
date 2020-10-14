package com.yurii.youtubemusic.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class Event {
    private var hasBeenHandled = false

    fun handle(c: (() -> Unit)) {
        if (!hasBeenHandled) {
            c.invoke()
            hasBeenHandled = true
        }
    }
}

class MainActivityViewModel : ViewModel() {
    private val _logOutAction: MutableLiveData<Event> = MutableLiveData()
    val logOutAction: LiveData<Event> = _logOutAction

    fun logOut() {
        _logOutAction.postValue(Event())
    }
}