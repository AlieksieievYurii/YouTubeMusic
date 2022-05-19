package com.yurii.youtubemusic.screens.youtube.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.MainThread
import com.yurii.youtubemusic.screens.youtube.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem

import java.lang.Exception

class ServiceConnectionException : Exception("Cannot connect to the service")

class ServiceConnection(private val context: Context) : ServiceConnection {
    @MainThread
    interface CallBack {
        fun onFinished(videoItem: VideoItem)
        fun onProgress(videoItem: VideoItem, progress: Progress)
        fun onError(videoItem: VideoItem, error: Exception)
    }

    private val serviceIntent = Intent(context, MusicDownloaderService::class.java)
    private var downloaderCallBacks: CallBack? = null
    private var service: MusicDownloaderService.ServiceInterface? = null
    private var isConnected = false

    @Throws(ServiceConnectionException::class)
    fun connect() {
        val status = context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
        if (!status)
            throw ServiceConnectionException()
    }

    fun setCallbacks(callBacks: CallBack) {
        downloaderCallBacks = callBacks
    }

    fun download(videoItem: VideoItem, categories: List<Category>) = service?.downloadMusicFrom(videoItem, categories)

    fun retryToDownload(videoItem: VideoItem) = service?.retryToDownload(videoItem)

    fun cancelDownloading(videoItem: VideoItem) = service?.cancelDownloading(videoItem)

    fun isItemDownloading(videoItem: VideoItem): Boolean = service?.isItemDownloading(videoItem) ?: false

    fun getProgress(videoItem: VideoItem): Progress? = service?.getProgress(videoItem)

    fun isDownloadingFailed(videoItem: VideoItem): Boolean = service?.isDownloadingFailed(videoItem) ?: false

    fun getError(videoItem: VideoItem): Exception? = service?.getLastError(videoItem)

    override fun onServiceDisconnected(name: ComponentName?) {
        isConnected = false
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        isConnected = true
        this.service = (service as MusicDownloaderService.ServiceInterface).also {
            it.setCallBacks(downloaderCallBacks)
        }
    }
}