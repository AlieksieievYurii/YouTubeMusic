package com.yurii.youtubemusic.services

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kiulian.downloader.OnYoutubeDownloadListener
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.formats.AudioFormat
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.models.VideoItem
import java.io.File
import java.io.IOException

interface DownloaderInteroperableInterface {
    companion object {
        const val NO_PROGRESS: Int = -1
    }

    fun isLoading(videoItem: VideoItem): Boolean
    fun getProgress(videoItem: VideoItem): Int
}

class MusicDownloaderService : Service(), DownloaderInteroperableInterface {
    companion object {
        const val EXTRA_VIDEO_ITEM: String = "com.yurii.youtubemusic.download.item"
        const val DOWNLOADING_PROGRESS_ACTION: String = "com.yurii.youtubemusic.downloading.progress.action"
        const val DOWNLOADING_FINISHED_ACTION: String = "com.yurii.youtubemusic.downloading.finished.action"
        const val DOWNLOADING_FAILED_ACTION: String = "com.yurii.youtubemusic.downloading.failed.action"
        const val EXTRA_PROGRESS: String = "com.yurii.youtubemusic.video.progress"
    }

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val executionVideoItems: MutableList<VideoItem> = ArrayList()

    override fun onCreate() {
        super.onCreate()
        Instance.serviceInterface = this
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (intent?.extras?.getSerializable(EXTRA_VIDEO_ITEM) as? VideoItem)?.let {
            startDownloading(it, startId)
            executionVideoItems.add(it)
        } ?: throw IOException("You must pass VideoItem object by key $EXTRA_VIDEO_ITEM to perform downloading")
        return START_REDELIVER_INTENT
    }

    private fun startDownloading(videoItem: VideoItem, startId: Int) {
        FormattedVideoTask { youtubeVideo, audioFormat ->
            youtubeVideo.downloadAsync(audioFormat, DataStorage.getMusicStorage(applicationContext), object : OnYoutubeDownloadListener {
                override fun onDownloading(progress: Int) {
                    videoItem.downloadingProgress = progress
                    localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_PROGRESS_ACTION).also {
                        it.putExtra(EXTRA_VIDEO_ITEM, videoItem)
                        it.putExtra(EXTRA_PROGRESS, progress)
                    })
                }

                override fun onFinished(file: File) {
                    stopSelf(startId)
                    executionVideoItems.remove(videoItem)
                    setCorrectFileName(file, videoItem)

                    localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_FINISHED_ACTION).also {
                        it.putExtra(EXTRA_VIDEO_ITEM, videoItem)
                    })
                }

                override fun onError(throwable: Throwable?) {
                    localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_FAILED_ACTION))
                }

            })
        }.execute(videoItem)
    }

    /**
    Change the name(title of the video) of the original [file]
    to proper name like this -> 12424aqz.mp3 where prefix is YouTube video's id which is retained from [videoItem]
     **/
    private fun setCorrectFileName(file: File, videoItem: VideoItem) {
        val newMusicFile = file.resolveSibling("${videoItem.videoId}.mp3")
        file.renameTo(newMusicFile)
    }

    override fun isLoading(videoItem: VideoItem): Boolean = executionVideoItems.find { it.videoId == videoItem.videoId } != null

    override fun getProgress(videoItem: VideoItem): Int =
        executionVideoItems.find { it.videoId == videoItem.videoId }?.downloadingProgress ?: DownloaderInteroperableInterface.NO_PROGRESS

    override fun onDestroy() {
        super.onDestroy()
        Instance.serviceInterface = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class FormattedVideoTask(private val onFinished: ((YoutubeVideo, AudioFormat) -> Unit)) : AsyncTask<VideoItem, Void, Void>() {
        override fun doInBackground(vararg downloadItem: VideoItem): Void? {
            val youTubeDownloader = YoutubeDownloader()
            val video = youTubeDownloader.getVideo(downloadItem.first().videoId)
            val audioFormat = video.audioFormats().last()

            onFinished.invoke(video, audioFormat)
            return null
        }
    }

    object Instance {
        var serviceInterface: DownloaderInteroperableInterface? = null
    }
}