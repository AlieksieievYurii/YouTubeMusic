package com.yurii.youtubemusic.services.downloader

import android.content.Context
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.VideoItem
import java.lang.Exception

abstract class MusicDownloaderAbstract {
    interface CallBack {
        fun onFinished(videoItem: VideoItem)
        fun onChangeProgress(videoItem: VideoItem, progress: Progress)
        fun onErrorOccurred(videoItem: VideoItem, error: Exception)
    }

    abstract fun download(videoItem: VideoItem, categories: List<Category> = listOf())
    abstract fun retryToDownload(videoItem: VideoItem)
    abstract fun cancel(videoItem: VideoItem)
    abstract fun isItemDownloading(videoItem: VideoItem): Boolean
    abstract fun isDownloadingFailed(videoItem: VideoItem): Boolean
    abstract fun getError(videoItem: VideoItem): Exception?
    abstract fun getProgress(videoItem: VideoItem): Progress?
}