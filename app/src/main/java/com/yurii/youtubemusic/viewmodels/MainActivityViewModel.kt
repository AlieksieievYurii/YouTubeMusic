package com.yurii.youtubemusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.models.MutableSingleLiveEvent
import com.yurii.youtubemusic.models.SingleLiveEvent
import com.yurii.youtubemusic.models.VideoItem

class MainActivityViewModel : ViewModel() {
    private val _onUpdateMediaItem: MutableSingleLiveEvent<MediaMetaData> = MutableSingleLiveEvent()
    val onUpdateMediaItem: SingleLiveEvent<MediaMetaData> = _onUpdateMediaItem

    private val _onVideoItemHasBeenDownloaded: MutableSingleLiveEvent<VideoItem> = MutableSingleLiveEvent()
    val onVideoItemHasBeenDownloaded: SingleLiveEvent<VideoItem> = _onVideoItemHasBeenDownloaded

    private val _onMediaItemIsDeleted: MutableSingleLiveEvent<String> = MutableSingleLiveEvent()
    val onMediaItemIsDeleted: SingleLiveEvent<String> = _onMediaItemIsDeleted

    private val _logInEvent: MutableSingleLiveEvent<GoogleSignInAccount> = MutableSingleLiveEvent()
    val logInEvent: SingleLiveEvent<GoogleSignInAccount> = _logInEvent

    private val _logOutEvent: MutableSingleLiveEvent<String?> = MutableSingleLiveEvent()
    val logOutEvent: SingleLiveEvent<String?> = _logOutEvent

    fun signIn(account: GoogleSignInAccount) = _logInEvent.sendEvent(account)

    fun logOut() = _logOutEvent.call()

    fun notifyMediaItemHasBeenDeleted(id: String) = _onMediaItemIsDeleted.sendEvent(id)

    fun notifyMediaItemHasBeenModified(mediaMetaData: MediaMetaData) = _onUpdateMediaItem.sendEvent(mediaMetaData)

    fun notifyVideoItemHasBeenDownloaded(videoItem: VideoItem) = _onVideoItemHasBeenDownloaded.sendEvent(videoItem)

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