package com.yurii.youtubemusic.models

import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import kotlinx.android.parcel.Parcelize

/**
 * Represents a media category
 */
@Parcelize
data class Category(val id: Int, val name: String) : Parcelable {
    companion object {
        val ALL = Category(0, "all")

        fun createFrom(mediaItem: MediaBrowserCompat.MediaItem): Category {
            return Category(mediaItem.mediaId!!.toInt(), mediaItem.description.title.toString())
        }
    }
}

val Category.isDefault: Boolean
    get() = this.id == Category.ALL.id

fun Category.toMediaItem(): MediaBrowserCompat.MediaItem {
    val mediaDescription = MediaDescriptionCompat.Builder()
        .setMediaId(this.id.toString())
        .setTitle(this.name)
        .build()
    return MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
}

/**
 * Represents a combination of [Category] and assigned media items
 */
@Parcelize
data class CategoryContainer(val category: Category, val mediaItemsIds: List<String>) : Parcelable

fun List<Category>.toPlaylists() = map { MediaItemPlaylist(it.id.toLong(), it.name) }