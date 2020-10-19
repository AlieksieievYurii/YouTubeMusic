package com.yurii.youtubemusic.models

import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(val id: Int, val name: String) : Parcelable {
    companion object {
        val ALL = Category(1, "all")

        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): Category {
            return Category(mediaItem.mediaId!!.toInt(), mediaItem.description.title.toString())
        }
    }
}

fun Category.toMediaItem(): MediaBrowserCompat.MediaItem {
    val mediaDescription = MediaDescriptionCompat.Builder()
        .setMediaId(this.id.toString())
        .setTitle(this.name)
        .build()
    return MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
}