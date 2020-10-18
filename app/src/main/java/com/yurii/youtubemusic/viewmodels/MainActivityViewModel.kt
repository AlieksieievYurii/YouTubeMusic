package com.yurii.youtubemusic.viewmodels

import androidx.lifecycle.ViewModel
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

    private val _logOutEvent: MutableSingleLiveEvent<Void> = MutableSingleLiveEvent()
    val logOutEvent: SingleLiveEvent<Void> = _logOutEvent

    fun signIn(account: GoogleSignInAccount) = _logInEvent.setValue(account)

    fun logOut() = _logOutEvent.call()

    fun notifyMediaItemHasBeenDeleted(id: String) = _onMediaItemIsDeleted.setValue(id)

    fun notifyMediaItemHasBeenModified(mediaMetaData: MediaMetaData) = _onUpdateMediaItem.setValue(mediaMetaData)

    fun notifyVideoItemHasBeenDownloaded(videoItem: VideoItem) = _onVideoItemHasBeenDownloaded.setValue(videoItem)
}