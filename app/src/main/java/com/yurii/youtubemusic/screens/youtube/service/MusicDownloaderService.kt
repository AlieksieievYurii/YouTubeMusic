package com.yurii.youtubemusic.screens.youtube.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.utilities.MediaLibraryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.lang.Exception


class MusicDownloaderService : Service() {
    sealed class DownloadingReport {
        data class Successful(val videoItem: VideoItem) : DownloadingReport()
        data class Failed(val videoItem: VideoItem, val error: Exception) : DownloadingReport()
    }

    private val scopeJob = Job()
    private val serviceCoroutineScope = CoroutineScope(scopeJob)

    private var downloadingProgress: MutableSharedFlow<Pair<VideoItem, Progress>>? = null
    private var downloadingReport: MutableSharedFlow<DownloadingReport>? = null

    private lateinit var downloader: MusicDownloaderAbstract
    private lateinit var notificationManager: NotificationManager
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()

        downloader = MusicDownloaderImp(MusicDownloaderCallBacks(), MediaLibraryManager.getInstance(this))
        notificationManager = NotificationManager(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServiceInterface()
    }

    private fun startAsForegroundService() {
        if (!isForeground) {
            startForeground(NotificationManager.NOTIFICATION_ID, notificationManager.buildNotification(0))
            isForeground = true
        }
    }

    private fun stopAsForegroundIfQueueIsEmpty() {
        if (isForeground && downloader.isQueueEmpty()) {
            stopForeground(true)
            isForeground = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scopeJob.cancel()
    }

    private inner class MusicDownloaderCallBacks : MusicDownloaderAbstract.CallBack {
        override fun onFinished(videoItem: VideoItem) {
            stopAsForegroundIfQueueIsEmpty()
            serviceCoroutineScope.launch {
                downloadingReport?.emit(DownloadingReport.Successful(videoItem))
            }
        }

        override fun onChangeProgress(videoItem: VideoItem, progress: Progress) {
            notificationManager.updateProgress(downloader.getCompletedProgress())
            serviceCoroutineScope.launch {
                downloadingProgress?.emit(videoItem to progress)
            }
        }

        override fun onErrorOccurred(videoItem: VideoItem, error: Exception) {
            stopAsForegroundIfQueueIsEmpty()
            serviceCoroutineScope.launch {
                downloadingReport?.emit(DownloadingReport.Failed(videoItem, error))
            }
        }
    }

    inner class ServiceInterface : Binder() {

        fun setFlowCallbacks(
            downloadingReport: MutableSharedFlow<DownloadingReport>,
            downloadingProgress: MutableSharedFlow<Pair<VideoItem, Progress>>
        ) {
            this@MusicDownloaderService.downloadingReport = downloadingReport
            this@MusicDownloaderService.downloadingProgress = downloadingProgress
        }

        fun downloadMusicFrom(videoItem: VideoItem, categories: List<Category>) {
            startAsForegroundService()
            downloader.download(videoItem, categories)
        }

        fun retryToDownload(videoItem: VideoItem) {
            startAsForegroundService()
            downloader.retryToDownload(videoItem)
        }

        fun cancelDownloading(videoItem: VideoItem) {
            downloader.cancel(videoItem)
            stopAsForegroundIfQueueIsEmpty()
        }

        fun isItemDownloading(videoItem: VideoItem) = downloader.isItemDownloading(videoItem)

        fun isDownloadingFailed(videoItem: VideoItem) = downloader.isDownloadingFailed(videoItem)

        fun getLastError(videoItem: VideoItem) = downloader.getError(videoItem)

        fun getProgress(videoItem: VideoItem) = downloader.getProgress(videoItem)
    }
}