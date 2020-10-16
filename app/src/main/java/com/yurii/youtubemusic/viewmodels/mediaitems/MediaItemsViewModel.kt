package com.yurii.youtubemusic.viewmodels.mediaitems

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yurii.youtubemusic.mediaservice.MusicServiceConnection
import com.yurii.youtubemusic.mediaservice.PLAYBACK_STATE_MEDIA_ITEM
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.EXTRA_KEY_CATEGORIES
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.MediaMetadataProvider

class MediaItemsViewModel(private val context: Context, val category: Category, musicServiceConnection: MusicServiceConnection) {
    private val mediaMetadataProvider = MediaMetadataProvider(context)
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

    fun getMetaData(mediaId: String): MediaMetaData = mediaMetadataProvider.readMetadata(mediaId)

    fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat? {
        val currentMediaItem = playbackState.value!!.extras?.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)
        return if (mediaItem == currentMediaItem) playbackState.value!! else null
    }

    fun deleteMediaItem(mediaMetaData: MediaMetaData) {
        musicServiceConnection.requestDeleteMediaItem(mediaMetaData.mediaId)

        mediaMetaData.mediaId.run {
            DataStorage.getMusic(context, this).delete()
            DataStorage.getMetadata(context, this).delete()
            DataStorage.getThumbnail(context, this).delete()
        }
    }

    fun onClickMediaItem(mediaMetaData: MediaMetaData) {
        getPlaybackState(mediaMetaData)?.run {
            if (state == PlaybackStateCompat.STATE_PLAYING)
                musicServiceConnection.transportControls.pause()
            else if (state == PlaybackStateCompat.STATE_PAUSED)
                musicServiceConnection.transportControls.play()
        } ?: playMusic(mediaMetaData)
    }

    private fun playMusic(mediaMetaData: MediaMetaData) {
        musicServiceConnection.transportControls.playFromMediaId(mediaMetaData.mediaId, Bundle().apply {
            putParcelable(EXTRA_KEY_CATEGORIES, category)
        })
    }

    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(category.id.toString(), mediaItemsSubscription)
    }
}