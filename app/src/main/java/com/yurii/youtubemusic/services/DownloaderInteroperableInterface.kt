package com.yurii.youtubemusic.services

import com.yurii.youtubemusic.models.VideoItem

interface DownloaderInteroperableInterface {
    fun isLoading(videoItem: VideoItem): Boolean
}