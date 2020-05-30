package com.yurii.youtubemusic.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.YoutubeException
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.formats.AudioFormat
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.models.VideoItem
import java.io.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.URL
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface DownloaderInteroperableInterface {
    companion object {
        const val NO_PROGRESS: Int = -1
    }

    fun isLoading(videoItem: VideoItem): Boolean
    fun getProgress(videoItem: VideoItem): Int
    fun cancel(videoItem: VideoItem)
}

interface ExecutionUpdate {
    fun onProgress(videoItem: VideoItem, progress: Int)
    fun onFinished(videoItem: VideoItem, outFile: File, startId: Int)
    fun onError(videoItem: VideoItem, exception: Exception, startId: Int)
}

class MusicDownloaderService : Service(), DownloaderInteroperableInterface, ExecutionUpdate {
    companion object {
        const val EXTRA_VIDEO_ITEM: String = "com.yurii.youtubemusic.download.item"
        const val DOWNLOADING_PROGRESS_ACTION: String = "com.yurii.youtubemusic.downloading.currentProgress.action"
        const val DOWNLOADING_FINISHED_ACTION: String = "com.yurii.youtubemusic.downloading.finished.action"
        const val DOWNLOADING_FAILED_ACTION: String = "com.yurii.youtubemusic.downloading.failed.action"
        const val EXTRA_PROGRESS: String = "com.yurii.youtubemusic.video.currentProgress"
        const val EXTRA_ERROR: String = "com.yurii.youtubemusic.video.error"

        const val TAG = "YouTubeMusicDownloader"
    }

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val executionVideoItems: MutableList<VideoItemTask> = mutableListOf()


    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service has been created")
        Instance.serviceInterface = this
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (intent?.extras?.getSerializable(EXTRA_VIDEO_ITEM) as? VideoItem)?.let {
            startDownloading(it, startId)
        } ?: throw IOException("You must pass VideoItem object by key $EXTRA_VIDEO_ITEM to perform downloading")
        return START_REDELIVER_INTENT
    }

    private fun startDownloading(videoItem: VideoItem, startId: Int) {
        Log.i(TAG, "Start downloading: ${videoItem.videoId}. StartId: $startId")
        val outDir = DataStorage.getMusicStorage(applicationContext)
        val task = VideoItemTask(videoItem, outDir, startId, this)
        check(!executionVideoItems.contains(task)) { "VideoItem $videoItem already is executing in the service" }
        executionVideoItems.add(task)
        ThreadPool.execute(task)
    }

    override fun onProgress(videoItem: VideoItem, progress: Int) {
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
            it.putExtra(EXTRA_ERROR, exception.message)
        })
    }


    override fun cancel(videoItem: VideoItem) {
        executionVideoItems.find { it.videoItem == videoItem }?.let {
            ThreadPool.cancel(it)
            executionVideoItems.remove(it)
        } ?: throw IllegalStateException("Cannot cancel the task because it does not exist")
    }

    override fun isLoading(videoItem: VideoItem): Boolean = executionVideoItems.find { it.videoItem == videoItem } != null

    override fun getProgress(videoItem: VideoItem): Int =
        executionVideoItems.find { it.videoItem == videoItem }?.currentProgress ?: DownloaderInteroperableInterface.NO_PROGRESS

    override fun onDestroy() {
        super.onDestroy()
        Instance.serviceInterface = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    object Instance {
        var serviceInterface: DownloaderInteroperableInterface? = null
    }
}

private object ThreadPool {
    private const val KEEP_ALIVE_TIME = 1L
    private val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS
    private var NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    private val decodeWorkQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>()
    private val decodeThreadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        NUMBER_OF_CORES,
        NUMBER_OF_CORES,
        KEEP_ALIVE_TIME,
        KEEP_ALIVE_TIME_UNIT,
        decodeWorkQueue
    )

    fun execute(task: VideoItemTask) {
        decodeThreadPool.execute(task)
    }

    fun cancel(task: VideoItemTask) {
        decodeThreadPool.remove(task)
        synchronized(this) {
            task.currentThread?.interrupt()
        }
    }

}


private class VideoItemTask(
    val videoItem: VideoItem,
    private val outDir: File,
    private val startId: Int,
    private val executionUpdate: ExecutionUpdate
) : Runnable {
    var currentProgress: Int = 0
    var currentThread: Thread? = null
    val myParser = MyParser()
    private var youTubeDownloader = YoutubeDownloader(myParser)
    override fun run() {
        currentThread = Thread.currentThread()
        var video: YoutubeVideo?
        var audioFormats: List<AudioFormat>?
        do {
            video = youTubeDownloader.getVideo(videoItem.videoId)
            audioFormats = video.audioFormats()
        } while (audioFormats.isNullOrEmpty())

        val audioFormat = audioFormats.last()
        download(video!!, audioFormat)
    }

    private fun download(video: YoutubeVideo, audioFormat: AudioFormat) {
        val videoDetails = video.details()
        if (videoDetails.isLive)
            throw YoutubeException.LiveVideoException("Can not download live stream")

        val outputFile = getOutputFile()
        val url = URL(audioFormat.url())
        try {
            BufferedInputStream(url.openStream()).use { bis ->
                BufferedOutputStream(FileOutputStream(outputFile)).use { bos ->
                    var total = 0.0
                    val buffer = ByteArray(4096)
                    var progress = 0
                    var count: Int
                    while (bis.read(buffer, 0, 4096).also { count = it } != -1) {
                        if (Thread.interrupted()) {
                            bos.close()
                            bis.close()
                            outputFile.delete()
                            return
                        }
                        bos.write(buffer, 0, count)
                        total += count.toDouble()
                        val newProgress = ((total / audioFormat.contentLength()!!) * 100).toInt()
                        if (newProgress > progress) {
                            progress = newProgress
                            executionUpdate.onProgress(videoItem, progress)
                            this.currentProgress = progress
                        }
                    }
                }
                check(outputFile.renameTo(File("${outputFile.parent}/${videoItem.videoId}.mp3"))) { "Cannot rename the file after complete downloading" }
                executionUpdate.onFinished(videoItem, outputFile, startId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            executionUpdate.onError(videoItem, e, startId)
        }

    }

    fun getOutputFile(): File {
        if (!outDir.exists()) {
            val mkdirs = outDir.mkdirs()
            if (!mkdirs)
                throw IOException("Could not create output directory: $outDir")
        }

        var id = 0

        while (true) {
            val fileName = "${videoItem.videoId}" + if (id != 0) ".downloading($id)" else ".downloading"
            val file = File(outDir, fileName)
            if (file.exists()) {
                id++
                continue
            }

            return file
        }
    }

}