package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.models.VideoItem
import java.io.File
import java.io.FileNotFoundException

data class MediaMetadata(
    val musicId: String,
    val title: String,
    val description: String,
    val author: String,
    val thumbnail: String,
    val tags: List<String> = emptyList()
)

class MediaMetadataProvider(private val context: Context) {
    fun readMetadata(musicId: String): MediaMetadata {
        val musicDescriptionFile = getMusicMetadataFile(musicId, raiseIfDoestNotExist = true)
        return Gson().fromJson<MediaMetadata>(musicDescriptionFile.reader(), MediaMetadata::class.java)
    }

    fun setMetadata(videoItem: VideoItem) {
        val metadata = MediaMetadata(
            title = videoItem.title,
            musicId = videoItem.videoId,
            author = videoItem.authorChannelTitle,
            description = videoItem.description,
            thumbnail = videoItem.thumbnail
        )

        val metadataJson = getMusicMetadataFile(videoItem.videoId)
        val json = Gson().toJson(metadata)
        metadataJson.writeText(json)
    }


    private fun getMusicMetadataFile(musicId: String, raiseIfDoestNotExist: Boolean = false): File {
        val file = DataStorage.getMetadata(context, musicId).apply {
            if (!parentFile!!.exists())
                parentFile!!.mkdirs()
        }

        if (!file.exists() && raiseIfDoestNotExist)
            throw FileNotFoundException("MetaData file is not found for $musicId music ID")

        return file
    }
}