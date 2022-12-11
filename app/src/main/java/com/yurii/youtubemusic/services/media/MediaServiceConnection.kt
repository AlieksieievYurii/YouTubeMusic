package com.yurii.youtubemusic.utilities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.services.media.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalStateException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class PlaybackState {
    object None : PlaybackState()
    data class Playing(val mediaItem: MediaItem) : PlaybackState()
    data class Paused(val mediaItem: MediaItem) : PlaybackState()
}

class MediaServiceConnection private constructor(private val context: Context) {
    private val _isMediaControllerConnected = MutableStateFlow(false)
    val isMediaControllerConnected = _isMediaControllerConnected.asStateFlow()

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MediaService::class.java),
        mediaBrowserConnectionCallback, null
    )

    private var mediaController: MediaControllerCompat? = null

    private val _playbackState: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState.None)
    val playbackState = _playbackState.asStateFlow()

    init {
        mediaBrowser.connect()
    }

    fun play(mediaItem: MediaItem, category: Category) {
        val extras = Bundle().apply {
            putParcelable(EXTRA_KEY_CATEGORIES, category)
        }
        mediaController?.transportControls?.playFromMediaId(mediaItem.id, extras)
            ?: throw IllegalStateException("Can not 'Play' media item because mediaController is not initialized")
    }

    fun pause() {
        mediaController?.transportControls?.pause()
            ?: throw IllegalStateException("Can not 'Pause' playing because mediaController is not initialized")
    }

    fun resume() {
        mediaController?.transportControls?.play()
            ?: throw IllegalStateException("Can not 'Resume' playing because mediaController is not initialized")
    }

    fun notifyItemIsDeleted(item: Item) {
        mediaController?.sendCommand(REQUEST_COMMAND_DELETE_MEDIA_ITEM, Bundle().apply {
            putString(EXTRA_MEDIA_ITEM, item.id)
        }, null) ?: throw IllegalStateException("Can not 'notifyItemIsDeleted' because mediaController is not initialized")
    }

    suspend fun getMediaItemsFor(category: Category): List<MediaItem> = suspendCoroutine { callback ->
        val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)
                callback.resume(children.map { MediaItem.createFrom(it) })
            }

            override fun onError(parentId: String, options: Bundle) {
                super.onError(parentId, options)
                throw options.getSerializable(EXTRA_ERROR_MESSAGE) as? Exception
                    ?: IllegalStateException("Error occurred during requesting media items for $category. Can not get Error")
            }
        }

        mediaBrowser.subscribe(category.id.toString(), mediaItemsSubscription)
    }

    suspend fun getCategories(): List<Category> = suspendCoroutine { callback ->
        val categoryItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)
                callback.resume(children.map { Category.createFrom(it) })
            }

            override fun onError(parentId: String, options: Bundle) {
                super.onError(parentId, options)
                throw options.getSerializable(EXTRA_ERROR_MESSAGE) as? Exception
                    ?: IllegalStateException("Error occurred during requesting Categories. Can not get Error")
            }
        }
        mediaBrowser.subscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
    }

    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Timber.d("MediaBrowserConnectionCallback -> OnConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply { registerCallback(MediaControllerCallback()) }
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Timber.d("MediaBrowserConnectionCallback -> onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Timber.d("MediaBrowserConnectionCallback -> onConnectionFailed")
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            val mediaItem = state.extras?.getParcelable<MediaItem>(PLAYBACK_STATE_MEDIA_ITEM)
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    _playbackState.value = PlaybackState.Playing(mediaItem!!)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    _playbackState.value = PlaybackState.Paused(mediaItem!!)
                }
                PlaybackStateCompat.STATE_STOPPED -> _playbackState.value = PlaybackState.None
                else -> {
                    //Do nothing
                }
            }
            Timber.d("MediaControllerCallback -> onPlaybackStateChanged: ${state}")
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Timber.d("MediaControllerCallback -> onMetadataChanged: $metadata")
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Timber.d("MediaControllerCallback -> onSessionDestroyed")
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MediaServiceConnection? = null

        fun getInstance(context: Context): MediaServiceConnection {
            if (instance == null)
                synchronized(this) {
                    instance = MediaServiceConnection(context)
                }
            return instance!!
        }
    }
}