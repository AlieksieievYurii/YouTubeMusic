package com.yurii.youtubemusic.mediaservice

import android.content.Context
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.mediaservice.MusicsProvider.Companion.METADATA_TRACK_CATEGORY
import com.yurii.youtubemusic.mediaservice.MusicsProvider.Companion.METADATA_TRACK_SOURCE
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.PreferencesV2
import com.yurii.youtubemusic.utilities.TaggerV1
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException

class MusicsProvider(private val context: Context) {
    interface CallBack {
        fun onLoadSuccessfully()
        fun onFailedToLoad(error: Exception)
    }

    private lateinit var metaDataItems: MutableList<MediaMetadataCompat>

    var isMusicsInitialized: Boolean = false
        private set

    fun getMetaData(queueItem: MediaSessionCompat.QueueItem): MediaMetadataCompat {
        return metaDataItems.find { it.description.mediaId == queueItem.description.mediaId } ?: throw IllegalStateException("Cannot find metadata")
    }

    fun getMusicsByCategory(category: String): MutableList<MediaBrowserCompat.MediaItem> {
        if (category == "all")
            return convertMusicMetaDataToMediaItem(metaDataItems)

        return convertMusicMetaDataToMediaItem(metaDataItems.filter { it.getString(METADATA_TRACK_CATEGORY) == category })
    }

    private fun convertMusicMetaDataToMediaItem(metaDataFiles: List<MediaMetadataCompat>): MutableList<MediaBrowserCompat.MediaItem> {
        return metaDataFiles.map { MediaBrowserCompat.MediaItem(it.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE) }.toMutableList()
    }


    fun retrieveMusics(callback: CallBack) {
        if (isMusicsInitialized)
            throw IllegalStateException("Music provider is already initialized")

        MusicsLoader(context).apply {
            onLoadSuccessfully = {
                metaDataItems = it
                isMusicsInitialized = true
                callback.onLoadSuccessfully()
            }

            onFailedToLoad = {
                callback.onFailedToLoad(it)
            }
        }.execute()
    }

    fun isEmptyMusicsList(): Boolean {
        return false
    }

    fun getMusicCategories(): MutableList<MediaBrowserCompat.MediaItem> {
        val categoriesMediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        retrieveMusicCategories().forEach {
            val mediaDescription = MediaDescriptionCompat.Builder()
                .setMediaId(it)
                .setTitle(it)
                .build()
            categoriesMediaItems.add(MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
        }
        return categoriesMediaItems
    }

    private fun retrieveMusicCategories(): List<String> {
        val categories = mutableListOf("all") //Always there will be minimum one category 'All'
        return try {
            categories.addAll(PreferencesV2.getMusicCategories(context))
            categories
        } catch (error: PreferencesV2.ValuesNotFound) {
            categories
        }
    }

    companion object {
        const val METADATA_TRACK_SOURCE = "__SOURCE__"
        const val METADATA_TRACK_CATEGORY = "__CATEGORY__"
    }
}

private class MusicsLoader(context: Context) : AsyncTask<Void, Void, MutableList<MediaMetadataCompat>>() {
    private val musicFolder = DataStorage.getMusicStorage(context)
    private val thumbnailsFolder = DataStorage.getThumbnailsStorage(context)

    var onLoadSuccessfully: ((musicItems: MutableList<MediaMetadataCompat>) -> Unit)? = null
    var onFailedToLoad: ((error: Exception) -> Unit)? = null

    private var currentError: Exception? = null

    override fun doInBackground(vararg params: Void?): ArrayList<MediaMetadataCompat>? {
        return try {
            getMetaDataItems()
        } catch (error: Exception) {
            currentError = error
            null
        }
    }

    private fun getMetaDataItems(): ArrayList<MediaMetadataCompat> {
        return ArrayList<MediaMetadataCompat>().apply {
            retrieveMusics().forEach { file ->
                this.add(retrieveMusicMetaData(file))
            }
        }
    }

    private fun retrieveMusics(): List<File> = musicFolder.walk().filter { it.extension == "mp3" }.toList()

    private fun retrieveMusicMetaData(musicFile: File): MediaMetadataCompat {
        val tag = TaggerV1(musicFile).readTag()
        return MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicFile.nameWithoutExtension)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, tag.title)
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, tag.authorChannel)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, tag.authorChannel)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Test description. Will be removed")
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, File(thumbnailsFolder, musicFile.nameWithoutExtension).toURI().toString())
            putString(METADATA_TRACK_SOURCE, musicFile.toURI().toString())
            putString(METADATA_TRACK_CATEGORY, "bass") //TODO (alieksiy) It's necessary to add category
        }.build()

    }

    override fun onPostExecute(result: MutableList<MediaMetadataCompat>?) {
        super.onPostExecute(result)
        if (result != null)
            onLoadSuccessfully?.invoke(result)
        else if (currentError != null)
            onFailedToLoad?.invoke(currentError!!)
    }
}
