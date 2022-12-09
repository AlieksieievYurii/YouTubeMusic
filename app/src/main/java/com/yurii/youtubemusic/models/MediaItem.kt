package com.yurii.youtubemusic.models

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toFile
import kotlinx.android.parcel.Parcelize
import java.io.File

/**
 * Data class representing media item containing all necessary information
 */
@Parcelize
data class MediaItem(
    override val id: String,
    override val title: String,
    override val author: String,
    override val durationInMillis: Long,
    val description: String,
    val thumbnail: File,
    val mediaFile: File,
    val categories: ArrayList<Category>
) : Item(id, title, author, durationInMillis) {
    companion object {
        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): MediaItem {
            val extras = mediaItem.description.extras!!
            return MediaItem(
                id = mediaItem.mediaId!!,
                title = mediaItem.description.title.toString(),
                description = mediaItem.description.description.toString(),
                author = extras.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)!!,
                durationInMillis = extras.getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
                thumbnail = mediaItem.description.iconUri!!.toFile(),
                mediaFile = mediaItem.description.mediaUri!!.toFile(),
                categories = extras.getParcelableArrayList(EXTRA_KEY_CATEGORIES)!!
            )
        }

        fun createFromMediaMetaData(mediaMetaData: MediaMetaData): MediaItem {
            return MediaItem(
                id = mediaMetaData.mediaId,
                title = mediaMetaData.title,
                author = mediaMetaData.author,
                durationInMillis = mediaMetaData.duration,
                description = mediaMetaData.description,
                thumbnail = mediaMetaData.thumbnail,
                mediaFile = mediaMetaData.mediaFile,
                categories = mediaMetaData.categories
            )
        }
    }
}