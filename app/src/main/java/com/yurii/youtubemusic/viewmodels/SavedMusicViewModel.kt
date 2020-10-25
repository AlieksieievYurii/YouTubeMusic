package com.yurii.youtubemusic.viewmodels

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.*
import com.yurii.youtubemusic.mediaservice.CATEGORIES_CONTENT
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData
import java.lang.IllegalStateException

class SavedMusicViewModel(application: Application, musicServiceConnection: MusicServiceConnection) : AndroidViewModel(application) {
    private val _categoryItems = MutableLiveData<List<Category>>()
    val categoryItems: LiveData<List<Category>> = _categoryItems


    private val categoryItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            _categoryItems.postValue(children.map { Category.createFrom(it) })
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            //TODO Implement error handling
        }
    }

    fun refreshCategories() {
        musicServiceConnection.requestUpdatingMediaItems {
            musicServiceConnection.unsubscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
            musicServiceConnection.subscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
        }
    }

    fun deleteMediaItem(mediaId: String) = musicServiceConnection.requestDeleteMediaItem(mediaId)

    fun updateMediaItem(mediaMetaData: MediaMetaData) = musicServiceConnection.requestUpdateMediaItem(mediaMetaData)

    fun notifyVideoItemHasBeenDownloaded(mediaId: String) = musicServiceConnection.requestAddMediaItem(mediaId)

    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
    }
}

@Suppress("UNCHECKED_CAST")
class SavedMusicViewModelFactory(private val context: Context, private val musicServiceConnection: MusicServiceConnection) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedMusicViewModel::class.java))
            return SavedMusicViewModel(context as Application, musicServiceConnection) as T
        throw IllegalStateException("Given the model class is not assignable from SavedMusicViewModel class")
    }
}