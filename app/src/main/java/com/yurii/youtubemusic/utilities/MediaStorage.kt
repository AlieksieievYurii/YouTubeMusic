package com.yurii.youtubemusic.utilities

import android.content.Context
import java.io.File

class MediaStorage(private val context: Context) {

    val musicStorageFolder = File(context.filesDir, "Musics")

    private fun getThumbnailsStorage(): File = File(context.filesDir, "Thumbnails")

    private fun getMusicMetadataStorage(): File = File(context.filesDir, "Metadata")

    fun getMusic(musicId: String): File = File(musicStorageFolder, "$musicId.mp3")

    fun getThumbnail(musicId: String): File = File(getThumbnailsStorage(), "$musicId.jpeg")

    fun getMetadata(musicId: String): File = File(getMusicMetadataStorage(), "$musicId.json")

    fun getAllMusicFiles(): List<File> = musicStorageFolder.walk().filter { it.extension == "mp3" }.toList()

    fun deleteAllDataFor(musicId: String) {
        getMusic(musicId).delete()
        getMetadata(musicId).delete()
        getThumbnail(musicId).delete()
    }
}