package com.yurii.youtubemusic.screens.youtube.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.YoutubeException
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.formats.AudioFormat
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.services.media.MediaStorage
import com.yurii.youtubemusic.utilities.parentMkdir
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class MusicDownloaderImp(private val callBack: CallBack, private val mediaStorage: MediaStorage) :
    MusicDownloaderAbstract() {
    private val youtubeDownloader = YoutubeDownloader()

    private val keepAliveTime = 1L
    private val keepAliveTimeUnit = TimeUnit.SECONDS
    private var numberOfCores = Runtime.getRuntime().availableProcessors()
    private val decodeWorkQueue: BlockingQueue<Runnable> = LinkedBlockingQueue()
    private val executionTasks: MutableList<Task> = mutableListOf()
    private val failedTasks: MutableList<Task> = mutableListOf()
    private val decodeThreadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        numberOfCores,
        numberOfCores,
        keepAliveTime,
        keepAliveTimeUnit,
        decodeWorkQueue
    )

    override fun download(videoItem: VideoItem, customCategories: List<Category>) {
        val task = Task(videoItem, customCategories)
        executeTask(task)
    }

    override fun retryToDownload(videoItem: VideoItem) {
        findFailedTask(videoItem)?.run {
            executeTask(this)
        }
    }

    private fun executeTask(task: Task) {
        executionTasks.add(task)
        decodeThreadPool.execute(task)
    }

    override fun cancel(videoItem: VideoItem) {
        findFailedTask(videoItem)?.run { failedTasks.remove(this) }

        findExecutingTask(videoItem)?.run {
            interruptTask()
            decodeThreadPool.remove(this)
            executionTasks.remove(this)
        }
    }

    override fun isItemDownloading(videoItem: VideoItem) = findExecutingTask(videoItem) != null

    override fun isDownloadingFailed(videoItem: VideoItem) = findFailedTask(videoItem) != null

    override fun getError(videoItem: VideoItem): Exception? = findFailedTask(videoItem)?.lastError

    override fun getCompletedProgress(): Int {
        var sum = 0
        executionTasks.forEach { task -> sum += task.progress.progress }
        return sum / executionTasks.size
    }

    override fun isQueueEmpty() = executionTasks.isEmpty()

    override fun getProgress(videoItem: VideoItem): Progress? = findExecutingTask(videoItem)?.progress

    private fun findExecutingTask(videoItem: VideoItem): Task? = executionTasks.find { it.videoItem.id == videoItem.id }

    private fun findFailedTask(videoItem: VideoItem): Task? = failedTasks.find { it.videoItem.id == videoItem.id }

    private inner class Task(val videoItem: VideoItem, private val customCategories: List<Category>) : Runnable {
        var progress: Progress = Progress.create()
        private var isInterrupted = false

        var lastError: Exception? = null

        override fun run() {
            try {
                downloadSource()
            } catch (error: Exception) {
                lastError = error
                failedTasks.add(this)
                executionTasks.remove(this)
                callBack.onErrorOccurred(videoItem, error)
            }
        }

        private fun downloadSource() {
            try {
                downloadMusic()
                downloadThumbnail()
            } catch (_: InterruptedException) {
                executionTasks.remove(this)
                return
            }
            executionTasks.remove(this)
            callBack.onFinished(videoItem, customCategories)
        }

        fun interruptTask() {
            check(!isInterrupted) { "Task: $this is already interrupted" }
            isInterrupted = true
        }

        private fun downloadMusic() {
            val video = tryToParseVideo()
            checkIfVideoIsLive(video)
            val audioFormat = video.audioFormats().last()
            download(audioFormat)
        }

        @Throws(YoutubeException::class)
        private fun tryToParseVideo(): YoutubeVideo {
            var attempt = 1
            while (true)
                try {
                    return youtubeDownloader.getVideo(videoItem.id)
                } catch (error: YoutubeException) {
                    if (attempt == 3)
                        throw error
                    else
                        attempt++
                }
        }

        private fun downloadThumbnail() {
            val bitmap = downloadBitmap(videoItem.normalThumbnail)
            mediaStorage.saveThumbnail(bitmap, videoItem)
        }

        private fun downloadBitmap(srcUrl: String): Bitmap {
            val httpConnection = URL(srcUrl).openConnection().apply {
                doInput = true
                connect()
            }
            val inputStream = httpConnection.getInputStream()
            return BitmapFactory.decodeStream(inputStream)
        }

        private fun download(audioFormat: AudioFormat) {
            val outputFile = mediaStorage.getDownloadingMockFile(videoItem).also { it.parentMkdir() }
            val url = URL(audioFormat.url())
            val totalSize = audioFormat.contentLength()!!

            downloadFile(url, totalSize, outputFile)

            if (isInterrupted)
                return

            mediaStorage.setMockAsDownloaded(videoItem)
        }

        private fun checkIfVideoIsLive(video: YoutubeVideo) {
            val videoDetails = video.details()
            if (videoDetails.isLive)
                throw YoutubeException.LiveVideoException("Can not download live stream")
        }

        private fun downloadFile(url: URL, totalSize: Long, outputFile: File) {
            var total = 0.0
            val buffer = ByteArray(4096)
            var progress = 0
            var count: Int

            BufferedInputStream(url.openStream()).use { bis ->
                BufferedOutputStream(FileOutputStream(outputFile)).use { bos ->
                    while (bis.read(buffer, 0, 4096).also { count = it } != -1) {
                        if (isInterrupted) {
                            bos.close()
                            bis.close()
                            outputFile.delete()
                            throw InterruptedException()
                        }

                        bos.write(buffer, 0, count)
                        total += count.toDouble()

                        val newProgress = ((total / totalSize) * 100).toInt()
                        if (newProgress > progress) {
                            progress = newProgress
                            this.progress.update(progress, total.toLong(), totalSize)
                            callBack.onChangeProgress(videoItem, this.progress)
                        }
                    }
                }
            }
        }


        override fun toString(): String = "VideoItemTask(videoItem=${videoItem.id}, progress=$progress)"
    }
}