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

    private lateinit var metaDataItems: List<MediaMetaData>
    private val categories: List<Category> by lazy {
        ArrayList<Category>().apply {
            add(Category.ALL)
            addAll(Preferences.getMusicCategories(context))
        }
    }

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

    private fun filterMediaItemsByCategory(category: Category): List<MediaMetaData> {
        return metaDataItems.filter { item -> category in item.categories }
    }

    fun retrieveMusics(callback: CallBack) {
        if (isMusicsInitialized)
            throw IllegalStateException("Music provider is already initialized")

        MusicsLoader(context).apply {
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

private class MusicsLoader(private val context: Context) : AsyncTask<Void, Void, List<MediaMetaData>>() {
    private val mediaMetadataCompat = MediaMetadataProvider(context)

    var onLoadSuccessfully: ((musicItems: List<MediaMetaData>) -> Unit)? = null
    var onFailedToLoad: ((error: Exception) -> Unit)? = null

    private var currentError: Exception? = null

    override fun doInBackground(vararg params: Void?): List<MediaMetaData>? {
        return try {
            getMetaDataItems()
        } catch (error: Exception) {
            currentError = error
            null
        }
    }

    private fun getMetaDataItems(): List<MediaMetaData> {
        return ArrayList<MediaMetaData>().apply {
            retrieveMusics().forEach { file ->
                this.add(mediaMetadataCompat.readMetadata(file.nameWithoutExtension))
            }
        }
    }

    private fun retrieveMusics(): List<File> = DataStorage.getAllMusicFiles(context)

    override fun onPostExecute(result: List<MediaMetaData>?) {
        super.onPostExecute(result)
        if (result != null)
            onLoadSuccessfully?.invoke(result)
        else if (currentError != null)
            onFailedToLoad?.invoke(currentError!!)
    }
}
