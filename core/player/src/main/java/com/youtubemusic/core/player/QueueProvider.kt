package com.youtubemusic.core.player

import android.support.v4.media.session.MediaSessionCompat
import com.youtubemusic.core.data.repository.MediaRepository
import com.youtubemusic.core.data.repository.PlaylistRepository
import com.youtubemusic.core.data.repository.QueueModesRepository
import com.youtubemusic.core.model.MediaItem
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.isDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


class QueueProvider @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val queueModesRepository: QueueModesRepository
) {

    private val queue = ArrayList<MediaItem>()

    private val job = Job()
    private val queueScope = CoroutineScope(job)

    private var observingMediaItemsJob: Job? = null
    private var observingShuffleModeJob: Job? = null

    var playingMediaItem: MediaItem? = null
        private set

    var playingPlaylist: MediaItemPlaylist? = null
        private set

    var mediaSession: MediaSessionCompat? = null

    var playingItemRemovedCallback: (() -> Unit)? = null

    suspend fun prepareQueue(playlist: MediaItemPlaylist) {
        if (playlist == playingPlaylist)
            return

        playingPlaylist = playlist

        if (playlist.isDefault())
            observeAllMediaItems().join()
        else
            observeMediaItems(playlist).join()

    }

    fun play(mediaItemId: String) {
        playingMediaItem = queue.find { it.id == mediaItemId }
    }

    suspend fun next() {
        val isLooped = queueModesRepository.getIsLooped().first()
        if (!isLooped)
            skipToNext()
    }

    fun skipToNext() {
        if (playingMediaItem == null)
            return

        val index = queue.indexOf(playingMediaItem)
        playingMediaItem = if (index < queue.lastIndex)
            queue[index + 1]
        else
            queue.first()

    }

    fun skipToPrevious() {
        if (playingMediaItem == null)
            return

        val index = queue.indexOf(playingMediaItem)

        playingMediaItem = if (index > 0)
            queue[index - 1]
        else
            queue.last()
    }

    fun releaseQueue() {
        job.cancel()
    }

    private fun observeAllMediaItems(): Job {
        observingMediaItemsJob?.cancel()
        val finishJob = Job()
        observingMediaItemsJob = queueScope.launch {
            mediaRepository.getOrderedMediaItems().collect { mediaItems ->
                setQueue(mediaItems)
                finishJob.complete()
            }
        }
        return finishJob
    }

    private fun observeMediaItems(playlist: MediaItemPlaylist): Job {
        observingMediaItemsJob?.cancel()
        val finishJob = Job()
        observingMediaItemsJob = queueScope.launch {
            playlistRepository.getMediaItemsFor(playlist).collect { mediaItems ->
                setQueue(mediaItems)
                finishJob.complete()
            }
        }
        return finishJob
    }

    private suspend fun setQueue(mediaItems: List<MediaItem>) {
        val finishJob = Job()

        if (playingMediaItem != null && !mediaItems.contains(playingMediaItem))
            playingItemRemovedCallback?.invoke()

        observingShuffleModeJob?.cancel()
        observingShuffleModeJob = queueScope.launch {
            queueModesRepository.getIsShuffle().collect { isShuffled ->
                queue.clear()
                queue.addAll(if (isShuffled) mediaItems.shuffled() else mediaItems)
                setQueueInMediaSession()
                finishJob.complete()
            }
        }
        finishJob.join()
    }

    private fun setQueueInMediaSession() {
        var id = 0L
        mediaSession?.setQueue(queue.map { MediaSessionCompat.QueueItem(it.getMediaDescriptionCompat(), id++) })
        mediaSession?.setQueueTitle("Queue from the playlist '$playingPlaylist'")
    }
}