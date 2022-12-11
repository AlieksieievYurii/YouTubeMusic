package com.yurii.youtubemusic.models

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
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
            )
        }
    }
}

fun MediaItem.getMediaDescriptionCompat(): MediaDescriptionCompat {
    val extras = Bundle().also {
        it.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, this.author)
        it.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, this.durationInMillis)
    }
    return MediaDescriptionCompat.Builder().also {
        it.setDescription(this.description)
        it.setTitle(this.title)
        it.setSubtitle(this.title)
        it.setMediaId(this.id)
        it.setIconUri(this.thumbnail.toUri())
        it.setMediaUri(this.mediaFile.toUri())
        it.setExtras(extras)
    }.build()
}


fun MediaItem.toCompatMediaItem(): MediaBrowserCompat.MediaItem {
    return MediaBrowserCompat.MediaItem(this.getMediaDescriptionCompat(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
}

fun MediaItem.toMediaMetadataCompat(): MediaMetadataCompat = MediaMetadataCompat.Builder().also {
    it.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, this.id)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, this.title)
    it.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, this.author)
    it.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, this.author)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, this.description)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, this.thumbnail.absolutePath)
    it.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, this.mediaFile.toString())
    it.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, this.durationInMillis)
}.build()