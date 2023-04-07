package com.youtubemusic.core.player

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.youtubemusic.core.model.MediaItem

fun MediaBrowserCompat.MediaItem.toMediaItem() = MediaItem(
    id = mediaId!!,
    title = description.title.toString(),
    description = description.description.toString(),
    author = description.extras?.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR) ?: "Unknown",
    durationInMillis = description.extras?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0L,
    thumbnail = description.iconUri!!.toFile(),
    mediaFile = description.mediaUri!!.toFile(),
)

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