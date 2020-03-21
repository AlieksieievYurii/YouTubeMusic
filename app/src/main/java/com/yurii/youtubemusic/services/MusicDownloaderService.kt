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
import com.yurii.youtubemusic.models.VideoItem
import java.io.File
import java.io.IOException


class MusicDownloaderService : Service(), DownloaderInteroperableInterface {
    companion object {
        const val EXTRA_VIDEO_ITEM: String = "com.yurii.youtubemusic.download.item"
        const val DOWNLOADING_PROGRESS_ACTION: String = "com.yurii.youtubemusic.downloading.progress.action"
        const val DOWNLOADING_FINISHED_ACTION: String = "com.yurii.youtubemusic.downloading.finished.action"
        const val DOWNLOADING_FAILED_ACTION: String = "com.yurii.youtubemusic.downloading.failed.action"
        const val EXTRA_VIDEO_ID: String = "com.yurii.youtubemusic.video.id"
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
        val outFolder = File(applicationContext.filesDir, "musics")
        FormattedVideoTask { youtubeVideo, audioFormat ->
            youtubeVideo.downloadAsync(audioFormat, outFolder, object : OnYoutubeDownloadListener {
                override fun onDownloading(progress: Int) {
                    localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_PROGRESS_ACTION).also {
                        it.putExtra(EXTRA_VIDEO_ID, videoItem.videoId)
                        it.putExtra(EXTRA_PROGRESS, progress)
                    })
                }

                override fun onFinished(file: File) {
                    stopSelf(startId)
                    executionVideoItems.remove(videoItem)
                    amendMusicName(file, videoItem)

                    localBroadcastManager.sendBroadcast(Intent(DOWNLOADING_FINISHED_ACTION).also {
                        it.putExtra(EXTRA_VIDEO_ID, videoItem.videoId)
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
    to proper name like this -> 12424aqz.mp3 where prefix is YouTube video's id
     **/
    private fun amendMusicName(file: File, videoItem: VideoItem) {
        val newMusicFile = file.resolveSibling("${videoItem.videoId}.mp3")
        file.renameTo(newMusicFile)
    }

    override fun isLoading(videoItem: VideoItem): Boolean = executionVideoItems.find { it == videoItem } != null

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