package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.gson.Gson
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.MediaMetaData
import com.yurii.youtubemusic.models.VideoItem
import java.io.File
import java.io.FileNotFoundException
import org.threeten.bp.Duration

class MediaMetadataProvider(private val context: Context) {
    fun readMetadata(musicId: String): MediaMetaData {
        val musicDescriptionFile = getMusicMetadataFile(musicId, raiseIfDoestNotExist = true)
        return Gson().fromJson(musicDescriptionFile.reader(), MediaMetaData::class.java)
    }

    fun setMetadata(videoItem: VideoItem, categories: List<Category>) {
        val metadata = MediaMetaData(
            title = videoItem.title,
            mediaId = videoItem.videoId,
            author = videoItem.authorChannelTitle,
            description = videoItem.description,
            thumbnail = DataStorage.getThumbnail(context, videoItem.videoId),
            duration = Duration.parse(videoItem.duration).toMillis(),
            mediaFile = DataStorage.getMusic(context, videoItem.videoId),
            categories = ArrayList(categories) //TODO It's necessary to investigate whether is better way to do it
        )

        writeToFile(metadata)
    }

    fun updateMetaData(metaData: MediaMetaData) = writeToFile(metaData)

    private fun writeToFile(metadata: MediaMetaData) {
        val metadataJson = getMusicMetadataFile(metadata.mediaId)
        val json = Gson().toJson(metadata)
        metadataJson.writeText(json)
    }


    @Throws(FileNotFoundException::class)
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