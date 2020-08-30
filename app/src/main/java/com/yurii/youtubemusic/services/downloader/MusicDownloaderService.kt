package com.yurii.youtubemusic.services.downloader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kiulian.downloader.YoutubeDownloader
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.models.VideoItem
import java.io.*
import java.lang.Exception

interface DownloaderInteroperableInterface {
    fun isLoading(videoItem: VideoItem): Boolean
    fun getProgress(videoItem: VideoItem): Progress?
    fun cancel(videoItem: VideoItem)
}

interface ExecutionUpdate {
    fun onProgress(videoItem: VideoItem, progress: Progress)
    fun onFinished(videoItem: VideoItem, outFile: File, startId: Int)
    fun onError(videoItem: VideoItem, exception: Exception, startId: Int)
}

class MusicDownloaderService : Service(), DownloaderInteroperableInterface, ExecutionUpdate {
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val youtubeDownloader = YoutubeDownloader()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service has been created")
        Instance.serviceInterface = this
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val videoItem = getVideoItemFromIntent(intent)
        startDownloading(videoItem, startId)

        return START_REDELIVER_INTENT
    }

    private fun getVideoItemFromIntent(intent: Intent): VideoItem {
        return intent.extras?.getSerializable(EXTRA_VIDEO_ITEM) as? VideoItem
            ?: throw IOException("You must pass VideoItem object by key $EXTRA_VIDEO_ITEM to perform downloading")
    }

    private fun startDownloading(videoItem: VideoItem, startId: Int) {
        Log.i(TAG, "Start downloading: ${videoItem.videoId}. StartId: $startId")
        val outDir = DataStorage.getMusicStorage(applicationContext)
        val task = VideoItemTask(videoItem, outDir, startId, this, youtubeDownloader)
        ThreadPool.execute(task)
    }

    override fun onProgress(videoItem: VideoItem, progress: Progress) {
        Log.i(TAG, "Progress: ${videoItem.videoId}. Progress: $progress")
        localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_PROGRESS_ACTION).also {
            it.putExtra(EXTRA_VIDEO_ITEM, videoItem)
            it.putExtra(EXTRA_PROGRESS, progress)
        })
    }

    override fun onFinished(videoItem: VideoItem, outFile: File, startId: Int) {
        stopSelf(startId)
        localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_FINISHED_ACTION).also {
            it.putExtra(EXTRA_VIDEO_ITEM, videoItem)
        })
    }

    override fun onError(videoItem: VideoItem, exception: Exception, startId: Int) {
        stopSelf(startId)
        localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_FAILED_ACTION).also {
            it.putExtra(EXTRA_VIDEO_ITEM, videoItem)
            it.putExtra(EXTRA_ERROR, exception)
        })
    }


    override fun cancel(videoItem: VideoItem) {
        ThreadPool.cancel(videoItem)
    }

    override fun isLoading(videoItem: VideoItem): Boolean = ThreadPool.findTask(videoItem) != null

    override fun getProgress(videoItem: VideoItem): Progress? = ThreadPool.findTask(videoItem)?.progress

    override fun onDestroy() {
        super.onDestroy()
        Instance.serviceInterface = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    object Instance {
        var serviceInterface: DownloaderInteroperableInterface? = null
    }

    companion object {
        const val EXTRA_VIDEO_ITEM: String = "com.yurii.youtubemusic.download.item"
        const val DOWNLOADING_PROGRESS_ACTION: String = "com.yurii.youtubemusic.downloading.currentProgress.action"
        const val DOWNLOADING_FINISHED_ACTION: String = "com.yurii.youtubemusic.downloading.finished.action"
        const val DOWNLOADING_FAILED_ACTION: String = "com.yurii.youtubemusic.downloading.failed.action"
        const val EXTRA_PROGRESS: String = "com.yurii.youtubemusic.video.currentProgress"
        const val EXTRA_ERROR: String = "com.yurii.youtubemusic.video.error"

        const val TAG = "YouTubeMusicDownloader"
    }
}