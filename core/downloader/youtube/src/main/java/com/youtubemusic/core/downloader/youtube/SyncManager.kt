package com.youtubemusic.core.downloader.youtube

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.youtubemusic.core.data.repository.MediaRepository
import com.youtubemusic.core.data.repository.YouTubePlaylistSyncRepository
import com.youtubemusic.core.data.repository.YouTubeRepository
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
    private val youTubeRepository: YouTubeRepository,
    private val youTubePlaylistSyncRepository: YouTubePlaylistSyncRepository,
    private val mediaRepository: MediaRepository
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val youTubePlaylistsIds = youTubeRepository.getAllMyPlaylistsIds()

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

    private suspend fun synchronizeMediaItems(youTubePlaylistSync: com.youtubemusic.core.model.YouTubePlaylistSync) {
        youTubeRepository.getAllVideoItemsFromPlaylist(youTubePlaylistSync.youTubePlaylistId).forEach {
            if (!mediaRepository.exists(it.id))
                downloadManager.enqueue(it, youTubePlaylistSync.mediaItemPlaylists)
        }
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