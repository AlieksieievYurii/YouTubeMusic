package com.yurii.youtubemusic.viewmodels.mediaitems

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem

class MediaItemsViewModel(
    private val context: Context,
    private val category: Category,
    musicServiceConnection: MusicServiceConnection
) {
    private val _mediaItems = MutableLiveData<List<MediaItem>>()
    val mediaItems: LiveData<List<MediaItem>> = _mediaItems

    private val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            val mediaItems = children.map { MediaItem.createFrom(it) }
            _mediaItems.postValue(mediaItems)
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            //TODO Implement error handling
        }
    }

    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(category.name, mediaItemsSubscription)
    }
}