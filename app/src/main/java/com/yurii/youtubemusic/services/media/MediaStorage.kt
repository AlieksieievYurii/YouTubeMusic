package com.yurii.youtubemusic.services.media

import android.content.Context
import android.graphics.Bitmap
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.models.VideoItem
import com.yurii.youtubemusic.utilities.parentMkdir
import com.yurii.youtubemusic.utilities.walkFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorage @Inject constructor(@ApplicationContext context: Context) {
    private val musicStorageFolder = File(context.filesDir, "Musics")
    private val thumbnailStorage = File(context.filesDir, "Thumbnails")

    fun getMediaFile(item: Item): File = File(musicStorageFolder, "${item.id}.mp3")
    fun getMediaFile(id: String): File = File(musicStorageFolder, "$id.mp3")

    fun getThumbnail(item: Item): File = File(thumbnailStorage, "${item.id}.jpeg")
    fun getThumbnail(id: String): File = File(thumbnailStorage, "$id.jpeg")

    fun getDownloadingMockFile(videoItem: VideoItem): File = File(musicStorageFolder, "${videoItem.id}.downloading")
    fun getDownloadingMockFile(videoId: String): File = File(musicStorageFolder, "$videoId.downloading")

    fun deleteDownloadingMocks() = musicStorageFolder.walkFiles().filter { it.extension == "downloading" }.forEach { it.delete() }

    fun deleteMediaFiles(item: Item) {
        getMediaFile(item).delete()
        getThumbnail(item).delete()
    }

    fun setMockAsDownloaded(videoItem: VideoItem) {
        val downloadingMockFile = getDownloadingMockFile(videoItem)

        if (!downloadingMockFile.exists())
            throw IllegalStateException("Can not find Downloading Mock file for $videoItem")


        val newFile = getMediaFile(videoItem)
        val isRenamed = downloadingMockFile.renameTo(newFile)

        check(isRenamed) { "Failed to rename the file from: $downloadingMockFile to $newFile" }
    }

    fun setMockAsDownloaded(videoId: String) {
        val downloadingMockFile = getDownloadingMockFile(videoId)

        if (!downloadingMockFile.exists())
            throw IllegalStateException("Can not find Downloading Mock file for $videoId")


        val newFile = getMediaFile(videoId)
        val isRenamed = downloadingMockFile.renameTo(newFile)

        check(isRenamed) { "Failed to rename the file from: $downloadingMockFile to $newFile" }
    }

    fun saveThumbnail(bitmap: Bitmap, videoItem: VideoItem) {
        val thumbnailFile = getThumbnail(videoItem)
        thumbnailFile.parentMkdir()
        thumbnailFile.outputStream().run { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this) }
    }

    fun saveThumbnail(bitmap: Bitmap, videoItemId: String) {
        val thumbnailFile = getThumbnail(videoItemId)
        thumbnailFile.parentMkdir()
        thumbnailFile.outputStream().run { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this) }
    }
}