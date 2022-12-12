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

class MediaItemValidationException(message: String) : Exception(message)

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

    fun getThumbnail(item: Item): File = getThumbnail(item.id)
    fun getThumbnail(id: String): File = File(thumbnailStorage, "$id.jpeg")

    fun getAllMusicFiles(): List<File> = musicStorageFolder.walkFiles().filter { it.extension == "mp3" }.toList()

    fun getDownloadingMockFile(videoItem: VideoItem): File = File(musicStorageFolder, "${videoItem.id}.downloading")

    fun deleteDownloadingMocks() = musicStorageFolder.walkFiles().filter { it.extension == "downloading" }.forEach { it.delete() }

    fun createMediaMetadata(mediaItem: MediaItem) {
        val metadataJson = getMediaMetadata(mediaItem).also { it.parentMkdir() }
        val json = gson.toJson(mediaItem)
        metadataJson.writeText(json)
    }

    private suspend fun getAllCategoryContainers(): List<CategoryContainer> =
        getCustomCategoryContainers().toMutableList().apply { add(0, getDefaultCategoryContainer()) }

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

    private suspend fun getMediaItem(id: String): MediaItem = withContext(Dispatchers.IO) {
        gson.fromJson(getMediaMetadata(id).readText(), MediaItem::class.java)!!
    }

    suspend fun validate(mediaItem: MediaItem) {
        if (!mediaItem.mediaFile.exists()) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("Media file does not exist for ${mediaItem.id}. So eliminate that")
        }

        if (!mediaItem.thumbnail.exists()) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("Thumbnail file does not exist for ${mediaItem.id}. So eliminate that")
        }

        if (getDefaultCategoryContainer().mediaItemsIds.find { it == mediaItem.id } == null) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("MediaItem (${mediaItem.id}) is not listed in the Default category, so eliminate that")
        }
    }

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

    private suspend fun eliminateMediaItem(id: String) {
        getMediaFile(id).delete()
        getMediaMetadata(id).delete()
        getThumbnail(id).delete()
        getAllCategoryContainers().forEach {
            if (it.mediaItemsIds.contains(id)) {
                val newMediaItemsIdsList = it.mediaItemsIds.toMutableList()
                newMediaItemsIdsList.remove(id)
                val newContainerCategory = CategoryContainer(it.category, newMediaItemsIdsList)
                saveCategoryContainer(newContainerCategory)
            }
        }
    }

    private suspend fun getDefaultCategory(): Category = getDefaultCategoryContainer().category

    private suspend fun getCustomCategoryContainers(): List<CategoryContainer> = withContext(Dispatchers.IO) {
        categoriesContainersStorage.walkFiles().map {
            gson.fromJson(it.readText(), CategoryContainer::class.java)
        }.toList()
    }

    private suspend fun getCategoryContainer(category: Category): CategoryContainer = getCategoryContainer(category.id)

    suspend fun getCategoryContainer(categoryId: Int): CategoryContainer = withContext(Dispatchers.IO) {
        gson.fromJson(getCategoryContainerFile(categoryId).readText(), CategoryContainer::class.java)
    }

    /**
     * Returns an instance of [MediaItem] for given [id]. Moreover, it checks if the media item is validated:
     *  - if can get media item itself
     *  - if a music file exists
     *  - if a thumbnail exists
     *  - if the media item is listed in Default category
     *
     * When some of this criteria is failed, then eliminate the whole media item and throws an exception [MediaItemValidationException]
     */
    suspend fun getValidatedMediaItem(id: String): MediaItem {
        val mediaItem = try {
            getMediaItem(id)
        } catch (error: Exception) {
            eliminateMediaItem(id)
            throw MediaItemValidationException("Can not get Media metadata for $id, so eliminating it")
        }

        if (!mediaItem.mediaFile.exists()) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("Media file does not exist for ${mediaItem.id}. So eliminate that")
        }

        if (!mediaItem.thumbnail.exists()) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("Thumbnail file does not exist for ${mediaItem.id}. So eliminate that")
        }

        if (getDefaultCategoryContainer().mediaItemsIds.find { it == mediaItem.id } == null) {
            eliminateMediaItem(mediaItem.id)
            throw MediaItemValidationException("MediaItem (${mediaItem.id}) is not listed in the Default category, so eliminate that")
        }

        return mediaItem
    }

    private suspend fun saveCategoryContainer(categoryContainer: CategoryContainer) = withContext(Dispatchers.IO) {
        val json = gson.toJson(categoryContainer)
        val categoryFile = getCategoryContainerFile(categoryContainer.category).also { it.parentMkdir() }
        categoryFile.writeText(json)
    }
}