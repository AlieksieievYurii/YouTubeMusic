package com.yurii.youtubemusic.services.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.utilities.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class PlaybackState {
    object None : PlaybackState()
    data class Playing(
        val mediaItem: MediaItem,
        val category: Category,
        val isPaused: Boolean,
        private val position: Long,
        private val lastUpdateTime: Long,
        private val playbackSpeed: Float,
    ) : PlaybackState() {
        val currentPosition: Long
            get() {
                val timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime
                return (position + (timeDelta * playbackSpeed)).toLong()
            }
    }
}

@Singleton
class MediaServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueModesRepository: QueueModesRepository,
    private val playlistRepository: PlaylistRepository
) {
    private val _isMediaControllerConnected = MutableStateFlow(false)
    val isMediaControllerConnected = _isMediaControllerConnected.asStateFlow()

    val isQueueShuffle = queueModesRepository.getIsShuffle()

    val isQueueLooped = queueModesRepository.getIsLooped()

    val allPlaylists: Flow<List<MediaItemPlaylist>> = playlistRepository.getPlaylists()

    private val _errors: MutableSharedFlow<Exception> = MutableSharedFlow(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MediaService::class.java),
        mediaBrowserConnectionCallback, null
    )

    private var mediaController: MediaControllerCompat? = null

    private val _playbackState: MutableStateFlow<PlaybackState> =
        MutableStateFlow(PlaybackState.None)
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

    fun seekTo(position: Long) = getMediaController().transportControls.seekTo(position)

    suspend fun setLoopState(isLooped: Boolean) = queueModesRepository.setLoop(isLooped)

    suspend fun setShuffleState(isShuffled: Boolean) = queueModesRepository.setShuffle(isShuffled)

    suspend fun getMediaItemsFor(category: Category): List<MediaItem> =
        suspendCoroutine { callback ->
            val mediaItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    callback.resume(children.map { it.toMediaItem() })
                }
            }

            mediaBrowser.subscribe(category.id.toString(), mediaItemsSubscription)
        }

    private fun getMediaController(): MediaControllerCompat =
        mediaController
            ?: throw IllegalStateException("Can not get mediaController because it is not initialized")

    suspend fun getCategories(): List<Category> = suspendCoroutine { callback ->
        val categoryItemsSubscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
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
            mediaController = MediaControllerCompat(
                context,
                mediaBrowser.sessionToken
            ).apply { registerCallback(MediaControllerCallback()) }
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
                    _playbackState.value = PlaybackState.Playing(
                        mediaItem!!,
                        category!!,
                        false,
                        state.position,
                        state.lastPositionUpdateTime,
                        state.playbackSpeed
                    )
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    _playbackState.value =
                        PlaybackState.Playing(
                            mediaItem!!,
                            category!!,
                            true,
                            state.position,
                            state.lastPositionUpdateTime,
                            state.playbackSpeed
                        )
                }
                PlaybackStateCompat.STATE_STOPPED -> _playbackState.value = PlaybackState.None
                else -> {
                    //Do nothing
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Timber.d("MediaControllerCallback -> onMetadataChanged: $metadata")
        }

        override fun onSessionEvent(event: String, extras: Bundle) {
            super.onSessionEvent(event, extras)
            if (event in arrayOf(
                    FAILED_TO_LOAD_MEDIA_ITEMS,
                    FAILED_TO_LOAD_CATEGORIES,
                    BROKEN_MEDIA_ITEM,
                    PLAYER_ERROR
                )
            )
                _errors.tryEmit(extras.getSerializable(EXTRA_EXCEPTION) as Exception)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Timber.d("MediaControllerCallback -> onSessionDestroyed")
        }
    }
}