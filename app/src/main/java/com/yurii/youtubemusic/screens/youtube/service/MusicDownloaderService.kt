package com.yurii.youtubemusic.screens.youtube.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import java.lang.Exception


class MusicDownloaderService : Service() {
    private lateinit var downloader: MusicDownloaderAbstract
    private lateinit var notificationManager: NotificationManager
    private var serviceConnectionCallback: ServiceConnection.CallBack? = null
    private lateinit var handler: Handler
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        downloader = MusicDownloaderImp(this, MusicDownloaderCallBacks())
        notificationManager = NotificationManager(this)
        handler = Handler(applicationContext.mainLooper)
    }

    override fun onBind(intent: Intent?): IBinder? {
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

    private inner class MusicDownloaderCallBacks : MusicDownloaderAbstract.CallBack {
        override fun onFinished(videoItem: VideoItem) {
            stopAsForegroundIfQueueIsEmpty()
            handler.post { serviceConnectionCallback?.onFinished(videoItem) }
        }

        override fun onChangeProgress(videoItem: VideoItem, progress: Progress) {
            notificationManager.updateProgress(downloader.getCompletedProgress())
            handler.post { serviceConnectionCallback?.onProgress(videoItem, progress) }
        }

        override fun onErrorOccurred(videoItem: VideoItem, error: Exception) {
            stopAsForegroundIfQueueIsEmpty()
            handler.post { serviceConnectionCallback?.onError(videoItem, error) }
        }

    }

    inner class ServiceInterface : Binder() {
        fun setCallBacks(callBacks: ServiceConnection.CallBack?) {
            serviceConnectionCallback = callBacks
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