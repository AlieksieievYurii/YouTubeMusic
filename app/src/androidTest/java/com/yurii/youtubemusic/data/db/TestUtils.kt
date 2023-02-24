package com.yurii.youtubemusic.data.db

import com.yurii.youtubemusic.db.MediaItemEntity
import com.yurii.youtubemusic.models.MediaItem
import java.io.File


fun createMediaItemEntities(n: Int, prefix: String = ""): List<MediaItemEntity> = (0 until n).map {
    createMediaItemEntity(it, prefix)
}

fun createMediaItems(n: Int): List<MediaItem> = (0 until n).map {
    MediaItem(
        id = it.toString(),
        title = "title-$it",
        description = "description-$it",
        author = "author-$it",
        durationInMillis = it.toLong(),
        thumbnail = File("/thumbnails/$it.jpg"),
        mediaFile = File("/media-files/$it.mp3"),
    )
}

fun createMediaItemEntity(id: Int, prefix: String = "") = MediaItemEntity(
    mediaItemId = "$prefix$id",
    title = "title-$prefix$id",
    author = "author-$prefix$id",
    durationInMillis = id.toLong(),
    thumbnail = File("/thumbnails/$prefix$id.jpg"),
    mediaFile = File("/media-files/$prefix$id.mp3"),
    position = id
)