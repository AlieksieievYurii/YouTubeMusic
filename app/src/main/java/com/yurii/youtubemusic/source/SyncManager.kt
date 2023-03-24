package com.yurii.youtubemusic.source

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.models.YouTubePlaylistSync
import com.yurii.youtubemusic.models.toVideoItem
import com.yurii.youtubemusic.screens.youtube.YouTubeAPI
import com.yurii.youtubemusic.services.downloader.DownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class YouTubePlaylistSynchronizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val downloadManager: DownloadManager,
    private val youTubeAPI: YouTubeAPI,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository,
    private val mediaRepository: MediaRepository
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val youTubePlaylistsIds = getAllYouTubePlaylistsIds()

            youTubePlaylistSyncRepository.youTubePlaylistSyncs.first().forEach {
                if (it.youTubePlaylistId in youTubePlaylistsIds) {
                    synchronizeMediaItems(it)
                } else {
                    //TODO handle when youTube playlist is registered to sync but on the YouTube it is missing
                }
            }
            Result.success()
        } catch (error: Exception) {
            Timber.e("Error occurred during YouTube Playlist synchronization: ${error.message}")
            return Result.retry()
        }
    }

    private suspend fun synchronizeMediaItems(youTubePlaylistSync: YouTubePlaylistSync) {
        getAllVideoItemsFromYouTubePlaylist(youTubePlaylistSync.youTubePlaylistId).forEach {
            if (!mediaRepository.exists(it.id))
                downloadManager.enqueue(it, youTubePlaylistSync.mediaItemPlaylists)
        }
    }

    private suspend fun getAllYouTubePlaylistsIds(): List<String> {
        val ids = mutableSetOf<String>()
        var nextPage: String? = null
        do {
            val res = youTubeAPI.getMyPlaylists(nextPage)
            ids.addAll(res.items.map { it.id })
            nextPage = res.nextPageToken

        } while (nextPage != null)

        return ids.toList()
    }

    private suspend fun getAllVideoItemsFromYouTubePlaylist(playlistId: String): List<VideoItem> {
        val videoItems = mutableListOf<VideoItem>()
        var nextPage: String? = null
        do {
            val res = youTubeAPI.getItemsFromPlaylist(playlistId, nextPage)
            nextPage = res.nextPageToken
            if (res.items.isNotEmpty()) {
                val videos = youTubeAPI.getVideosDetails(res.items.map { it.snippet.resourceId.videoId }).items
                videoItems.addAll(videos.map { it.toVideoItem() })
            }
        } while (nextPage != null)

        return videoItems
    }

}

@Singleton
class SyncManager @Inject constructor(private val workManager: WorkManager) {

    suspend fun isOn(): Boolean {
        val works = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME).await()
        return works.isNotEmpty() && works.first().state != WorkInfo.State.CANCELLED

    }

    fun turnOn() {
        val request = PeriodicWorkRequestBuilder<YouTubePlaylistSynchronizationWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun turnOff() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "YouTubePlaylistSynchronizer"
    }

}