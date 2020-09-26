package com.yurii.youtubemusic.mediaservice

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.yurii.youtubemusic.mediaservice.MusicsProvider.Companion.METADATA_TRACK_CATEGORY
import com.yurii.youtubemusic.utilities.DataStorage
import com.yurii.youtubemusic.utilities.MediaMetadataProvider
import com.yurii.youtubemusic.utilities.Preferences
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
        return metaDataFiles.map {
            MediaBrowserCompat.MediaItem(getDescription(it), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }.toMutableList()
    }

    private fun getDescription(mediaItem: MediaMetadataCompat): MediaDescriptionCompat {
        val extras = Bundle().apply {
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, mediaItem.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR))
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaItem.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        }
        return MediaDescriptionCompat.Builder().apply {
            setDescription(mediaItem.description.description)
            setTitle(mediaItem.description.title)
            setSubtitle(mediaItem.description.subtitle)
            setMediaId(mediaItem.description.mediaId)
            setIconUri(mediaItem.description.iconUri)
            setMediaUri(mediaItem.description.mediaUri)
            setExtras(extras)
        }.build()
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
            categories.addAll(Preferences.getMusicCategories(context))
            categories
        } catch (error: Preferences.ValuesNotFound) {
            categories
        }
    }

    companion object {
        const val METADATA_TRACK_CATEGORY = "__CATEGORY__"
    }
}

private class MusicsLoader(private val context: Context) : AsyncTask<Void, Void, MutableList<MediaMetadataCompat>>() {
    private val mediaMetadataCompat = MediaMetadataProvider(context)

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
                this.add(retrieveMusicMetaData(file.nameWithoutExtension, file))
            }
        }
    }

    private fun retrieveMusics(): List<File> = DataStorage.getAllMusicFiles(context)

    private fun retrieveMusicMetaData(musicId: String, musicFile: File): MediaMetadataCompat {
        val musicMetadata = mediaMetadataCompat.readMetadata(musicId)
        return MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicMetadata.musicId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, musicMetadata.title)
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, musicMetadata.author)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicMetadata.author)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, musicMetadata.description)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, musicMetadata.thumbnail.toURI().toString())
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, musicFile.toURI().toString())
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicMetadata.duration)
            putString(METADATA_TRACK_CATEGORY, "bass")
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
