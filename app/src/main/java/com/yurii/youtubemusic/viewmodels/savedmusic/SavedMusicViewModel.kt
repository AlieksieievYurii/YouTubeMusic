package com.yurii.youtubemusic.viewmodels.savedmusic

import android.app.Application
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yurii.youtubemusic.mediaservice.CATEGORIES_CONTENT
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaItem

class SavedMusicViewModel(application: Application, musicServiceConnection: MusicServiceConnection) : AndroidViewModel(application) {
    private val _categoryItems = MutableLiveData<List<Category>>()
    val categoryItems: LiveData<List<Category>> = _categoryItems


    private val categoryItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            _categoryItems.postValue(children.map { Category(0, it.mediaId!!) })
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            //TODO Implement error handling
        }
    }


    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
    }
}