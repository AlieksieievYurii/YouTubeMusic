package com.yurii.youtubemusic.services.media

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.screens.youtube.models.VideoItem
import com.yurii.youtubemusic.utilities.parentMkdir
import com.yurii.youtubemusic.utilities.walkFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.IllegalStateException

/**
 * Represents an logical interface to the file system in order to manage media files from single repository
 */
class MediaStorage(context: Context) {
    private val gson = Gson()
    private val musicStorageFolder = File(context.filesDir, "Musics")
    private val thumbnailStorage = File(context.filesDir, "Thumbnails")
    private val categoriesContainersStorage = File(context.filesDir, "Categories")
    private val musicMetadataStorage = File(context.filesDir, "Metadata")

    fun getMediaFile(item: Item): File = getMediaFile(item.id)

    fun getThumbnail(item: Item): File = File(thumbnailStorage, "${item.id}.jpeg")

    fun getAllMusicFiles(): List<File> = musicStorageFolder.walkFiles().filter { it.extension == "mp3" }.toList()

    fun getDownloadingMockFile(videoItem: VideoItem): File = File(musicStorageFolder, "${videoItem.id}.downloading")

    fun deleteDownloadingMocks() = musicStorageFolder.walkFiles().filter { it.extension == "downloading" }.forEach { it.delete() }

    fun createMediaMetadata(mediaItem: MediaItem) {
        val metadataJson = getMediaMetadata(mediaItem).also { it.parentMkdir() }
        val json = gson.toJson(mediaItem)
        metadataJson.writeText(json)
    }

    suspend fun getMediaItemsFor(category: Category) = getMediaItemsFor(category.id)

    suspend fun getMediaItemsFor(categoryId: Int): List<MediaItem> = getCategoryContainer(categoryId).mediaItemsIds.map { getMediaItem(it) }

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

    suspend fun assignItemToCategory(category: Category, item: Item) {
        val categoryContainer = getCategoryContainer(category)
        val mediaItems = categoryContainer.mediaItemsIds.toMutableList()
        mediaItems.add(item.id)
        val newCategoryContainer = CategoryContainer(category, mediaItems)
        saveCategoryContainer(newCategoryContainer)
    }

    suspend fun getCustomCategories(): List<Category> = getCustomCategoryContainers().map { it.category }.filter { !it.isDefault }

    suspend fun getAllCategories(): List<Category> = getCustomCategories().toMutableList().apply { add(0, getDefaultCategory()) }

    private fun getMediaFile(id: String): File = File(musicStorageFolder, "${id}.mp3")

    private fun getMediaItem(id: String): MediaItem = gson.fromJson(getMediaMetadata(id).readText(), MediaItem::class.java)!!

    private fun getCategoryContainerFile(category: Category): File = getCategoryContainerFile(category.id)

    private fun getCategoryContainerFile(categoryId: Int): File = File(categoriesContainersStorage, "$categoryId.json")

    private fun getCategoryFile(category: Category): File = File(categoriesContainersStorage, "${category.id}.json")

    private fun getMediaMetadata(item: Item): File = getMediaMetadata(item.id)

    private fun getMediaMetadata(id: String): File = File(musicMetadataStorage, "$id.json")

    private suspend fun getDefaultCategoryContainer(): CategoryContainer = withContext(Dispatchers.IO) {
        val file = getCategoryFile(Category.ALL)
        if (file.exists())
            gson.fromJson(file.readText(), CategoryContainer::class.java)
        else CategoryContainer(Category.ALL, mutableListOf()).also {
            file.parentMkdir()
            file.writeText(text = gson.toJson(it))
        }
    }

    private suspend fun getDefaultCategory(): Category = getDefaultCategoryContainer().category

    private suspend fun getCustomCategoryContainers(): List<CategoryContainer> = withContext(Dispatchers.IO) {
        categoriesContainersStorage.walkFiles().map {
            gson.fromJson(it.readText(), CategoryContainer::class.java)
        }.toList()
    }

    private suspend fun getCategoryContainer(category: Category): CategoryContainer = getCategoryContainer(category.id)

    private suspend fun getCategoryContainer(categoryId: Int): CategoryContainer = withContext(Dispatchers.IO) {
        gson.fromJson(getCategoryContainerFile(categoryId).readText(), CategoryContainer::class.java)
    }

    private fun saveCategoryContainer(categoryContainer: CategoryContainer) {
        val json = gson.toJson(categoryContainer)
        val categoryFile = getCategoryContainerFile(categoryContainer.category).also { it.parentMkdir() }
        categoryFile.writeText(json)
    }
}