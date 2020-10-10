package com.yurii.youtubemusic.viewmodels.mediaitems

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData

class MediaItemsViewModel(
    private val context: Context,
    private val category: Category,
    musicServiceConnection: MusicServiceConnection
) {
    private val _mediaItems = MutableLiveData<List<MediaMetaData>>()
    val mediaItems: LiveData<List<MediaMetaData>> = _mediaItems

    val playbackState = musicServiceConnection.playbackState

    private val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            val mediaItems = children.map { MediaMetaData.createFrom(it) }
            _mediaItems.postValue(mediaItems)
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            //TODO Implement error handling
        }
    }

    fun playMusic(mediaMetaData: MediaMetaData) {
        musicServiceConnection.transportControls.playFromMediaId(mediaMetaData.mediaId, null)
    }

    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(category.id.toString(), mediaItemsSubscription)
    }
}