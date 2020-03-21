package com.yurii.youtubemusic

import com.yurii.youtubemusic.models.VideoItem

interface ItemsHandler {
    fun isLoading(videoItem: VideoItem): Boolean
    fun exists(videoItem: VideoItem): Boolean
    fun download(videoItem: VideoItem)
}