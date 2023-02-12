package com.yurii.youtubemusic.data.db

import com.yurii.youtubemusic.db.MediaItemEntity
import com.yurii.youtubemusic.models.MediaItem
import java.io.File


fun createMediaItemEntities(n: Int, prefix: String = ""): List<MediaItemEntity> = (0 until n).map {
    MediaItemEntity(
        mediaItemId = "$prefix$it",
        title = "title-$prefix$it",
        author = "author-$prefix$it",
        durationInMillis = it.toLong(),
        thumbnail = File("/thumbnails/$prefix$it.jpg"),
        mediaFile = File("/media-files/$prefix$it.mp3"),
        position = it
    )
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