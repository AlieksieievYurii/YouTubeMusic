package com.yurii.youtubemusic.services.mediaservice

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.utilities.AudioEffectManager

class MusicServiceConnection(context: Context, serviceComponent: ComponentName) {
    val isConnected = MutableLiveData<Boolean>().apply {
        postValue(false)
    }

    val audioEffectManager = AudioEffectManager(context)

    val rootMediaId: String get() = mediaBrowser.root
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponent,
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    private val _playbackState = MutableLiveData<PlaybackStateCompat>().apply {
        postValue(EMPTY_PLAYBACK_STATE)
    }
    val playbackState: LiveData<PlaybackStateCompat> = Transformations.map(_playbackState) {
        if (it.state == PlaybackStateCompat.STATE_PLAYING)
            audioEffectManager.applyLastChanges(it.extras!!.getInt(PLAYBACK_STATE_SESSION_ID))
         it
    }

    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private lateinit var mediaController: MediaControllerCompat

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    fun requestUpdateMediaItem(mediaMetaData: MediaMetaData) {
        mediaController.sendCommand(REQUEST_COMMAND_UPDATE_MEDIA_ITEM, Bundle().apply {
            putParcelable(PLAYBACK_STATE_MEDIA_ITEM, mediaMetaData)
        }, null)
    }

    fun requestUpdatingMediaItems(onFinishedCallback: (() -> Unit)) {
        mediaController.sendCommand(REQUEST_COMMAND_UPDATE_MEDIA_ITEMS, null, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == REQUEST_CODE_UPDATE_MEDIA_ITEMS)
                    onFinishedCallback.invoke()
            }
        })
    }

    fun requestDeleteMediaItem(mediaId: String) {
        mediaController.sendCommand(REQUEST_COMMAND_DELETE_MEDIA_ITEM, Bundle().apply {
            putString(EXTRA_MEDIA_ITEM, mediaId)
        }, null)
    }

    fun requestCurrentMediaTimePosition(onCurrentTimePosition: (position: Long) -> Unit) {
        mediaController.sendCommand(REQUEST_MEDIA_ITEM_TIME_POSITION, null, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                onCurrentTimePosition.invoke(resultData?.getLong(EXTRA_CURRENT_TIME_POSITION, 0) ?: 0)
            }
        })
    }

    fun requestAddMediaItem(mediaId: String) {
        mediaController.sendCommand(REQUEST_COMMAND_ADD_NEW_MEDIA_ITEM, Bundle().apply {
            putString(EXTRA_MEDIA_ITEM, mediaId)
        }, null)
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            isConnected.postValue(true)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            isConnected.postValue(false)
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            isConnected.postValue(false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.postValue(
                if (metadata?.description?.mediaId == null) {
                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    companion object {
        @Volatile
        private var instance: MusicServiceConnection? = null

        fun getInstance(context: Context, serviceComponent: ComponentName) =
            instance ?: synchronized(this) {
                instance ?: MusicServiceConnection(context, serviceComponent).also { instance = it }
            }
    }
}

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()