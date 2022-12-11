package com.yurii.youtubemusic.screens.youtube.service

import androidx.annotation.IntRange
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.screens.youtube.models.Progress
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import java.lang.Exception

abstract class MusicDownloaderAbstract {
    interface CallBack {
        fun onFinished(videoItem: VideoItem, customCategories: List<Category>)
        fun onChangeProgress(videoItem: VideoItem, progress: Progress)
        fun onErrorOccurred(videoItem: VideoItem, error: Exception)
    }

    abstract fun download(videoItem: VideoItem, customCategories: List<Category> = listOf())
    abstract fun retryToDownload(videoItem: VideoItem)
    abstract fun cancel(videoItem: VideoItem)
    abstract fun isItemDownloading(videoItem: VideoItem): Boolean
    abstract fun isDownloadingFailed(videoItem: VideoItem): Boolean
    abstract fun getError(videoItem: VideoItem): Exception?

    @IntRange(from = 0, to = 100)
    abstract fun getCompletedProgress(): Int
    abstract fun isQueueEmpty(): Boolean

    abstract fun getProgress(videoItem: VideoItem): Progress?
}