package com.yurii.youtubemusic

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.services.MusicDownloaderService

class MusicItemsHandler(private val context: Context) : ItemsHandler {

    override fun isLoading(videoItem: VideoItem): Boolean {
        MusicDownloaderService.Instance.serviceInterface?.let { return it.isLoading(videoItem) }
        return false
    }

    override fun exists(videoItem: VideoItem): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun download(videoItem: VideoItem) {
        context.startService(Intent(context, MusicDownloaderService::class.java).also {
            it.putExtra(MusicDownloaderService.EXTRA_VIDEO_ITEM, videoItem)
        })
    }

}