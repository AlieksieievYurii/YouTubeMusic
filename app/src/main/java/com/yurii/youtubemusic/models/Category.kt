package com.yurii.youtubemusic.models

import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import kotlinx.android.parcel.Parcelize

/**
 * Represents a media category that is a group of included media items
 */
@Parcelize
data class Category(val id: Int, val name: String, private val _mediaItemsIds: MutableList<String>) : Parcelable {
    val mediaItemsIds: List<String>
        get() = _mediaItemsIds

    fun appendItem(item: Item) {
        if (_mediaItemsIds.contains(item.id))
            return

        _mediaItemsIds.add(item.id)
    }

    companion object {
        val ALL = Category(0, "all", mutableListOf())

        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): Category {
            return Category(mediaItem.mediaId!!.toInt(), mediaItem.description.title.toString(), mutableListOf())
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