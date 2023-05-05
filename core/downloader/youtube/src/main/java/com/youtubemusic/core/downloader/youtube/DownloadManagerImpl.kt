package com.youtubemusic.core.downloader.youtube

import androidx.work.*
import com.youtubemusic.core.common.asFlow
import com.youtubemusic.core.data.repository.MediaFileRepository
import com.youtubemusic.core.data.repository.MediaLibraryDomain
import com.youtubemusic.core.data.repository.MediaRepository
import com.youtubemusic.core.downloader.youtube.di.MainScope
import com.youtubemusic.core.model.MediaItemPlaylist
import com.youtubemusic.core.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val mediaRepository: MediaRepository,
    @MainScope private val coroutineScope: CoroutineScope,
    private val mediaStorage: MediaFileRepository,
    private val mediaLibraryDomain: MediaLibraryDomain
) : DownloadManager {
    private data class CacheItem(val status: DownloadManager.Status, val downloadingJobId: UUID?)

    private val cache = ConcurrentHashMap<String, CacheItem>()

    private val statusesFlow =
        MutableSharedFlow<DownloadManager.Status>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        coroutineScope.launch { synchronize() }
        coroutineScope.launch { bindSynchronizationOfCacheWithMediaItems() }
        coroutineScope.launch { observeWorkManagerStatuses() }
    }

    private suspend fun observeWorkManagerStatuses() {
        workManager.getWorkInfosByTagLiveData(TAG_DOWNLOADING).asFlow().collect { downloadingJobs ->
            downloadingJobs.forEach { downloadingJob ->
                val cacheItem = cache.entries.find { it.value.downloadingJobId == downloadingJob.id }
                if (cacheItem != null) {
                    val status = getStatus(downloadingJob, cacheItem.key)
                    if (status != cacheItem.value.status) {
                        cache[cacheItem.key] = CacheItem(status, downloadingJob.id)

                        if (downloadingJob.state == WorkInfo.State.SUCCEEDED)
                            mediaLibraryDomain.setMediaItemAsDownloaded(cacheItem.key)
                        else if (downloadingJob.state == WorkInfo.State.CANCELLED)
                            mediaRepository.getMediaItem(cacheItem.key)?.let { mediaRepository.delete(it) }

                        statusesFlow.emit(status)
                    }
                }
            }
        }
    }

    private suspend fun bindSynchronizationOfCacheWithMediaItems() {
        mediaRepository.mediaItemCores.collect { mediaItems ->
            mediaItems.forEach {
                if (it.downloadingJobUUID != null) {
                    val status = DownloadManager.Status(it.mediaItem.id, DownloadManager.State.Downloading(0, 0, 0))
                    cache.putIfAbsent(it.mediaItem.id, CacheItem(status, it.downloadingJobUUID))
                } else {
                    val fileSize = mediaStorage.getMediaFile(it.mediaItem.id).length()
                    val status = DownloadManager.Status(it.mediaItem.id, DownloadManager.State.Downloaded(fileSize))
                    cache.putIfAbsent(it.mediaItem.id, CacheItem(status, null))
                }
            }
            cache.keys.forEach { mediaItemId ->
                if (mediaItems.find { it.mediaItem.id == mediaItemId } == null) {
                    cache.remove(mediaItemId)
                    statusesFlow.emit(DownloadManager.Status(mediaItemId, DownloadManager.State.Download))
                }
            }
        }
    }

    private suspend fun synchronize() {
        mediaRepository.mediaItemCores.first().forEach {
            if (it.downloadingJobUUID != null) {
                val workInfo: WorkInfo? = workManager.getWorkInfoById(it.downloadingJobUUID!!).await()
                if (workInfo == null) {
                    Timber.e("Work is not found for ${it.downloadingJobUUID}")
                    mediaLibraryDomain.deleteMediaItem(it.mediaItem)
                }
            }
        }
    }

    override fun getDownloadingJobs(): Flow<List<DownloadingJob>> {
        return mediaRepository.downloadingMediaItemEntities.map { downloadingMediaItems ->
            downloadingMediaItems.map { DownloadingJob(it.mediaItem, it.thumbnailUrl, it.downloadingJobUUID!!) }
        }
    }

    override suspend fun enqueue(videoItem: VideoItem, playlists: List<MediaItemPlaylist>) {
        setDownloadingStatus(videoItem.id)
        val alreadyExists = mediaRepository.getMediaItemCore(videoItem.id) != null
        if (!alreadyExists) {
            val downloadingJobId = enqueueDownloadingJob(videoItem)
            mediaLibraryDomain.registerDownloadingMediaItem(videoItem, playlists, downloadingJobId)
            setDownloadingStatus(videoItem.id, downloadingJobId)
        }
    }

    override suspend fun retry(videoId: String) {
        setDownloadingStatus(videoId)
        val downloadingItem = mediaRepository.getMediaItemCore(videoId)
        if (downloadingItem?.downloadingJobUUID != null) {
            val downloadingJobId = enqueueDownloadingJob(downloadingItem.mediaItem.id, downloadingItem.thumbnailUrl)
            mediaRepository.updateDownloadingJobId(downloadingItem.mediaItem, downloadingJobId)
            setDownloadingStatus(videoId, downloadingJobId)
        } else
            throw IllegalStateException("Can not retry to download failed media item")
    }

    override suspend fun cancel(videoId: String) {
        statusesFlow.emit(DownloadManager.Status(videoId, DownloadManager.State.Download))
        cache[videoId]?.downloadingJobId?.let {
            workManager.cancelWorkById(it)
            val item = mediaRepository.getMediaItem(videoId)
            if (item != null)
                mediaLibraryDomain.deleteMediaItem(item)
        }
    }

    override fun getDownloadingJobState(videoId: String): DownloadManager.State {
        val cacheItem = cache.entries.find { it.key == videoId }
        return cacheItem?.value?.status?.state ?: DownloadManager.State.Download
    }

    override fun observeStatus(): Flow<DownloadManager.Status> = statusesFlow.asSharedFlow()

    private fun getStatus(downloadingJobWorkInfo: WorkInfo, mediaItemId: String): DownloadManager.Status {
        return DownloadManager.Status(
            mediaItemId, when (downloadingJobWorkInfo.state) {
                WorkInfo.State.ENQUEUED -> DownloadManager.State.Downloading(0, 0, 0)
                WorkInfo.State.RUNNING -> DownloadManager.State.Downloading(
                    progress = downloadingJobWorkInfo.progress.getInt(MusicDownloadWorker.PROGRESS, 0),
                    currentSize = downloadingJobWorkInfo.progress.getLong(MusicDownloadWorker.PROGRESS_DOWNLOADED_SIZE, 0),
                    size = downloadingJobWorkInfo.progress.getLong(MusicDownloadWorker.PROGRESS_TOTAL_SIZE, 0)
                )

                WorkInfo.State.SUCCEEDED -> DownloadManager.State.Downloaded(
                    downloadingJobWorkInfo.outputData.getLong(MusicDownloadWorker.MEDIA_SIZE, 0)
                )

                WorkInfo.State.FAILED -> DownloadManager.State.Failed(
                    downloadingJobWorkInfo.outputData.getString(MusicDownloadWorker.ERROR_MESSAGE)
                )

                WorkInfo.State.BLOCKED -> TODO("Unhandled")
                WorkInfo.State.CANCELLED -> DownloadManager.State.Download
            }
        )
    }

    private fun enqueueDownloadingJob(videoItem: VideoItem): UUID = enqueueDownloadingJob(videoItem.id, videoItem.normalThumbnail)

    private fun enqueueDownloadingJob(videoId: String, thumbnailUrl: String): UUID {
        val data = workDataOf(
            MusicDownloadWorker.ARG_VIDEO_ID to videoId,
            MusicDownloadWorker.ARG_VIDEO_THUMBNAIL_URL to thumbnailUrl
        )

        val request = OneTimeWorkRequestBuilder<MusicDownloadWorker>().also {
            it.setInputData(data)
            it.addTag(TAG_DOWNLOADING)
        }.build()

        workManager.enqueue(request)

        return request.id
    }

    private suspend fun setDownloadingStatus(itemId: String, downloadingJobId: UUID? = null) {
        val status = DownloadManager.Status(itemId, DownloadManager.State.Downloading(0, 0, 0))
        setStatus(CacheItem(status, downloadingJobId))
    }

    private suspend fun setStatus(cacheItem: CacheItem) {
        cache[cacheItem.status.videoId] = cacheItem
        statusesFlow.emit(cacheItem.status)
    }

    companion object {
        private const val TAG_DOWNLOADING = "downloading"
    }
}