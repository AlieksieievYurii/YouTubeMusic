package com.yurii.youtubemusic.utilities

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.yurii.youtubemusic.models.Category
import com.yurii.youtubemusic.models.Item
import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import java.io.File
import java.lang.IllegalStateException

/**
 * Represents an logical interface to the file system in order to manage media files from single repository
 */
class MediaStorage(context: Context) {

    private val gson = Gson()

    private val musicStorageFolder = File(context.filesDir, "Musics")

    private val thumbnailStorage = File(context.filesDir, "Thumbnails")

    private val categoriesStorage = File(context.filesDir, "Categories")

    private val musicMetadataStorage = File(context.filesDir, "Metadata")

    fun getMediaFile(item: Item): File = File(musicStorageFolder, "${item.id}.mp3")

    fun getThumbnail(item: Item): File = File(thumbnailStorage, "${item.id}.jpeg")

    fun getCategoryFile(category: Category): File = File(categoriesStorage, "${category.id}.json")

    fun getMediaMetadata(item: Item): File = File(musicMetadataStorage, "${item.id}.json")

    fun getAllMusicFiles(): List<File> = musicStorageFolder.walkFiles().filter { it.extension == "mp3" }.toList()

    fun getDownloadingMockFile(videoItem: VideoItem): File = File(musicStorageFolder, "${videoItem.id}.downloading")

    fun deleteDownloadingMocks() = musicStorageFolder.walkFiles().filter { it.extension == "downloading" }.forEach { it.delete() }

    fun createMediaMetadata(mediaItem: MediaItem) {
        val metadataJson = getMediaMetadata(mediaItem).also { it.parentMkdir() }
        val json = gson.toJson(mediaItem)
        metadataJson.writeText(json)
    }

    fun getDefaultCategory(): Category {
        val file = getCategoryFile(Category.ALL)
        return if (file.exists())
            gson.fromJson(file.readText(), Category::class.java)
        else {
            file.parentMkdir()
            file.writeText(text = gson.toJson(Category.ALL))
            Category.ALL
        }
    }

    fun getCategories(): List<Category> = categoriesStorage.walkFiles().map { gson.fromJson(it.readText(), Category::class.java) }.toList()

    fun saveCategory(category: Category) {
        val json = gson.toJson(category)
        val categoryFile = getCategoryFile(category).also { it.parentMkdir() }
        categoryFile.writeText(json)
    }

    fun setMockAsDownloaded(videoItem: VideoItem) {
        val downloadingMockFile = getDownloadingMockFile(videoItem)

        if (!downloadingMockFile.exists())
            throw IllegalStateException("Can not find Downloading Mock file for $videoItem")


        val newFile = getMediaFile(videoItem)
        val isRenamed = downloadingMockFile.renameTo(newFile)

        check(isRenamed) { "Failed to rename the file from: $downloadingMockFile to $newFile" }
    }

    fun saveThumbnail(bitmap: Bitmap, videoItem: VideoItem) {
        val thumbnailFile = getThumbnail(videoItem)
        thumbnailFile.parentMkdir()
        thumbnailFile.outputStream().run { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this) }
    }

    fun deleteAllDataFor(item: Item) {
        getMediaFile(item).delete()
        getMediaMetadata(item).delete()
        getThumbnail(item).delete()
    }


}