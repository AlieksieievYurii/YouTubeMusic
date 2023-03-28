package com.yurii.youtubemusic.source

import com.yurii.youtubemusic.models.MediaItem
import com.yurii.youtubemusic.models.MediaItemPlaylist
import com.yurii.youtubemusic.services.media.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Validator @Inject constructor(private val mediaLibraryDomain: MediaLibraryDomain, private val mediaStorage: MediaStorage) {
    sealed class Error {
        data class MediaFileMissing(val mediaItem: MediaItem) : Error()
        data class ThumbnailFileMissing(val mediaItem: MediaItem) : Error()
    }

    fun removeDownloadingMocks() {
        mediaStorage.deleteDownloadingMocks()
    }

    suspend fun validateMediaItems() = withContext(Dispatchers.IO) {
        mediaLibraryDomain.getMediaItems(MediaItemPlaylist.ALL).first().forEach {
            val error = validate(it)
            if (error != null) {
                mediaLibraryDomain.deleteMediaItem(it)
            }
        }

        mediaStorage
    }

    private fun validate(mediaItem: MediaItem): Error? {
        if (!mediaStorage.getMediaFile(mediaItem).exists())
            return Error.MediaFileMissing(mediaItem)

        if (!mediaStorage.getThumbnail(mediaItem.id).exists())
            return Error.ThumbnailFileMissing(mediaItem)

        return null
    }
}