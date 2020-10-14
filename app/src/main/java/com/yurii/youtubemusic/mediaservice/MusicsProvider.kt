package com.yurii.youtubemusic.mediaservice

import android.content.Context
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import com.yurii.youtubemusic.models.*
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.MediaMetadataProvider
import com.yurii.youtubemusic.utilities.Preferences
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException

class MusicProviderException(message: String) : Exception(message)

class MusicsProvider(private val context: Context) {
    interface CallBack {
        fun onLoadSuccessfully()
        fun onFailedToLoad(error: Exception)
    }

    private val mediaMetadataProvider = MediaMetadataProvider(context)
    private lateinit var metaDataItems: ArrayList<MediaMetaData>
    private val categories: ArrayList<Category> by lazy { retrieveCategories() }

    var isMusicsInitialized: Boolean = false
        private set

    fun getMediaCompactItemsByCategoryId(categoryId: Int): List<MediaBrowserCompat.MediaItem> {
        val category = categories.find { it.id == categoryId } ?: throw MusicProviderException("Cannot find category with id: $categoryId")
        return getMediaItemsByCategory(category).map { it.toCompatMediaItem() }
    }

    fun getMediaItemsByCategory(category: Category): List<MediaMetaData> {
        if (category == Category.ALL)
            return metaDataItems

        return filterMediaItemsByCategory(category)
    }

    fun updateMediaItems() {
        updateCategories()
        updateMetadata()
    }

    fun deleteMediaItem(mediaId: String) {
        metaDataItems.find { it.mediaId == mediaId }?.run {
            metaDataItems.remove(this)
        }
    }

    private fun updateMetadata() {
        metaDataItems.forEach {
            if (hasNonExistentCategories(it))
                deleteNonExistentCategories(it)
        }
    }

    private fun deleteNonExistentCategories(mediaMetaData: MediaMetaData) {
        mediaMetaData.categories.removeAll { it !in categories }
        mediaMetadataProvider.updateMetaData(mediaMetaData)
    }

    private fun hasNonExistentCategories(mediaMetaData: MediaMetaData): Boolean {
        mediaMetaData.categories.forEach {
            if (it !in categories)
                return true
        }
        return false
    }

    private fun updateCategories() {
        categories.clear()
        categories.addAll(retrieveCategories())
    }

    private fun retrieveCategories(): ArrayList<Category> =
        ArrayList<Category>().apply {
            add(Category.ALL)
            addAll(Preferences.getMusicCategories(context))
        }

    private fun filterMediaItemsByCategory(category: Category): List<MediaMetaData> {
        return metaDataItems.filter { item -> category in item.categories }
    }

    fun retrieveMusics(callback: CallBack) {
        if (isMusicsInitialized)
            throw IllegalStateException("Music provider is already initialized")

        MusicsLoader(context, mediaMetadataProvider).apply {
            onLoadSuccessfully = { mediaItems ->
                metaDataItems = mediaItems
                isMusicsInitialized = true
                callback.onLoadSuccessfully()
            }
            onFailedToLoad = { callback.onFailedToLoad(it) }
        }.execute()
    }

    fun getMediaItemsCategories(): List<MediaBrowserCompat.MediaItem> = categories.map { it.toMediaItem() }

    fun isEmptyMusicsList() = metaDataItems.isEmpty()
}

private class MusicsLoader(private val context: Context, private val mediaMetadataProvider: MediaMetadataProvider) :
    AsyncTask<Void, Void, ArrayList<MediaMetaData>>() {
    var onLoadSuccessfully: ((musicItems: ArrayList<MediaMetaData>) -> Unit)? = null
    var onFailedToLoad: ((error: Exception) -> Unit)? = null

    private var currentError: Exception? = null

    override fun doInBackground(vararg params: Void?): ArrayList<MediaMetaData>? {
        return try {
            getMetaDataItems()
        } catch (error: Exception) {
            currentError = error
            null
        }
    }

    private fun getMetaDataItems(): ArrayList<MediaMetaData> {
        return ArrayList<MediaMetaData>().apply {
            retrieveMusics().forEach { file ->
                this.add(mediaMetadataProvider.readMetadata(file.nameWithoutExtension))
            }
        }
    }

    private fun retrieveMusics(): List<File> = DataStorage.getAllMusicFiles(context)

    override fun onPostExecute(result: ArrayList<MediaMetaData>?) {
        super.onPostExecute(result)
        if (result != null)
            onLoadSuccessfully?.invoke(result)
        else if (currentError != null)
            onFailedToLoad?.invoke(currentError!!)
    }
}
