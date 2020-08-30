package com.yurii.youtubemusic.services.downloader

import android.util.Log
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.YoutubeException
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.formats.AudioFormat
import com.yurii.youtubemusic.models.VideoItem
import java.io.*
import java.lang.Exception
import java.net.URL

class VideoItemTask(
    val videoItem: VideoItem,
    private val outDir: File,
    private val serviceStartId: Int,
    private val downloadingUpdater: DownloadingUpdater,
    private val youTubeDownloader: YoutubeDownloader
) : Runnable {
    var progress: Progress = Progress.create()
    private var isInterrupted = false

    override fun run() {
        try {
            val video = youTubeDownloader.getVideo(videoItem.videoId)
            checkIfVideoIsLive(video)
            val audioFormat = video.audioFormats().last()
            download(audioFormat)
        } catch (error: Exception) {
            ThreadPool.completeTask(this)
            downloadingUpdater.onError(videoItem, error, serviceStartId)
        }
    }

    fun cancel() {
        check(!isInterrupted) { "The task: ${this} is already interrupted" }
        isInterrupted = true
    }

    private fun download(audioFormat: AudioFormat) {
        val outputFile = getOutputFile()
        val url = URL(audioFormat.url())
        val totalSize = audioFormat.contentLength()!!

        downloadFile(url, totalSize, outputFile)
        ThreadPool.completeTask(this)
        setFileName(outputFile)
        downloadingUpdater.onFinished(videoItem, outputFile, serviceStartId)
    }

    private fun checkIfVideoIsLive(video: YoutubeVideo) {
        val videoDetails = video.details()
        if (videoDetails.isLive)
            throw YoutubeException.LiveVideoException("Can not download live stream")
    }

    private fun setFileName(file: File) {
        val isRenamed = file.renameTo(File("${file.parent}/${videoItem.videoId}.mp3"))

        check(isRenamed) { "Cannot rename the file after complete downloading" }
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
                        return
                    }

                    Log.i("Experiment", "${videoItem.videoId} is downloading")
                    bos.write(buffer, 0, count)
                    total += count.toDouble()

                    val newProgress = ((total / totalSize) * 100).toInt()
                    if (newProgress > progress) {
                        progress = newProgress
                        this.progress.update(progress, total.toInt(), totalSize.toInt())
                        downloadingUpdater.onProgress(videoItem, this.progress)
                    }
                }
            }
        }
    }

    private fun getOutputFile(): File {
        createOutputFolderIfDoesNotExist()
        return getFreeFileName()
    }

    private fun getFreeFileName(): File {
        var id = 0

        while (true) {
            val extension = if (id != 0) "downloading($id)" else "downloading"
            val fileName = "${videoItem.videoId}.$extension"
            val file = File(outDir, fileName)

            if (file.exists()) {
                id++
                continue
            }

            return file
        }
    }

    private fun createOutputFolderIfDoesNotExist() {
        if (outDir.exists())
            return

        val isFolderCreated = outDir.mkdirs()
        if (!isFolderCreated)
            throw IOException("Could not create output directory: $outDir")
    }

    override fun toString(): String {
        return "VideoItemTask(videoItem=${videoItem.videoId}, progress=$progress)"
    }

}