package com.yurii.youtubemusic.models

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat

data class MediaItem(
    val musicId: String,
    val title: String,
    val author: String,
    val thumbnailPath: String,
    val duration: Long
) {
    companion object {
        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): MediaItem {
            return MediaItem(
                musicId = mediaItem.mediaId!!,
                title = mediaItem.description.title.toString(),
                author = mediaItem.description.extras!!.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)!!,
                thumbnailPath = mediaItem.description.iconUri!!.encodedPath!!,
                duration = mediaItem.description.extras!!.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )
        }
    }
}