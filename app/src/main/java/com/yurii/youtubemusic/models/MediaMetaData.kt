package com.yurii.youtubemusic.models

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File

data class MediaMetaData(
    val mediaId: String,
    val title: String,
    val description: String,
    val author: String,
    val duration: Long,
    val thumbnail: File,
    val mediaFile: File,
    val categories: ArrayList<Category> = ArrayList()
) {
    companion object {
        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): MediaMetaData {
            val extras = mediaItem.description.extras!!
            return MediaMetaData(
                mediaId = mediaItem.mediaId!!,
                title = mediaItem.description.title.toString(),
                description = mediaItem.description.description.toString(),
                author = extras.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)!!,
                duration = extras.getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
                thumbnail = mediaItem.description.iconUri!!.toFile(),
                mediaFile = mediaItem.description.mediaUri!!.toFile(),
                categories = extras.getParcelableArrayList(EXTRA_KEY_CATEGORIES)!!
            )
        }
    }
}

const val EXTRA_KEY_CATEGORIES = "com.yurii.youtubemusics.key.categories"

fun MediaMetaData.getMediaDescriptionCompat(): MediaDescriptionCompat {
    val extras = Bundle().also {
        it.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, this.author)
        it.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, this.duration)
        it.putParcelableArrayList(EXTRA_KEY_CATEGORIES, ArrayList(categories))
    }
    return MediaDescriptionCompat.Builder().also {
        it.setDescription(this.description)
        it.setTitle(this.title)
        it.setSubtitle(this.title)
        it.setMediaId(this.mediaId)
        it.setIconUri(this.thumbnail.toUri())
        it.setMediaUri(this.mediaFile.toUri())
        it.setExtras(extras)
    }.build()
}

fun MediaMetaData.toMediaMetadataCompat(): MediaMetadataCompat = MediaMetadataCompat.Builder().also {
    it.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, this.mediaId)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, this.title)
    it.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, this.author)
    it.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, this.author)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, this.description)
    it.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, this.thumbnail.toURI().toString())
    it.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, this.mediaFile.toString())
    it.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, this.duration)
}.build()

fun MediaMetaData.toCompatMediaItem(): MediaBrowserCompat.MediaItem {
    return MediaBrowserCompat.MediaItem(this.getMediaDescriptionCompat(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
}
