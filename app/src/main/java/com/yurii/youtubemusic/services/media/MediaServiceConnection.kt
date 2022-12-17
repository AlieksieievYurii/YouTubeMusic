package com.yurii.youtubemusic.services.media

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.yurii.youtubemusic.models.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalStateException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class PlaybackState {
    object None : PlaybackState()
    data class Playing(val mediaItem: MediaItem, val category: Category, val currentPosition: Long) : PlaybackState()
    data class Paused(val mediaItem: MediaItem, val category: Category, val currentPosition: Long) : PlaybackState()
}

class MediaServiceConnection private constructor(private val application: Application) {
    private val _isMediaControllerConnected = MutableStateFlow(false)
    val isMediaControllerConnected = _isMediaControllerConnected.asStateFlow()

    private val _errors: MutableSharedFlow<Exception> = MutableSharedFlow(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()

    private val mediaBrowser = MediaBrowserCompat(
        application,
        ComponentName(application, MediaService::class.java),
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
        getMediaController().transportControls.playFromMediaId(mediaItem.id, extras)
    }

    fun pause() = getMediaController().transportControls.pause()

    fun resume() = getMediaController().transportControls.play()

    fun stop() = getMediaController().transportControls.stop()

    fun skipToNextTrack() = getMediaController().transportControls.skipToNext()

    fun skipToPreviousTrack() = getMediaController().transportControls.skipToPrevious()

    suspend fun getMediaItemsFor(category: Category): List<MediaItem> = suspendCoroutine { callback ->
        val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)
                callback.resume(children.map { MediaItem.createFrom(it) })
            }
        }

        mediaBrowser.subscribe(category.id.toString(), mediaItemsSubscription)
    }

    private fun getMediaController(): MediaControllerCompat =
        mediaController ?: throw IllegalStateException("Can not get mediaController because it is not initialized")

    suspend fun getCategories(): List<Category> = suspendCoroutine { callback ->
        val categoryItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)
                callback.resume(children.map { Category.createFrom(it) })
            }
        }
        mediaBrowser.subscribe(CATEGORIES_CONTENT, categoryItemsSubscription)
    }

    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Timber.d("MediaBrowserConnectionCallback -> OnConnected")
            mediaController = MediaControllerCompat(application, mediaBrowser.sessionToken).apply { registerCallback(MediaControllerCallback()) }
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
            val category = state.extras?.getParcelable<Category>(PLAYBACK_STATE_PLAYING_CATEGORY)
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    _playbackState.value = PlaybackState.Playing(mediaItem!!, category!!, state.position)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    _playbackState.value = PlaybackState.Paused(mediaItem!!, category!!, state.position)
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

        override fun onSessionEvent(event: String, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if (event == FAILED_TO_LOAD_MEDIA_ITEMS || event == FAILED_TO_LOAD_CATEGORIES || event == BROKEN_MEDIA_ITEM)
                _errors.tryEmit(extras?.getSerializable(EXTRA_EXCEPTION) as? Exception ?: Exception("Unknown"))
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Timber.d("MediaControllerCallback -> onSessionDestroyed")
        }
    }

    companion object {

        @Volatile
        private var instance: MediaServiceConnection? = null

        fun getInstance(application: Application): MediaServiceConnection {
            if (instance == null)
                synchronized(this) {
                    instance = MediaServiceConnection(application)
                }
            return instance!!
        }
    }
}