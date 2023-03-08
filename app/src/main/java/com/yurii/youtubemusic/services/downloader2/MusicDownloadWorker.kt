package com.yurii.youtubemusic.services.downloader2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.YoutubeException
import com.github.kiulian.downloader.model.YoutubeVideo
import com.yurii.youtubemusic.services.media.MediaStorage
import com.yurii.youtubemusic.utilities.parentMkdir
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@HiltWorker
class MusicDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val youtubeDownloader: YoutubeDownloader,
    private val mediaStorage: MediaStorage
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val videoId = inputData.getString(ARG_VIDEO_ID) ?: throw IllegalStateException("Worker requires <string> ARG_VIDEO_ID argument")
        val thumbnailUrl = inputData.getString(ARG_VIDEO_THUMBNAIL_URL)
            ?: throw IllegalStateException("Worker requires <string> ARG_VIDEO_THUMBNAIL_URL argument")
       Timber.i("Start downloading music from YouTube video ID: $videoId")

        return try {
            val mediaFileSize = download(videoId, thumbnailUrl)
            Result.success(workDataOf(MEDIA_SIZE to mediaFileSize))
        } catch (error: Exception) {
            Timber.e(error)
            mediaStorage.getDownloadingMockFile(videoId).delete()
            Result.failure(workDataOf(ERROR_MESSAGE to error.message))
        }
    }

    private suspend fun download(videoId: String, thumbnailUrl: String): Long {
        val video = tryToParseVideo(videoId)
        checkIfVideoIsLive(video)

        val mediaFileSize = download(video)
        downloadAndSaveThumbnail(thumbnailUrl, videoId)

        return mediaFileSize
    }

    private suspend fun downloadAndSaveThumbnail(thumbnailUrl: String, videoId: String) {
        val bitmap = downloadBitmap(thumbnailUrl)
        mediaStorage.saveThumbnail(bitmap, videoId)
    }

    private suspend fun download(video: YoutubeVideo): Long {
        val audioFormat = video.audioFormats().last()
        val outputFile = mediaStorage.getDownloadingMockFile(video.details().videoId()).also { it.parentMkdir() }
        val url = URL(audioFormat.url())
        val totalSize = audioFormat.contentLength()!!

        downloadFile(url, totalSize, outputFile)

        mediaStorage.setMockAsDownloaded(video.details().videoId())

        return totalSize
    }

    @Throws(YoutubeException::class)
    private fun tryToParseVideo(videoId: String): YoutubeVideo {
        var attempt = 1
        while (true)
            try {
                return youtubeDownloader.getVideo(videoId)
            } catch (error: YoutubeException) {
                if (attempt == 3)
                    throw error
                else
                    attempt++
            }
    }

    private fun checkIfVideoIsLive(video: YoutubeVideo) {
        val videoDetails = video.details()
        if (videoDetails.isLive)
            throw YoutubeException.LiveVideoException("Can not download live stream")
    }

    private suspend fun downloadFile(url: URL, totalSize: Long, outputFile: File) = withContext(Dispatchers.IO) {
        var total = 0.0
        val buffer = ByteArray(4096)
        var progress = 0
        var count: Int

        BufferedInputStream(url.openStream()).use { bis ->
            BufferedOutputStream(FileOutputStream(outputFile)).use { bos ->
                while (bis.read(buffer, 0, 4096).also { count = it } != -1) {
                    if (isStopped)
                        throw CancellationException()

                    bos.write(buffer, 0, count)
                    total += count.toDouble()

                    val newProgress = ((total / totalSize) * 100).toInt()
                    if (newProgress > progress) {
                        progress = newProgress

                        setProgressAsync(
                            workDataOf(
                                PROGRESS to progress,
                                PROGRESS_DOWNLOADED_SIZE to total.toLong(),
                                PROGRESS_TOTAL_SIZE to totalSize
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun downloadBitmap(srcUrl: String): Bitmap = withContext(Dispatchers.IO) {
        val httpConnection = URL(srcUrl).openConnection().apply {
            doInput = true
            connect()
        }
        val inputStream = httpConnection.getInputStream()
        BitmapFactory.decodeStream(inputStream)
    }

    companion object {
        const val ARG_VIDEO_ID = "ARG_VIDEO_ID"
        const val ARG_VIDEO_THUMBNAIL_URL = "ARG_VIDEO_THUMBNAIL_URL"
        const val PROGRESS = "progress"
        const val PROGRESS_TOTAL_SIZE = "total_size"
        const val PROGRESS_DOWNLOADED_SIZE = "downloaded_size"
        const val MEDIA_SIZE = "media_size"
        const val ERROR_MESSAGE = "error_message"
    }
}