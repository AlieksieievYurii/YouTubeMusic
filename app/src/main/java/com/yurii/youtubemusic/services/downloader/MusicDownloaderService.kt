package com.yurii.youtubemusic.services.downloader

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.VideoItem
import java.lang.Exception

const val TAG = "MusicDownloadService"

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
        Log.i(TAG, "The service has been created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "The service has called onStarCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "The service has been bind")
        return ServiceInterface()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "The service has been destroyed")
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