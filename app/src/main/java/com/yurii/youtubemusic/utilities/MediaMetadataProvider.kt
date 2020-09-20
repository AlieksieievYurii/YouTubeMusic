package com.yurii.youtubemusic.utilities

import android.content.Context
import com.google.gson.Gson
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

class MediaMetadataProvider(context: Context) {
    private val musicMetadataFolder = DataStorage.getMusicMetadataStorage(context).also {
        if (!it.exists())
            it.mkdirs()
    }

    fun readMetadata(musicId: String): MediaMetadata {
        val musicDescriptionFile = getMusicMetadataFile(musicId, raiseIfDoestNotExist = true)
        return Gson().fromJson<MediaMetadata>(musicDescriptionFile.reader(), MediaMetadata::class.java)
    }

    fun setMetadata(musicId: String, metadata: MediaMetadata) {
        val metadataJson = getMusicMetadataFile(musicId)
        val json = Gson().toJson(metadata)
        metadataJson.writeText(json)
    }

    private fun getMusicMetadataFile(musicId: String, raiseIfDoestNotExist: Boolean = false): File {
        val file = File(musicMetadataFolder, "$musicId.json")

        if (!file.exists() && raiseIfDoestNotExist)
            throw FileNotFoundException("MetaData file is not found for $musicId music ID")

        return file
    }
}